package gr.uniwa.unihealth.backend.service.personalization;

import gr.uniwa.unihealth.backend.service.personalization.resolver.MedicalTermIndex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The rule that matters here is an ordering one: a condition we could not identify must never rank
 * above a condition we could.
 *
 * <p>Getting that backwards is not cosmetic. The frontend multiplies this number by a content item's
 * own weight, so a student whose free-text answer we failed to parse would be pushed content ahead
 * of one whose answer we understood — exactly the confusion the dictionary exists to avoid.
 */
class LabelPriorityResolverTest {

  private MedicalTermIndex termIndex;
  private LabelPriorityResolver resolver;

  @BeforeEach
  void setUp() {
    termIndex = mock(MedicalTermIndex.class);
    resolver = new LabelPriorityResolver(termIndex);

    when(termIndex.lowestPriorityIn("CHRONIC")).thenReturn(1100);
    when(termIndex.lowestPriorityIn("ALLERGY")).thenReturn(950);
  }

  @Test
  @DisplayName("a recognised concept keeps the priority the dictionary gave it")
  void usesTheDictionaryPriority() {
    when(termIndex.priorityOf("CHRONIC_DIABETES")).thenReturn(1350);

    assertThat(resolver.priorityOf("CHRONIC_DIABETES")).isEqualTo(1350);
  }

  @Test
  @DisplayName("an unrecognised condition ranks below every condition the dictionary knows")
  void unrecognisedConditionRanksBelowTheFloor() {
    assertThat(resolver.priorityOf("CHRONIC_OTHER")).isLessThan(1100);
  }

  @Test
  @DisplayName("an unrecognised allergy ranks below every allergy the dictionary knows")
  void unrecognisedAllergyRanksBelowTheFloor() {
    assertThat(resolver.priorityOf("ALLERGY_OTHER")).isLessThan(950);
  }

  @Test
  @DisplayName("the fallback tracks the dictionary rather than sitting at a fixed number")
  void fallbackFollowsTheDictionaryFloor() {
    // The point of deriving it: adding a lower-priority concept later must not leave the
    // "we did not understand" label stranded above it.
    int before = resolver.priorityOf("CHRONIC_OTHER");

    when(termIndex.lowestPriorityIn("CHRONIC")).thenReturn(600);
    int after = resolver.priorityOf("CHRONIC_OTHER");

    assertThat(after).isLessThan(before).isLessThan(600);
  }

  @Test
  @DisplayName("falls back to a fixed band when the dictionary is empty")
  void toleratesAnEmptyDictionary() {
    when(termIndex.lowestPriorityIn("CHRONIC")).thenReturn(null);
    when(termIndex.lowestPriorityIn("ALLERGY")).thenReturn(null);

    assertThat(resolver.priorityOf("CHRONIC_OTHER")).isPositive();
    assertThat(resolver.priorityOf("ALLERGY_OTHER")).isPositive();
  }

  @Test
  @DisplayName("never returns a non-positive priority, which the label sort would mis-order")
  void neverGoesNonPositive() {
    when(termIndex.lowestPriorityIn("CHRONIC")).thenReturn(10);

    assertThat(resolver.priorityOf("CHRONIC_OTHER")).isPositive();
  }

  @Test
  @DisplayName("keeps the optional-form bands intact")
  void keepsOptionalBands() {
    assertThat(resolver.priorityOf("OPTIONAL_HIGH_MEDICATION")).isEqualTo(900);
    assertThat(resolver.priorityOf("OPTIONAL_SLEEP_LESS_THAN_6_HOURS")).isEqualTo(200);
    assertThat(resolver.priorityOf("BMI_OVERWEIGHT")).isEqualTo(50);
  }

  @Test
  @DisplayName("OPTIONAL_HIGH_ is matched before the broader OPTIONAL_ prefix")
  void prefersTheMoreSpecificOptionalPrefix() {
    // OPTIONAL_HIGH_MEDICATION also starts with OPTIONAL_, so the order of the checks is
    // load-bearing rather than incidental.
    assertThat(resolver.priorityOf("OPTIONAL_HIGH_SURGERY_HISTORY"))
        .isGreaterThan(resolver.priorityOf("OPTIONAL_WATER_LOW"));
  }
}
