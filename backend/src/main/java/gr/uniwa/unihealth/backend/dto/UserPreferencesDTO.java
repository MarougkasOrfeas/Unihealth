package gr.uniwa.unihealth.backend.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * The preferences a user manages for themselves under Profile &gt; Preferences.
 *
 * <p>A dedicated DTO rather than fields on {@link UserDTO}: this is the only payload a
 * non-administrator may send about their own account, and keeping it separate means the
 * admin-guarded user update cannot be reached through it.
 */
@Getter
@Setter
public class UserPreferencesDTO {

  /** True while the user still wants the health-news digest. */
  private boolean newsletterSubscribed;

  /** True while the user accepts occasional non-critical notifications. */
  private boolean notificationsEnabled;

  /**
   * Consent to usage measurement. Boxed because {@code null} ("never asked") must survive the
   * round trip: it is what tells the client to show the consent dialog. Only {@code TRUE} permits
   * collection, and setting it away from {@code TRUE} deletes everything already collected.
   */
  private Boolean analyticsConsent;
}
