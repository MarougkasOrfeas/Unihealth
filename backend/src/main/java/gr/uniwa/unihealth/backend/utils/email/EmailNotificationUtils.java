package gr.uniwa.unihealth.backend.utils.email;


import static gr.uniwa.unihealth.backend.utils.email.EmailNotificationTextConstants.*;

public class EmailNotificationUtils {

  public static EmailSubjectBodyPair getSubjectBodyPairByNotificationTypeAndLanguage(
      EmailNotificationType emailNotificationType, String locale) {
    String localeToUse = locale.toLowerCase();
    return switch (emailNotificationType) {
      case USER_CREATED -> userCreated(localeToUse);
      case EMAIL_CHANGED -> emailChanged(localeToUse);
      case ACCOUNT_ABOUT_TO_EXPIRE -> imminentDeactivation(localeToUse);
      case ADMIN_ACCOUNT_DATED -> adminAccountDated();
      case NEWSLETTER_UNSUBSCRIBED -> newsletterUnsubscribed(localeToUse);
      case OPTIONAL_FORM_REMINDER -> optionalFormReminder(localeToUse);
      case NEWS_DIGEST -> newsDigest(localeToUse);
    };
  }

  private static EmailSubjectBodyPair userCreated(String locale) {
    return switch (locale) {
      case "en" -> new EmailSubjectBodyPair(EmailNotificationTextConstants.USER_CREATED_SUBJECT_EN,
          EmailNotificationTextConstants.USER_CREATED_BODY_EN);
      default -> new EmailSubjectBodyPair(EmailNotificationTextConstants.USER_CREATED_SUBJECT_GR,
          EmailNotificationTextConstants.USER_CREATED_BODY_GR);
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
      case "en" ->
          new EmailSubjectBodyPair(IMMINENT_DEACTIVATION_SUBJECT_EN, IMMINENT_DEACTIVATION_BODY_EN);
      default ->
          new EmailSubjectBodyPair(IMMINENT_DEACTIVATION_SUBJECT_GR, IMMINENT_DEACTIVATION_BODY_GR);
    };
  }

  private static EmailSubjectBodyPair newsletterUnsubscribed(String locale) {
    return switch (locale) {
      case "en" -> new EmailSubjectBodyPair(NEWSLETTER_UNSUBSCRIBED_SUBJECT_EN,
          NEWSLETTER_UNSUBSCRIBED_BODY_EN);
      default -> new EmailSubjectBodyPair(NEWSLETTER_UNSUBSCRIBED_SUBJECT_GR,
          NEWSLETTER_UNSUBSCRIBED_BODY_GR);
    };
  }

  private static EmailSubjectBodyPair optionalFormReminder(String locale) {
    return switch (locale) {
      case "en" -> new EmailSubjectBodyPair(OPTIONAL_FORM_REMINDER_SUBJECT_EN,
          OPTIONAL_FORM_REMINDER_BODY_EN);
      default -> new EmailSubjectBodyPair(OPTIONAL_FORM_REMINDER_SUBJECT_GR,
          OPTIONAL_FORM_REMINDER_BODY_GR);
    };
  }

  private static EmailSubjectBodyPair newsDigest(String locale) {
    return switch (locale) {
      case "en" -> new EmailSubjectBodyPair(NEWS_DIGEST_SUBJECT_EN, NEWS_DIGEST_BODY_EN);
      default -> new EmailSubjectBodyPair(NEWS_DIGEST_SUBJECT_GR, NEWS_DIGEST_BODY_GR);
    };
  }

  private static EmailSubjectBodyPair adminAccountDated() {
    return new EmailSubjectBodyPair(DATED_ADMIN_ACCOUNT_SUBJECT, DATED_ADMIN_ACCOUNT_BODY);
  }
}
