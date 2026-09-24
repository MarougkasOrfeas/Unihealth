package gr.uniwa.unihealth.backend.dto.ai;

/** * Contains the symptom information needed by the assistant.
 *
 * @param title symptom name
 * @param brief short symptom summary
 * @param whenToSeeDoctor guidance on when to see a doctor
 * @param urgent urgent medical guidance
 * @param emergency emergency medical guidance
 * @param hasFactors whether additional symptom factors are available
 * @param slug stable symptom identifier
 * @param sourceUrl link to the source
 *
 * @author omaro */
public record SymptomSummaryDTO(String title, String brief, String whenToSeeDoctor, String urgent,
                                String emergency, boolean hasFactors, String slug, String sourceUrl) { }
