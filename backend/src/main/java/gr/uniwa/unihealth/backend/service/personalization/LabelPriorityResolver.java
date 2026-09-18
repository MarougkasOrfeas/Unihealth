package gr.uniwa.unihealth.backend.service.personalization;

import gr.uniwa.unihealth.backend.service.personalization.resolver.MedicalTermIndex;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * How significant a label is, for every label in the system.
 *
 * <p>This exists because the answer was previously given in two places that disagreed. Label
 * ordering scored an unrecognised condition zero, so it sorted last, while the profile read path
 * reported it as 1200 — above several conditions the dictionary does understand. Same label, two
 * answers, and the number is not cosmetic: the frontend multiplies it by a content item's own
 * weight to decide which topics and tips a student is shown.
 *
 * <p>The ladder, highest first:
 *
 * <ol>
 *   <li>a priority the term dictionary carries for this concept;</li>
 *   <li>for an unrecognised allergy or condition, a value derived to sit <em>below</em> every
 *       concept the dictionary knows in that field;</li>
 *   <li>the optional-form bands;</li>
 *   <li>a floor for anything else.</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class LabelPriorityResolver {

  /**
   * Prefixes that decide both ownership and clinical ranking. Mirrored in the frontend's
   * {@code SAFETY_PREFIXES}; changing one means changing the other.
   */
  public static final String CHRONIC_PREFIX = "CHRONIC_";
  public static final String ALLERGY_PREFIX = "ALLERGY_";
  public static final String OPTIONAL_HIGH_PREFIX = "OPTIONAL_HIGH_";
  public static final String OPTIONAL_PREFIX = "OPTIONAL_";

  private static final String CHRONIC_SCOPE = "CHRONIC";
  private static final String ALLERGY_SCOPE = "ALLERGY";

  /**
   * How far beneath the dictionary's floor an unrecognised answer sits.
   *
   * <p>{@code CHRONIC_OTHER} means "you told us about a condition and we could not identify it". It
   * must never outrank a condition we did identify, or a student whose answer we failed to parse
   * would be shown content ahead of one whose answer we understood.
   */
  private static final int UNRECOGNISED_MARGIN = 50;

  /** Used only when the dictionary is empty, which in practice means it failed to load. */
  private static final int CHRONIC_FLOOR_WITHOUT_DICTIONARY = 900;
  private static final int ALLERGY_FLOOR_WITHOUT_DICTIONARY = 800;

  private static final int OPTIONAL_HIGH_PRIORITY = 900;
  private static final int OPTIONAL_PRIORITY = 200;
  private static final int DEFAULT_PRIORITY = 50;

  private final MedicalTermIndex termIndex;

  public int priorityOf(String labelCode) {
    Integer fromDictionary = termIndex.priorityOf(labelCode);
    if (fromDictionary != null) {
      return fromDictionary;
    }

    if (labelCode.startsWith(CHRONIC_PREFIX)) {
      return belowDictionaryFloor(CHRONIC_SCOPE, CHRONIC_FLOOR_WITHOUT_DICTIONARY);
    }
    if (labelCode.startsWith(ALLERGY_PREFIX)) {
      return belowDictionaryFloor(ALLERGY_SCOPE, ALLERGY_FLOOR_WITHOUT_DICTIONARY);
    }
    if (labelCode.startsWith(OPTIONAL_HIGH_PREFIX)) {
      return OPTIONAL_HIGH_PRIORITY;
    }
    if (labelCode.startsWith(OPTIONAL_PREFIX)) {
      return OPTIONAL_PRIORITY;
    }
    return DEFAULT_PRIORITY;
  }

  private int belowDictionaryFloor(String scope, int fallback) {
    Integer lowest = termIndex.lowestPriorityIn(scope);
    if (lowest == null) {
      return fallback;
    }
    return Math.max(DEFAULT_PRIORITY, lowest - UNRECOGNISED_MARGIN);
  }
}
