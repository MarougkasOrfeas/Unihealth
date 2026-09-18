package gr.uniwa.unihealth.backend.service.personalization.impl;

import com.eurodyn.qlack.common.exception.QDoesNotExistException;
import gr.uniwa.unihealth.backend.dto.LabelUsageDTO;
import gr.uniwa.unihealth.backend.model.User;
import gr.uniwa.unihealth.backend.model.UserLabelMetric;
import gr.uniwa.unihealth.backend.model.UserProfileLabels;
import gr.uniwa.unihealth.backend.repository.UserLabelMetricRepository;
import gr.uniwa.unihealth.backend.repository.UserProfileLabelsRepository;
import gr.uniwa.unihealth.backend.repository.UserRepository;
import gr.uniwa.unihealth.backend.service.personalization.UserLabelMetricService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Implementation for {@link UserLabelMetricService}.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserLabelMetricServiceImpl implements UserLabelMetricService {

  /**
   * A single submitted duration is capped here. The client already clamps a view at ten minutes,
   * but this is the value the database will actually take — a tampered or buggy client must not be
   * able to claim a whole afternoon on one label.
   */
  private static final long MAX_VIEW_SECONDS_PER_SUBMISSION = 600L;

  /** Guards against a runaway client flushing thousands of labels in one request. */
  private static final int MAX_ENTRIES_PER_SUBMISSION = 200;

  private final UserLabelMetricRepository repository;
  private final UserProfileLabelsRepository userProfileLabelsRepository;
  private final UserRepository userRepository;

  @Override
  @Transactional
  public void record(String username, List<LabelUsageDTO> usage) {
    if (usage == null || usage.isEmpty()) {
      return;
    }

    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new QDoesNotExistException("Could not find logged in user."));

    // The authoritative consent check. The client also checks, but only this one counts.
    if (!Boolean.TRUE.equals(user.getAnalyticsConsent())) {
      log.debug("Dropping usage submission for user {}: no consent", user.getId());
      return;
    }

    // Only labels the user genuinely has. Without this a client could report engagement with any
    // code it liked and quietly reshape its own profile.
    Set<String> ownedLabels = new HashSet<>(
        userProfileLabelsRepository.findByUserId(user.getId())
            .map(UserProfileLabels::getLabels)
            .orElse(List.of()));

    if (ownedLabels.isEmpty()) {
      return;
    }

    LocalDateTime now = LocalDateTime.now();
    int applied = 0;

    for (LabelUsageDTO entry : usage) {
      if (applied >= MAX_ENTRIES_PER_SUBMISSION) {
        log.warn("Truncating usage submission for user {} at {} entries", user.getId(), applied);
        break;
      }
      if (entry.getLabelCode() == null || !ownedLabels.contains(entry.getLabelCode())) {
        continue;
      }

      long seconds = Math.min(Math.max(0L, entry.getViewSeconds()), MAX_VIEW_SECONDS_PER_SUBMISSION);
      int interactions = Math.max(0, entry.getInteractionCount());
      if (seconds == 0L && interactions == 0) {
        continue;
      }

      accumulate(user, entry.getLabelCode(), seconds, interactions, now);
      applied++;
    }
  }

  @Override
  @Transactional(readOnly = true)
  public List<LabelUsageDTO> findForUser(String userId) {
    return repository.findByUserId(userId).stream()
        .map(metric -> new LabelUsageDTO(
            metric.getLabelCode(), metric.getViewSeconds(), metric.getInteractionCount()))
        .toList();
  }

  @Override
  @Transactional
  public void deleteForUser(String userId) {
    repository.deleteByUserId(userId);
    log.info("Deleted all usage metrics for user {}", userId);
  }

  /**
   * Read-then-upsert. Like {@code UserFavouriteTopicServiceImpl.add}, this races under concurrency
   * and relies on {@code uq_user_label_metric} as the real guard; a lost race costs one flush of
   * one label, which is an acceptable trade for keeping the write path this simple.
   */
  private void accumulate(User user, String labelCode, long seconds, int interactions,
      LocalDateTime now) {
    UserLabelMetric metric = repository.findByUserIdAndLabelCode(user.getId(), labelCode)
        .orElseGet(() -> {
          UserLabelMetric created = new UserLabelMetric();
          // A reference, not a load: the id came from the authenticated principal, so there is
          // nothing to validate and no reason to pay for the select.
          created.setUser(userRepository.getReferenceById(user.getId()));
          created.setLabelCode(labelCode);
          return created;
        });

    metric.setViewSeconds(metric.getViewSeconds() + seconds);
    metric.setInteractionCount(metric.getInteractionCount() + interactions);
    metric.setLastSeenOn(now);

    repository.save(metric);
  }
}
