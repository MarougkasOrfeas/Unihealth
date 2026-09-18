package gr.uniwa.unihealth.backend.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * How a student files a medical test document on the "Οι Εξετάσεις Μου" page.
 *
 * <p>Broad on purpose. These are buckets a student picks from a dropdown without thinking, not a
 * clinical taxonomy — the point is to make a list of thirty files navigable, so a handful of
 * obvious groupings beats an accurate one nobody can choose from.
 */
@Getter
@RequiredArgsConstructor
public enum MedicalTestCategory {

  BLOOD_TEST("examFile.category.BLOOD_TEST"),
  IMAGING("examFile.category.IMAGING"),
  CARDIOLOGY("examFile.category.CARDIOLOGY"),
  MICROBIOLOGY("examFile.category.MICROBIOLOGY"),
  OTHER("examFile.category.OTHER");

  /**
   * Lexicon key for the name the student sees.
   *
   * <p>Held here so free-text search can match what is on screen: the column stores
   * {@code BLOOD_TEST}, but someone searching types «Αιματολογική».
   */
  private final String labelCode;
}
