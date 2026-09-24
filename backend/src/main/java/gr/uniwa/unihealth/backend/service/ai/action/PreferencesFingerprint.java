package gr.uniwa.unihealth.backend.service.ai.action;

import gr.uniwa.unihealth.backend.dto.UserPreferencesDTO;
import lombok.experimental.UtilityClass;
import org.apache.commons.codec.digest.DigestUtils;

/**
 * A digest of a student's three preference settings, taken when a change is proposed and again
 * before it is applied.
 *
 * <p>Shared by the service that proposes and the service that executes, so the two cannot drift
 * into computing it differently - which would either reject every confirmation or accept every
 * stale one, and both would look like the feature working.
 *
 * <p>The tri-state consent is stringified rather than treated as a boolean on purpose: {@code null}
 * means "never asked" and is a genuinely different state from {@code false} meaning "declined".
 * Collapsing them would let a proposal made before the consent dialog was ever shown apply
 * afterwards as though nothing had changed.
 *
 * @author omaro
 */
@UtilityClass
public class PreferencesFingerprint {

  public String of(UserPreferencesDTO preferences) {
    return DigestUtils.md5Hex(preferences.isNewsletterSubscribed() + "|"
        + preferences.isNotificationsEnabled() + "|"
        + preferences.getAnalyticsConsent());
  }
}
