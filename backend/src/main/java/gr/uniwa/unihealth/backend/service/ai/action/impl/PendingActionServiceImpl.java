package gr.uniwa.unihealth.backend.service.ai.action.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import gr.uniwa.unihealth.backend.config.context.AuthenticationContext;
import gr.uniwa.unihealth.backend.config.context.TenantContext;
import gr.uniwa.unihealth.backend.dto.UserPreferencesDTO;
import gr.uniwa.unihealth.backend.dto.ai.ActionFieldChangeDTO;
import gr.uniwa.unihealth.backend.dto.ai.PendingActionDTO;
import gr.uniwa.unihealth.backend.service.UserPreferencesService;
import gr.uniwa.unihealth.backend.service.ai.action.AiActionKind;
import gr.uniwa.unihealth.backend.service.ai.action.PendingAction;
import gr.uniwa.unihealth.backend.service.ai.action.PendingActionService;
import gr.uniwa.unihealth.backend.service.ai.action.PreferencesFingerprint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation for {@link PendingActionService}, backed by Redis.
 *
 * <p>Redis rather than a table because these entries are worthless after a few minutes and a TTL
 * expresses that better than a cleanup job. It is already deployed for the cache.
 *
 * <p>Deliberately <em>not</em> using the configured {@code CacheManager}. That one serialises with
 * {@code JdkSerializationRedisSerializer}, which would make this payload's wire format a function
 * of Java class shape - rename a field and every entry in flight becomes unreadable. A
 * {@link StringRedisTemplate} plus explicit JSON keeps the format inspectable with
 * {@code redis-cli}
 * and survives a refactor.
 *
 * @author omaro
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PendingActionServiceImpl implements PendingActionService {

  /**
   * Long enough to read a confirmation card and think about it, short enough that a proposal cannot
   * be confirmed from a tab left open since yesterday.
   */
  private static final Duration TTL = Duration.ofMinutes(5);

  private static final String KEY_PREFIX = "ai:pending:";

  private final StringRedisTemplate redis;
  private final UserPreferencesService userPreferencesService;
  private final AuthenticationContext authenticationContext;
  private final TenantContext tenantContext;

  /**
   * Its own mapper rather than an injected one: this application configures Jackson 3 for HTTP,
   * while this payload is bound with Jackson 2. Constructing it here keeps the two apart instead of
   * depending on which happens to win injection.
   */
  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  @Override
  public Optional<PendingActionDTO> propose(AiActionKind kind, boolean enabled) {
    // Read-modify-write, and the single most important line in this class. updateMyPreferences
    // replaces all three settings; a payload built from just the one being changed would silently
    // reset the other two - and for analytics consent, "reset" means deleting every metric the
    // student has ever generated.
    UserPreferencesDTO current = userPreferencesService.findMyPreferences();

    if (kind.isCurrentlyEnabled(current) == enabled) {
      log.debug("Proposal for {} is a no-op; nothing to confirm.", kind);
      return Optional.empty();
    }

    UserPreferencesDTO target = copyOf(current);
    kind.apply(target, enabled);

    PendingAction action = new PendingAction(UUID.randomUUID().toString(), kind, enabled, target,
        PreferencesFingerprint.of(current), kind.isIrreversible(enabled));

    store(action);

    log.info("Assistant proposed {} -> {} for the logged-in student; awaiting confirmation.",
        kind, enabled);

    return Optional.of(new PendingActionDTO(action.getActionId(), kind.name(), kind.labelKey(),
        List.of(new ActionFieldChangeDTO(kind.labelKey(), !enabled, enabled)),
        action.isIrreversible()));
  }

  @Override
  public Optional<PendingAction> consume(String actionId) {
    if (actionId == null || actionId.isBlank()) {
      return Optional.empty();
    }

    // getAndDelete, so a replayed confirmation finds nothing. The key is rebuilt from the caller's
    // own tenant and username, never from the request, which is what makes a leaked actionId
    // useless to anybody else.
    String json = redis.opsForValue().getAndDelete(keyFor(actionId));
    if (json == null) {
      return Optional.empty();
    }

    try {
      return Optional.of(objectMapper.readValue(json, PendingAction.class));
    } catch (Exception e) {
      // An entry written by an older shape of this class. Dropping it is right: the student will be
      // asked again, and applying a half-understood payload to their settings would not be.
      log.warn("Discarding an unreadable pending action: {}", e.getMessage());
      return Optional.empty();
    }
  }

  @Override
  public void discard(String actionId) {
    if (actionId != null && !actionId.isBlank()) {
      redis.delete(keyFor(actionId));
    }
  }

  private UserPreferencesDTO copyOf(UserPreferencesDTO source) {
    UserPreferencesDTO copy = new UserPreferencesDTO();
    copy.setNewsletterSubscribed(source.isNewsletterSubscribed());
    copy.setNotificationsEnabled(source.isNotificationsEnabled());
    copy.setAnalyticsConsent(source.getAnalyticsConsent());
    return copy;
  }

  private void store(PendingAction action) {
    try {
      redis.opsForValue()
          .set(keyFor(action.getActionId()), objectMapper.writeValueAsString(action), TTL);
    } catch (Exception e) {
      throw new IllegalStateException(
          "Could not record the proposed change: " + e.getMessage(), e);
    }
  }

  private String keyFor(String actionId) {
    return KEY_PREFIX + tenantContext.getCurrentTenant() + ':'
        + authenticationContext.getCurrentUsername() + ':' + actionId;
  }
}
