package gr.uniwa.unihealth.backend.utils.email;

public enum EmailNotificationType {
  USER_CREATED,
  EMAIL_CHANGED,
  ACCOUNT_ABOUT_TO_EXPIRE,
  ADMIN_ACCOUNT_DATED,
  /** Confirms an opt-out and explains how to opt back in. Always sent. */
  NEWSLETTER_UNSUBSCRIBED,
  /** Nudge to finish the optional health profile. Gated on notificationsEnabled. */
  OPTIONAL_FORM_REMINDER,
  /** Periodic health-news round-up. Gated on newsletterSubscribed. */
  NEWS_DIGEST
}
