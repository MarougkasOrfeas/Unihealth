package gr.uniwa.unihealth.backend.model.enums.survey;

/**
 * How comfortable the student is entering health data here.
 *
 * <p>Distinct from the analytics consent already stored on the user: that asks permission to
 * measure reading habits, this asks whether they trust the place with the data at all.
 */
public enum SurveyComfort {
  VERY, QUITE, A_LITTLE, NOT_AT_ALL;
}
