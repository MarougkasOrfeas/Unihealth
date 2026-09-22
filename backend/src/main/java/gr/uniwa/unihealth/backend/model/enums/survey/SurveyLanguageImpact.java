package gr.uniwa.unihealth.backend.model.enums.survey;

/**
 * Whether the English-language pages get in the student's way.
 *
 * <p>The symptom, condition and wellness pages are hardcoded English in a Greek-default
 * application. This question decides whether translating them is urgent or merely tidy.
 */
public enum SurveyLanguageImpact {
  A_LOT, A_LITTLE, NOT_AT_ALL;
}
