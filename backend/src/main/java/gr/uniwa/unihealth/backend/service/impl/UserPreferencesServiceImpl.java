package gr.uniwa.unihealth.backend.service.impl;

import com.eurodyn.qlack.common.exception.QDoesNotExistException;
import gr.uniwa.unihealth.backend.config.context.AuthenticationContext;
import gr.uniwa.unihealth.backend.dto.UserPreferencesDTO;
import gr.uniwa.unihealth.backend.model.User;
import gr.uniwa.unihealth.backend.repository.UserRepository;
import gr.uniwa.unihealth.backend.service.EmailNotificationService;
import gr.uniwa.unihealth.backend.service.UserPreferencesService;
import gr.uniwa.unihealth.backend.service.personalization.UserLabelMetricService;
import gr.uniwa.unihealth.backend.utils.email.EmailNotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation for {@link UserPreferencesService}.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class UserPreferencesServiceImpl implements UserPreferencesService {

  private final UserRepository userRepository;
  private final AuthenticationContext authenticationContext;
  private final EmailNotificationService emailNotificationService;
  private final UserLabelMetricService userLabelMetricService;

  @Value("${unihealth.app.www.url}")
  private String appUrl;

  @Override
  @Transactional(readOnly = true)
  public UserPreferencesDTO findMyPreferences() {
    return toDto(currentUser());
  }

  @Override
  public UserPreferencesDTO updateMyPreferences(UserPreferencesDTO dto) {
    User user = currentUser();

    // Captured before the write: the goodbye email is owed to the transition, not to the state.
    // Re-saving the form while already unsubscribed must not send it again.
    boolean justUnsubscribed = user.isNewsletterSubscribed() && !dto.isNewsletterSubscribed();

    // Same reasoning: only an actual withdrawal deletes. Re-saving while already opted out must
    // not fire a pointless delete, and a first-time "no" has nothing to delete.
    boolean justWithdrewConsent = Boolean.TRUE.equals(user.getAnalyticsConsent())
        && !Boolean.TRUE.equals(dto.getAnalyticsConsent());

    user.setNewsletterSubscribed(dto.isNewsletterSubscribed());
    user.setNotificationsEnabled(dto.isNotificationsEnabled());
    user.setAnalyticsConsent(dto.getAnalyticsConsent());
    userRepository.save(user);

    if (justUnsubscribed) {
      sendGoodbye(user);
    }

    if (justWithdrewConsent) {
      // Withdrawal means the data goes too, not merely that collection stops.
      userLabelMetricService.deleteForUser(user.getId());
    }

    return toDto(user);
  }

  /**
   * Sent regardless of the preference just set: it confirms the opt-out and carries the
   * instructions for opting back in, which is the one message an unsubscriber still needs.
   */
  private void sendGoodbye(User user) {
    emailNotificationService.sendEmailNotification(List.of(user.getEmail()),
        localeOf(user), EmailNotificationType.NEWSLETTER_UNSUBSCRIBED, appUrl);
    log.info("Queued newsletter goodbye email for user {}", user.getId());
  }

  private User currentUser() {
    String username = authenticationContext.getCurrentUsername();
    return userRepository.findByUsername(username)
        .orElseThrow(() -> new QDoesNotExistException("Could not find logged in user."));
  }

  /** Falls back to Greek, matching the default used when an administrator creates a user. */
  private String localeOf(User user) {
    return user.getLanguage() != null ? user.getLanguage().getLocale() : "el";
  }

  private UserPreferencesDTO toDto(User user) {
    UserPreferencesDTO dto = new UserPreferencesDTO();
    dto.setNewsletterSubscribed(user.isNewsletterSubscribed());
    dto.setNotificationsEnabled(user.isNotificationsEnabled());
    dto.setAnalyticsConsent(user.getAnalyticsConsent());
    return dto;
  }
}
