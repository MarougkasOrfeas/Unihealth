package gr.uniwa.unihealth.backend.service.ai.action;

import gr.uniwa.unihealth.backend.dto.UserPreferencesDTO;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * Defines the user settings the AI is allowed to change.
 * <p>Each action knows how to read and update its corresponding preference.
 * Actions not listed here cannot be proposed by the AI.
 *
 * @author omaro */
public enum AiActionKind {

  /** Controls non-critical notifications. */
  NOTIFICATIONS("ai.action.notifications", false, UserPreferencesDTO::isNotificationsEnabled, UserPreferencesDTO::setNotificationsEnabled),

  /** Controls the health-news newsletter. */
  NEWSLETTER("ai.action.newsletter", false, UserPreferencesDTO::isNewsletterSubscribed, UserPreferencesDTO::setNewsletterSubscribed),

  /** Controls analytics consent. Disabling it also deletes existing analytics data. */
  ANALYTICS_CONSENT("ai.action.analyticsConsent", true,
      preferences -> Boolean.TRUE.equals(preferences.getAnalyticsConsent()),
      (preferences, enabled) -> preferences.setAnalyticsConsent(enabled));

  private final String labelKey;
  private final boolean irreversibleWhenDisabled;
  private final Predicate<UserPreferencesDTO> reader;
  private final BiConsumer<UserPreferencesDTO, Boolean> writer;

  AiActionKind(String labelKey, boolean irreversibleWhenDisabled,
      Predicate<UserPreferencesDTO> reader, BiConsumer<UserPreferencesDTO, Boolean> writer) {
    this.labelKey = labelKey;
    this.irreversibleWhenDisabled = irreversibleWhenDisabled;
    this.reader = reader;
    this.writer = writer;
  }

  public String labelKey() {
    return labelKey;
  }

  public boolean isCurrentlyEnabled(UserPreferencesDTO preferences) {
    return reader.test(preferences);
  }

  public void apply(UserPreferencesDTO preferences, boolean enabled) {
    writer.accept(preferences, enabled);
  }

  public boolean isIrreversible(boolean enabled) {
    return irreversibleWhenDisabled && !enabled;
  }
}
