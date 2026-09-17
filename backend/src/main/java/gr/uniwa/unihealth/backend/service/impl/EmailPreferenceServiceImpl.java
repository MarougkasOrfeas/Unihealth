package gr.uniwa.unihealth.backend.service.impl;

import gr.uniwa.unihealth.backend.model.User;
import gr.uniwa.unihealth.backend.service.EmailPreferenceService;
import gr.uniwa.unihealth.backend.utils.email.EmailNotificationType;
import org.springframework.stereotype.Service;

/**
 * Implementation for {@link EmailPreferenceService}.
 *
 * <p>Deliberately a whitelist per type rather than a single "wants email" flag: a new notification
 * type has to state which preference governs it, so nothing becomes silently suppressible.
 */
@Service
public class EmailPreferenceServiceImpl implements EmailPreferenceService {

  @Override
  public boolean mayReceive(User user, EmailNotificationType type) {
    if (user == null) {
      return false;
    }

    return switch (type) {
      // Account-critical or transactional: never suppressed. ACCOUNT_ABOUT_TO_EXPIRE in particular
      // is the only warning before deactivation, so muting it would cost the user their account.
      case USER_CREATED, EMAIL_CHANGED, ACCOUNT_ABOUT_TO_EXPIRE, ADMIN_ACCOUNT_DATED -> true;
      // Confirms the opt-out itself and carries the instructions for opting back in, so it has to
      // go out precisely when the user has just said "no more email".
      case NEWSLETTER_UNSUBSCRIBED -> true;
      case NEWS_DIGEST -> user.isNewsletterSubscribed();
      case OPTIONAL_FORM_REMINDER -> user.isNotificationsEnabled();
    };
  }
}
