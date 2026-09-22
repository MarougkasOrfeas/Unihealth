package gr.uniwa.unihealth.backend.model.enums.survey;

/**
 * Which part of the application the student uses most.
 *
 * <p>Only sections that actually work are offered. {@code NONE_YET} is a real answer and not a
 * cop-out: a student who has not used anything yet is telling us something about discoverability.
 */
public enum SurveySection {
  TOPICS, ADVICE, SYMPTOMS, AI_CHAT, NEWS, MY_TESTS, PROFILE, NONE_YET;
}
