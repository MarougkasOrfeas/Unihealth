package gr.uniwa.unihealth.backend.model.enums.survey;

/**
 * A five-point "how much" scale, shared by the overall-usefulness and ease-of-finding questions.
 *
 * <p>One enum for both rather than two identical ones: they ask the same shape of question, and a
 * single scale is one fewer thing to keep in step with the frontend and the lexicon.
 */
public enum SurveyRating5 {
  NOT_AT_ALL, A_LITTLE, MODERATELY, QUITE, VERY;
}
