package gr.uniwa.unihealth.backend.service.bootstrap;

import gr.uniwa.unihealth.backend.config.context.TenantContext;
import gr.uniwa.unihealth.backend.model.LabelKeywordMapping;
import gr.uniwa.unihealth.backend.repository.LabelKeywordMappingRepository;
import gr.uniwa.unihealth.backend.repository.ReferenceDataVersionRepository;
import gr.uniwa.unihealth.backend.service.personalization.LabelPriorityResolver;
import gr.uniwa.unihealth.backend.service.personalization.resolver.MedicalTermIndex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Exercises the real {@code medical-terms.json}, not a fixture.
 *
 * <p>The dictionary is the part of this feature most likely to be edited by hand, and most of the
 * ways it can go wrong are silent: a term that collides with another concept's stem, a Greeklish
 * spelling that lands on the wrong idea, a label code quietly dropped. Those produce a wrong health
 * label rather than an error, so they are worth a test that runs on every build.
 */
class MedicalTermDictionaryTest {

  /**
   * The codes the previous hardcoded dictionary produced. The dataset must remain a superset: losing
   * one would silently narrow what the app can recognise, with nothing to notice it.
   */
  private static final List<String> LEGACY_CODES = List.of(
      "ALLERGY_AVOCADO", "ALLERGY_BANANA", "ALLERGY_CELERY", "ALLERGY_CHOCOLATE", "ALLERGY_CORN",
      "ALLERGY_DAIRY", "ALLERGY_EGG", "ALLERGY_FISH", "ALLERGY_GARLIC", "ALLERGY_GLUTEN",
      "ALLERGY_KIWI", "ALLERGY_MUSHROOM", "ALLERGY_MUSTARD", "ALLERGY_ONION", "ALLERGY_PEANUT",
      "ALLERGY_SESAME", "ALLERGY_SHELLFISH", "ALLERGY_SOY", "ALLERGY_STRAWBERRY", "ALLERGY_SULFITE",
      "ALLERGY_TOMATO", "ALLERGY_TREE_NUT",
      "CHRONIC_ANEMIA", "CHRONIC_ANXIETY", "CHRONIC_ARTHRITIS", "CHRONIC_ASTHMA", "CHRONIC_CELIAC",
      "CHRONIC_COPD", "CHRONIC_CROHNS", "CHRONIC_DEPRESSION", "CHRONIC_DIABETES",
      "CHRONIC_EPILEPSY", "CHRONIC_FIBROMYALGIA", "CHRONIC_GERD", "CHRONIC_HEART_DISEASE",
      "CHRONIC_HYPERTENSION", "CHRONIC_IBS", "CHRONIC_KIDNEY_DISEASE", "CHRONIC_LIVER_DISEASE",
      "CHRONIC_LUPUS", "CHRONIC_MIGRAINE", "CHRONIC_OSTEOPOROSIS", "CHRONIC_PCOS",
      "CHRONIC_PSORIASIS", "CHRONIC_SLEEP_APNEA", "CHRONIC_THYROID", "CHRONIC_ULCERATIVE_COLITIS");

  // Static, and built in a class initialiser rather than @BeforeAll: the argument source for the
  // parameterised test below is resolved during discovery, which happens before @BeforeAll would
  // have run.
  private static final LabelKeywordInitData LOADER = new LabelKeywordInitData(
      mock(LabelKeywordMappingRepository.class), mock(ReferenceDataVersionRepository.class),
      mock(MedicalTermIndex.class), mock(TenantContext.class));

  private static final MedicalTermDictionary DICTIONARY = LOADER.readDictionary();
  private static final List<LabelKeywordMapping> ROWS = LOADER.flatten(DICTIONARY);
  private static final MedicalTermIndex INDEX = buildIndex();

  private static MedicalTermIndex buildIndex() {
    LabelKeywordMappingRepository repository = mock(LabelKeywordMappingRepository.class);
    when(repository.findByActiveTrue()).thenReturn(ROWS);

    TenantContext tenantContext = mock(TenantContext.class);
    when(tenantContext.getCurrentTenant()).thenReturn("TEST");

    return new MedicalTermIndex(repository, tenantContext);
  }

  private List<String> match(String text, String scope) {
    return List.copyOf(INDEX.match(text, scope).labels());
  }

  @Test
  @DisplayName("parses, and passes the loader's own validation")
  void isValid() {
    assertThat(DICTIONARY.concepts()).isNotEmpty();
    assertThatCode(() -> LOADER.validate(DICTIONARY)).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("still covers every label code the hardcoded dictionary produced")
  void coversEveryLegacyCode() {
    Set<String> codes = new LinkedHashSet<>();
    DICTIONARY.concepts().forEach(concept -> codes.add(concept.labelCode()));

    assertThat(codes).containsAll(LEGACY_CODES);
  }

  @Test
  @DisplayName("every concept is genuinely bilingual")
  void everyConceptIsBilingual() {
    for (MedicalTermDictionary.Concept concept : DICTIONARY.concepts()) {
      assertThat(concept.terms()).as("%s has an English term", concept.conceptId())
          .anyMatch(term -> "en".equals(term.lang()));
      assertThat(concept.terms()).as("%s has a Greek term", concept.conceptId())
          .anyMatch(term -> "el".equals(term.lang()));
    }
  }

  @Test
  @DisplayName("no term is claimed by two concepts in the same field and language")
  void hasNoDuplicateTerms() {
    // Mirrors uq_label_keyword_mapping. A duplicate would abort the seed mid-insert.
    Set<String> seen = new LinkedHashSet<>();
    List<String> duplicates = new ArrayList<>();

    for (LabelKeywordMapping row : ROWS) {
      String key = row.getKeyword().toLowerCase(Locale.ROOT) + "|" + row.getKeywordType()
          + "|" + row.getLang();
      if (!seen.add(key)) {
        duplicates.add(key);
      }
    }

    assertThat(duplicates).isEmpty();
  }

  /**
   * The strongest assertion here, and the one that replaces a whole family of hand-written checks.
   *
   * <p>If every term resolves to its own concept then no two concepts share a stem, no two share a
   * Greeklish skeleton, and no term drifts into a neighbour under the fuzzy budget — all three at
   * once, without enumerating pairs.
   */
  @ParameterizedTest(name = "{2} -> {1}")
  @MethodSource("everyTerm")
  @DisplayName("every term in the dictionary resolves to its own label code")
  void everyTermResolvesToItsOwnConcept(String scope, String labelCode, String text) {
    assertThat(match(text, scope)).contains(labelCode);
  }

  private static Stream<Arguments> everyTerm() {
    // Built from the flattened rows so the scope expansion of a BOTH concept is covered too.
    return ROWS.stream().map(row -> Arguments.of(
        row.getKeywordType(), row.getLabelCode(), row.getKeyword()));
  }

  @ParameterizedTest(name = "[{1}] {0} -> {2}")
  @MethodSource("realisticInput")
  @DisplayName("recognises what a student would actually type")
  void recognisesRealisticInput(String text, String scope, String expected) {
    assertThat(match(text, scope)).contains(expected);
  }

  // A method source rather than @CsvSource: these values carry apostrophes, Greek, and leading
  // articles, and CSV parsing treats a single quote as a quote character by default.
  private static Stream<Arguments> realisticInput() {
    return Stream.of(
        // Greek, across inflections
        Arguments.of("Διαβήτης τύπου 2", "CHRONIC", "CHRONIC_DIABETES"),
        Arguments.of("διαβήτου", "CHRONIC", "CHRONIC_DIABETES"),
        Arguments.of("έχω διαβήτη και υπέρταση", "CHRONIC", "CHRONIC_DIABETES"),
        Arguments.of("άσθματος", "CHRONIC", "CHRONIC_ASTHMA"),
        Arguments.of("χρόνια αποφρακτική πνευμονοπάθεια", "CHRONIC", "CHRONIC_COPD"),
        Arguments.of("ΧΑΠ", "CHRONIC", "CHRONIC_COPD"),
        Arguments.of("υψηλή αρτηριακή πίεση", "CHRONIC", "CHRONIC_HYPERTENSION"),
        Arguments.of("αλλεργία στους ξηρούς καρπούς", "ALLERGY", "ALLERGY_TREE_NUT"),
        Arguments.of("ξηρών καρπών", "ALLERGY", "ALLERGY_TREE_NUT"),
        Arguments.of("αλλεργία στα γαλακτοκομικά", "ALLERGY", "ALLERGY_DAIRY"),
        Arguments.of("λακτόζη", "ALLERGY", "ALLERGY_DAIRY"),
        Arguments.of("οστρακοειδή", "ALLERGY", "ALLERGY_SHELLFISH"),
        Arguments.of("φιστίκι Αιγίνης", "ALLERGY", "ALLERGY_TREE_NUT"),
        Arguments.of("κοιλιοκάκη", "ALLERGY", "CHRONIC_CELIAC"),
        Arguments.of("υποθυρεοειδισμός", "CHRONIC", "CHRONIC_THYROID"),
        // English
        Arguments.of("peanuts", "ALLERGY", "ALLERGY_PEANUT"),
        Arguments.of("Crohn's disease", "CHRONIC", "CHRONIC_CROHNS"),
        Arguments.of("chronic obstructive pulmonary disease", "CHRONIC", "CHRONIC_COPD"),
        Arguments.of("ulcerative colitis", "CHRONIC", "CHRONIC_ULCERATIVE_COLITIS"),
        Arguments.of("high blood pressure", "CHRONIC", "CHRONIC_HYPERTENSION"),
        Arguments.of("I have IBS", "CHRONIC", "CHRONIC_IBS"),
        Arguments.of("cod liver oil", "ALLERGY", "ALLERGY_FISH"),
        // Greek written in Latin letters
        Arguments.of("diavitis", "CHRONIC", "CHRONIC_DIABETES"),
        Arguments.of("diavhths", "CHRONIC", "CHRONIC_DIABETES"),
        Arguments.of("ksiroi karpoi", "ALLERGY", "ALLERGY_TREE_NUT"),
        Arguments.of("ypertasi", "CHRONIC", "CHRONIC_HYPERTENSION"),
        Arguments.of("galaktokomika", "ALLERGY", "ALLERGY_DAIRY"),
        // Typos
        Arguments.of("diabetis", "CHRONIC", "CHRONIC_DIABETES"),
        Arguments.of("peantu", "ALLERGY", "ALLERGY_PEANUT"),
        Arguments.of("psoriasi", "CHRONIC", "CHRONIC_PSORIASIS"),
        Arguments.of("asma", "CHRONIC", "CHRONIC_ASTHMA"));
  }

  @ParameterizedTest(name = "[{1}] {0}")
  @MethodSource("nonConditions")
  @DisplayName("stays silent on input that only looks like a condition")
  void staysSilentOnNonConditions(String text, String scope) {
    assertThat(match(text, scope)).isEmpty();
  }

  private static Stream<Arguments> nonConditions() {
    return Stream.of(
        // Denials and attribution
        Arguments.of("δεν έχω αλλεργίες", "ALLERGY"),
        Arguments.of("no known allergies", "ALLERGY"),
        Arguments.of("den exw allergies", "ALLERGY"),
        Arguments.of("οικογενειακό ιστορικό διαβήτη", "CHRONIC"),
        Arguments.of("family history of heart disease", "CHRONIC"),
        // Direction: only the qualified form means hypertension
        Arguments.of("χαμηλή πίεση", "CHRONIC"),
        Arguments.of("my blood pressure is fine", "CHRONIC"),
        // The abbreviation cohort, every one of which matched under the old flat edit budget
        Arguments.of("its fine", "CHRONIC"),
        Arguments.of("broke my leg", "ALLERGY"),
        Arguments.of("eye drops", "ALLERGY"),
        Arguments.of("t20 form", "CHRONIC"),
        Arguments.of("reflex problems", "CHRONIC"),
        // Substring and over-stemming traps
        Arguments.of("coconut", "ALLERGY"),
        Arguments.of("ξηρός λαιμός", "ALLERGY"),
        Arguments.of("παίρνω χάπια για την πίεση", "CHRONIC"));
  }

  @Test
  @DisplayName("celery and celiac never cross-match, in either direction")
  void keepsCeleryAndCeliacApart() {
    assertThat(match("celery", "ALLERGY")).containsExactly("ALLERGY_CELERY");
    assertThat(match("celiac", "CHRONIC")).containsExactly("CHRONIC_CELIAC");
  }

  @Test
  @DisplayName("a denial is reported as a denial, so no *_OTHER fallback is recorded")
  void reportsDenialsDistinctly() {
    MedicalTermIndex.MatchResult result = INDEX.match("δεν έχω αλλεργίες", "ALLERGY");

    assertThat(result.labels()).isEmpty();
    assertThat(result.negationSeen()).isTrue();
  }

  @Test
  @DisplayName("an unidentified answer ranks below every concept the dictionary can identify")
  void theUnrecognisedFallbackSitsBeneathTheWholeDictionary() {
    LabelPriorityResolver resolver = new LabelPriorityResolver(INDEX);

    int unknownAllergy = resolver.priorityOf("ALLERGY_OTHER");
    int unknownCondition = resolver.priorityOf("CHRONIC_OTHER");

    for (MedicalTermDictionary.Concept concept : DICTIONARY.concepts()) {
      int fallback = concept.labelCode().startsWith("ALLERGY_") ? unknownAllergy : unknownCondition;

      // Checked against the real priorities rather than a remembered number, so adding a
      // low-priority concept later fails here instead of quietly outranking itself.
      assertThat(concept.priority())
          .as("%s must outrank an answer we could not identify", concept.conceptId())
          .isGreaterThan(fallback);
    }
  }

  @Test
  @DisplayName("every priority is a positive number the label sort can unbox")
  void everyPriorityIsUsable() {
    for (MedicalTermDictionary.Concept concept : DICTIONARY.concepts()) {
      assertThat(concept.priority()).as("%s priority", concept.conceptId())
          .isNotNull().isPositive();
      assertThat(INDEX.priorityOf(concept.labelCode())).isEqualTo(concept.priority());
    }
  }
}
