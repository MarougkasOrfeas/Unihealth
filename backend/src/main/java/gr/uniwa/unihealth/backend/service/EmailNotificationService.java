package gr.uniwa.unihealth.backend.service;


import gr.uniwa.unihealth.backend.utils.email.EmailNotificationType;

import java.util.List;

public interface EmailNotificationService {

  /**
   * Sends and email notification.
   *
   * @param emailAddresses The email addresses to send the notification to.
   * @param locale The user's preferred language for the email content.
   * @param emailNotificationType The type of the Email notification to send, according to
   *        {@link EmailNotificationType}.
   */
  void sendEmailNotification(List<String> emailAddresses, String locale, EmailNotificationType emailNotificationType, Object... emailBodyVars);
}
