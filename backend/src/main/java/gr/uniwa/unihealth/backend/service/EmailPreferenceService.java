package gr.uniwa.unihealth.backend.service;

import gr.uniwa.unihealth.backend.model.User;
import gr.uniwa.unihealth.backend.utils.email.EmailNotificationType;

/**
 * Decides whether a notification type may be sent to a given user.
 *
 * <p>The point of routing every optional email through here is that account-critical mail must stay
 * unmutable. A user who unsubscribes from the news digest still has to receive the warning before
 * their account is deactivated, otherwise they lose the account silently.
 */
public interface EmailPreferenceService {

  /**
   * @param user the intended recipient.
   * @param type the notification about to be queued.
   * @return true when the user's preferences allow this notification.
   */
  boolean mayReceive(User user, EmailNotificationType type);
}
