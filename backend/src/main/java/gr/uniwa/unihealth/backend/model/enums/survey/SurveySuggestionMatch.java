package gr.uniwa.unihealth.backend.model.enums.survey;

/**
 * How well the personalised "Because you told us…" suggestions fit.
 *
 * <p>{@code NOT_NOTICED} matters as much as the other three. The reasons appear in exactly two
 * places, so a student picking it is reporting a placement problem rather than a scoring one.
 */
public enum SurveySuggestionMatch {
  YES_CLOSELY, MOSTLY, NOT_REALLY, NOT_NOTICED;
}
