package gr.uniwa.unihealth.backend.model.enums.survey;

/**
 * How often the student wants to hear from us by email.
 *
 * <p>There are two emails today — the Monday news digest and the optional-form reminder — governed
 * by a single on/off toggle. {@code REMINDERS_ONLY} is the answer that toggle cannot express.
 */
public enum SurveyEmailFrequency {
  NEVER, MONTHLY, WEEKLY, REMINDERS_ONLY;
}
