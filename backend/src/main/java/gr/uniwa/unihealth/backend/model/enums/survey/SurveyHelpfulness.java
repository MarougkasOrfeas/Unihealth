package gr.uniwa.unihealth.backend.model.enums.survey;

/**
 * How helpful the AI assistant's answers were.
 *
 * <p>{@code NOT_USED} keeps the other four honest — without it, everyone who has never opened the
 * chat is pushed into picking a rating they have no basis for.
 */
public enum SurveyHelpfulness {
  VERY, QUITE, A_LITTLE, NOT_AT_ALL, NOT_USED;
}
