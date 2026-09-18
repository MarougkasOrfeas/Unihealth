package gr.uniwa.unihealth.backend.service.personalization.resolver;

import gr.uniwa.unihealth.backend.dto.HealthProfileDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Turns the two free-text answers on the health form into label codes.
 *
 * <p>Students describe their allergies and conditions in their own words: in Greek, in English, in
 * Greek written with Latin letters, and with whatever typos come with a phone keyboard. The codes
 * produced here are always English, because they are identifiers rather than display text — their
 * translated names live in the lexicon under {@code profile.label.*}.
 *
 * <p>The matching itself lives in {@link MedicalTermIndex}, which holds the dictionary in memory and
 * is described there. Two properties of this class are worth stating where they are visible:
 *
 * <ul>
 *   <li><strong>It issues no queries.</strong> This previously ran two SELECTs on every profile
 *       save, <em>before</em> checking whether the student had written anything at all, so a
 *       profile with both boxes unticked still paid for them. The dictionary only changes between
 *       deployments, so it is now read once per tenant at startup.</li>
 *   <li><strong>It stays inside the save transaction.</strong> That was the wrong place when this
 *       meant two queries plus tens of thousands of edit-distance computations. It is the right
 *       place now that it is a few microseconds of pure CPU with no I/O: doing it asynchronously
 *       would buy nothing and open a window where a profile is saved but the personalisation drawn
 *       from it is stale, with nothing in the UI to explain the gap.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HealthDetailLabelExtractor {

  private static final String ALLERGY = "ALLERGY";
  private static final String CHRONIC = "CHRONIC";

  /** Emitted when a student clearly described something the dictionary does not know. */
  private static final String ALLERGY_OTHER = "ALLERGY_OTHER";
  private static final String CHRONIC_OTHER = "CHRONIC_OTHER";

  private final MedicalTermIndex termIndex;

  public List<String> extract(HealthProfileDTO dto) {
    Set<String> labels = new LinkedHashSet<>();

    // Only read a field the student actually opted into. The details are cleared when the box is
    // unticked, but reading them unconditionally would resurrect a stale answer.
    if (dto.isHasFoodAllergies()) {
      labels.addAll(resolve(dto.getFoodAllergiesDetails(), ALLERGY, ALLERGY_OTHER));
    }
    if (dto.isHasChronicConditions()) {
      labels.addAll(resolve(dto.getChronicConditionsDetails(), CHRONIC, CHRONIC_OTHER));
    }

    return new ArrayList<>(labels);
  }

  private Set<String> resolve(String text, String scope, String fallback) {
    if (text == null || text.isBlank()) {
      return Set.of();
    }

    MedicalTermIndex.MatchResult result = termIndex.match(text, scope);

    // Never log the text itself: this is special-category health data. A character count is enough
    // to tell a truncated answer from a genuinely unrecognised one when reading the logs.
    int length = text.trim().length();

    if (!result.labels().isEmpty()) {
      log.debug("Matched {} answer of {} characters to {} label(s)",
          scope, length, result.labels().size());
      return result.labels();
    }

    // "δεν έχω αλλεργίες" and "no known allergies" resolve to nothing, and that is the right
    // answer rather than a failure to understand. Without this branch the student would be told we
    // had recorded an unspecified allergy, which is the opposite of what they wrote.
    if (result.negationSeen()) {
      log.debug("{} answer of {} characters was a denial; recording no label", scope, length);
      return Set.of();
    }

    log.warn("No {} term matched an answer of {} characters", scope, length);
    return Set.of(fallback);
  }
}
