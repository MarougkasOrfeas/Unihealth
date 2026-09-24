package gr.uniwa.unihealth.backend.service.ai.ingest;

import gr.uniwa.unihealth.backend.model.SymptomItem;

import java.util.function.Function;

/**
 * The parts of a symptom page that become one chunk each.
 *
 * <p>These are the NHS page's own editorial divisions, already separated into columns at ingest.
 * Splitting on token count instead would cut across them and throw away the one thing this corpus
 * gives for free: every retrieved passage can say which question it answers, so a citation reads
 * "Sore throat - When to see a GP" rather than naming a character offset.
 *
 * <p>The labels are English because the passages are: the corpus is the NHS website under Open
 * Government Licence. The assistant is told to reply in the student's language, so a Greek answer
 * cites an English section name, which is honest about where the text came from.
 *
 * @author omaro
 */
public enum SymptomSection {

  BRIEF("Summary", Urgency.NONE, SymptomItem::getBrief),
  OVERVIEW("Overview", Urgency.NONE, SymptomItem::getOverviewText),
  SYMPTOMS("Symptoms", Urgency.NONE, SymptomItem::getSymptomsText),
  DO("What you can do", Urgency.NONE, SymptomItem::getDoText),
  DONT("What to avoid", Urgency.NONE, SymptomItem::getDontText),
  SEE_DOCTOR_IF("When to see a GP", Urgency.NONE, SymptomItem::getSeeDoctorIfText),
  TREATMENT("Treatment", Urgency.NONE, SymptomItem::getTreatmentText),
  CAUSES("Possible causes", Urgency.NONE, SymptomItem::getCausesText),
  URGENT("Get urgent advice", Urgency.URGENT, SymptomItem::getUrgentText),
  EMERGENCY("Call 999 or go to A&E", Urgency.EMERGENCY, SymptomItem::getEmergencyText);

  private final String label;
  private final Urgency urgency;
  private final Function<SymptomItem, String> extractor;

  SymptomSection(String label, Urgency urgency, Function<SymptomItem, String> extractor) {
    this.label = label;
    this.urgency = urgency;
    this.extractor = extractor;
  }

  public String label() {
    return label;
  }

  public Urgency urgency() {
    return urgency;
  }

  /** @return this section's text on the given symptom, or {@code null} when the page has none. */
  public String textOf(SymptomItem symptom) {
    return extractor.apply(symptom);
  }
}
