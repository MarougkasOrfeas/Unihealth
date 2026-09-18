package gr.uniwa.unihealth.backend.service.bootstrap;

import java.util.List;

/**
 * Mirrors {@code resources/data/labels/medical-terms.json}, the bilingual term dictionary used to
 * turn free-text health answers into label codes.
 *
 * <p>Shaped the same way {@link NhsSnapshot} mirrors the NHS symptom snapshot: a plain record bound
 * by Jackson, living beside the loader that reads it, so the file's schema and the Java view of it
 * cannot drift apart without a compile error.
 *
 * <p>Boxed {@code Integer} rather than {@code int} so a field missing from the JSON is detectable
 * as {@code null} instead of silently arriving as zero — which for {@code priority} would rank a
 * chronic condition below everything else rather than failing validation.
 */
public record MedicalTermDictionary(Integer schemaVersion, String datasetVersion, String notes,
                                    List<Concept> concepts) {

  /**
   * One medical idea, and every way a student might write it.
   *
   * @param scope which free-text field this may be matched against. {@code BOTH} exists for the
   *              handful of concepts — coeliac, gluten, lactose — that students routinely write in
   *              the allergy box, where a chronic-only term could previously never match.
   */
  public record Concept(String conceptId, String labelCode, String scope, Integer priority,
                        String source, String note, List<Term> terms) {

    public boolean appliesTo(String keywordType) {
      return "BOTH".equals(scope) || scope.equals(keywordType);
    }
  }

  /**
   * One surface form.
   *
   * @param type PREFERRED, SYNONYM, INFLECTION, ABBREVIATION, MISSPELLING or WEAK. This is where
   *             precision comes from: ABBREVIATION, MISSPELLING and WEAK are never fuzzy-matched,
   *             which is what stops a three-letter entry like {@code ibs} matching {@code its}.
   *             WEAK additionally only counts when nothing else matched the same field.
   */
  public record Term(String lang, String type, String text, String note) {

    /** Types that must match character for character, whatever their length. */
    public boolean exactOnly() {
      return "ABBREVIATION".equals(type) || "MISSPELLING".equals(type) || "WEAK".equals(type);
    }

    public boolean weak() {
      return "WEAK".equals(type);
    }
  }
}
