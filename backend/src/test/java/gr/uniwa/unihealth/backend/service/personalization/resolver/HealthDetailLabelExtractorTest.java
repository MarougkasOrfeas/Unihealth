package gr.uniwa.unihealth.backend.service.personalization.resolver;

import gr.uniwa.unihealth.backend.dto.HealthProfileDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The extractor itself decides one thing the matcher cannot: what it means when nothing matched.
 *
 * <p>"We do not recognise this" and "they told us there is nothing" look identical from the outside
 * — both produce an empty set — but recording {@code ALLERGY_OTHER} for the second tells a student
 * we have noted an unspecified allergy, which is the opposite of what they wrote.
 */
class HealthDetailLabelExtractorTest {

  private MedicalTermIndex termIndex;
  private HealthDetailLabelExtractor extractor;

  @BeforeEach
  void setUp() {
    termIndex = mock(MedicalTermIndex.class);
    extractor = new HealthDetailLabelExtractor(termIndex);
  }

  private void whenMatching(String scope, Set<String> labels, boolean negationSeen) {
    when(termIndex.match(any(), eq(scope)))
        .thenReturn(new MedicalTermIndex.MatchResult(labels, negationSeen));
  }

  private HealthProfileDTO profile(String allergyDetails, String chronicDetails) {
    HealthProfileDTO dto = new HealthProfileDTO();
    dto.setHasFoodAllergies(allergyDetails != null);
    dto.setFoodAllergiesDetails(allergyDetails);
    dto.setHasChronicConditions(chronicDetails != null);
    dto.setChronicConditionsDetails(chronicDetails);
    return dto;
  }

  @Test
  @DisplayName("returns what the matcher found")
  void returnsMatchedLabels() {
    whenMatching("ALLERGY", Set.of("ALLERGY_PEANUT"), false);

    assertThat(extractor.extract(profile("peanuts", null)))
        .containsExactly("ALLERGY_PEANUT");
  }

  @Test
  @DisplayName("falls back to *_OTHER when a real description matched nothing")
  void fallsBackWhenNothingMatched() {
    whenMatching("CHRONIC", Set.of(), false);

    assertThat(extractor.extract(profile(null, "some rare condition")))
        .containsExactly("CHRONIC_OTHER");
  }

  @Test
  @DisplayName("records nothing at all when the student was denying a condition")
  void doesNotFallBackOnADenial() {
    whenMatching("ALLERGY", Set.of(), true);

    // The distinction this test exists for. Before the matcher understood negation, this input
    // produced ALLERGY_OTHER and the student was shown an unspecified food allergy.
    assertThat(extractor.extract(profile("δεν έχω αλλεργίες", null))).isEmpty();
  }

  @Test
  @DisplayName("ignores a field whose box is not ticked, without consulting the dictionary")
  void ignoresUntickedFields() {
    HealthProfileDTO dto = profile(null, null);

    assertThat(extractor.extract(dto)).isEmpty();
    verify(termIndex, never()).match(any(), any());
  }

  @Test
  @DisplayName("a ticked box with no text yields nothing rather than *_OTHER")
  void ignoresBlankDetails() {
    HealthProfileDTO dto = new HealthProfileDTO();
    dto.setHasFoodAllergies(true);
    dto.setFoodAllergiesDetails("   ");

    assertThat(extractor.extract(dto)).isEmpty();
    verify(termIndex, never()).match(any(), any());
  }

  @Test
  @DisplayName("reads both fields independently")
  void readsBothFields() {
    whenMatching("ALLERGY", Set.of("ALLERGY_PEANUT"), false);
    whenMatching("CHRONIC", Set.of(), false);

    assertThat(extractor.extract(profile("peanuts", "something unrecognised")))
        .containsExactlyInAnyOrder("ALLERGY_PEANUT", "CHRONIC_OTHER");
  }

  @Test
  @DisplayName("does not repeat a label that both fields produced")
  void deduplicatesAcrossFields() {
    // A concept scoped BOTH — coeliac, gluten, lactose — is reachable from either box.
    whenMatching("ALLERGY", Set.of("CHRONIC_CELIAC"), false);
    whenMatching("CHRONIC", Set.of("CHRONIC_CELIAC"), false);

    assertThat(extractor.extract(profile("gluten", "coeliac disease")))
        .containsExactly("CHRONIC_CELIAC");
  }
}
