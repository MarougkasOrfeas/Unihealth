package gr.uniwa.unihealth.backend.model.enums.survey;

/**
 * How the health questionnaire felt.
 *
 * <p>{@code TOO_LONG_UNFINISHED} is deliberately separate from {@code LONG}: "long but I finished"
 * and "long so I gave up" call for very different responses.
 */
public enum SurveyFormLength {
  SHORT, ABOUT_RIGHT, LONG, TOO_LONG_UNFINISHED;
}
