package gr.uniwa.unihealth.backend.utils.email;


import static gr.uniwa.unihealth.backend.utils.email.EmailNotificationTextConstants.*;

public class EmailNotificationUtils {

  public static EmailSubjectBodyPair getSubjectBodyPairByNotificationTypeAndLanguage(EmailNotificationType emailNotificationType, String locale) {
    String localeToUse = locale.toLowerCase();
    return switch (emailNotificationType) {
      case EMAIL_CHANGED -> emailChanged(localeToUse);
      case ACCOUNT_ABOUT_TO_EXPIRE -> imminentDeactivation(localeToUse);
      case ADMIN_ACCOUNT_DATED -> adminAccountDated();
    };
  }

  private static EmailSubjectBodyPair emailChanged(String locale) {
    return switch (locale) {
      case "en" -> new EmailSubjectBodyPair(EMAIL_CHANGED_SUBJECT_EN, EMAIL_CHANGED_BODY_EN);
      default -> new EmailSubjectBodyPair(EMAIL_CHANGED_SUBJECT_GR, EMAIL_CHANGED_BODY_GR);
    };
  }

  private static EmailSubjectBodyPair imminentDeactivation(String locale) {
    return switch (locale) {
      case "en" -> new EmailSubjectBodyPair(IMMINENT_DEACTIVATION_SUBJECT_EN, IMMINENT_DEACTIVATION_BODY_EN);
      default -> new EmailSubjectBodyPair(IMMINENT_DEACTIVATION_SUBJECT_GR, IMMINENT_DEACTIVATION_BODY_GR);
    };
  }

  private static EmailSubjectBodyPair adminAccountDated() {
    return new EmailSubjectBodyPair(DATED_ADMIN_ACCOUNT_SUBJECT, DATED_ADMIN_ACCOUNT_BODY);
  }
}
