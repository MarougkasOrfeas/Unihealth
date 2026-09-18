package gr.uniwa.unihealth.backend.service.personalization.resolver;

import gr.uniwa.unihealth.backend.config.context.TenantContext;
import gr.uniwa.unihealth.backend.model.LabelKeywordMapping;
import gr.uniwa.unihealth.backend.repository.LabelKeywordMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Matching rules, exercised against a small hand-built dictionary so each rule fails on its own
 * rather than being masked by the real dataset.
 *
 * <p>Most of these encode a bug that existed before: a flat edit-distance budget that let
 * {@code ibs} match {@code its}, an n-gram ceiling that made a four-word term unreachable, and no
 * notion of negation at all.
 */
class MedicalTermIndexTest {

  private static final String ALLERGY = "ALLERGY";
  private static final String CHRONIC = "CHRONIC";

  private final List<LabelKeywordMapping> rows = new ArrayList<>();

  @BeforeEach
  void seedDictionary() {
    rows.clear();
    term("peanut", "ALLERGY_PEANUT", ALLERGY, "en", "PREFERRED", 1050);
    term("dairy", "ALLERGY_DAIRY", ALLERGY, "en", "PREFERRED", 1050);
    term("egg", "ALLERGY_EGG", ALLERGY, "en", "ABBREVIATION", 1050);
    term("αραχιδα", "ALLERGY_PEANUT", ALLERGY, "el", "PREFERRED", 1050);
    term("ξηροι καρποι", "ALLERGY_TREE_NUT", ALLERGY, "el", "PREFERRED", 1050);

    term("diabetes", "CHRONIC_DIABETES", CHRONIC, "en", "PREFERRED", 1350);
    term("διαβητησ", "CHRONIC_DIABETES", CHRONIC, "el", "PREFERRED", 1350);
    term("ibs", "CHRONIC_IBS", CHRONIC, "en", "ABBREVIATION", 1150);
    term("ulcerative colitis", "CHRONIC_ULCERATIVE_COLITIS", CHRONIC, "en", "PREFERRED", 1250);
    term("colitis", "CHRONIC_ULCERATIVE_COLITIS", CHRONIC, "en", "WEAK", 1250);
    term("thyroid", "CHRONIC_THYROID", CHRONIC, "en", "WEAK", 1200);
    term("hypothyroidism", "CHRONIC_THYROID", CHRONIC, "en", "PREFERRED", 1200);
    term("chronic obstructive pulmonary disease", "CHRONIC_COPD", CHRONIC, "en", "PREFERRED", 1350);
  }

  private void term(String keyword, String labelCode, String scope, String lang, String type,
      int priority) {
    LabelKeywordMapping row = new LabelKeywordMapping();
    row.setKeyword(keyword);
    row.setLabelCode(labelCode);
    row.setKeywordType(scope);
    row.setLang(lang);
    row.setTermType(type);
    row.setConceptId(labelCode.toLowerCase());
    row.setPriority(priority);
    row.setActive(true);
    rows.add(row);
  }

  private MedicalTermIndex index() {
    LabelKeywordMappingRepository repository = mock(LabelKeywordMappingRepository.class);
    when(repository.findByActiveTrue()).thenReturn(List.copyOf(rows));

    TenantContext tenantContext = mock(TenantContext.class);
    when(tenantContext.getCurrentTenant()).thenReturn("TEST");

    return new MedicalTermIndex(repository, tenantContext);
  }

  private List<String> match(String text, String scope) {
    return List.copyOf(index().match(text, scope).labels());
  }

  @Nested
  @DisplayName("exact matching")
  class Exact {

    @Test
    @DisplayName("finds a term regardless of the inflection it was written in")
    void matchesInflections() {
      assertThat(match("διαβητη", CHRONIC)).containsExactly("CHRONIC_DIABETES");
      assertThat(match("peanuts", ALLERGY)).containsExactly("ALLERGY_PEANUT");
    }

    @Test
    @DisplayName("reaches a four-word term, which the old trigram tokeniser could not")
    void matchesFourWordTerm() {
      assertThat(match("chronic obstructive pulmonary disease", CHRONIC))
          .containsExactly("CHRONIC_COPD");
    }

    @Test
    @DisplayName("prefers the longest span, so the specific term beats the general one")
    void prefersLongestSpan() {
      // Both "ulcerative colitis" and "colitis" are in the dictionary. Maximal munch has to pick
      // the two-word one, and it must be reported once rather than twice.
      assertThat(match("I have ulcerative colitis", CHRONIC))
          .containsExactly("CHRONIC_ULCERATIVE_COLITIS");
    }

    @Test
    @DisplayName("only searches terms belonging to the field being read")
    void respectsScope() {
      assertThat(match("diabetes", ALLERGY)).isEmpty();
      assertThat(match("peanut", CHRONIC)).isEmpty();
    }

    @Test
    @DisplayName("matches a Greek multi-word term across its case endings")
    void matchesGreekMultiWord() {
      assertThat(match("αλλεργια στους ξηρους καρπους", ALLERGY))
          .containsExactly("ALLERGY_TREE_NUT");
    }
  }

  @Nested
  @DisplayName("fuzzy policy")
  class Fuzzy {

    @Test
    @DisplayName("tolerates a typo in a long term")
    void toleratesTypos() {
      assertThat(match("diabetis", CHRONIC)).containsExactly("CHRONIC_DIABETES");
    }

    @Test
    @DisplayName("rescues a transposition, which plain edit distance scores as two")
    void rescuesTransposition() {
      assertThat(match("peantu", ALLERGY)).containsExactly("ALLERGY_PEANUT");
    }

    @Test
    @DisplayName("never fuzzy-matches an abbreviation, however close the input")
    void abbreviationsAreExactOnly() {
      // Under the old flat budget of two these all matched. They are the reason the budget now
      // scales with the term's length and bottoms out at zero.
      assertThat(match("its fine", CHRONIC)).isEmpty();
      assertThat(match("I broke my leg", ALLERGY)).isEmpty();

      assertThat(match("I have IBS", CHRONIC)).containsExactly("CHRONIC_IBS");
    }

    @Test
    @DisplayName("does not let a short word drift into a different concept")
    void doesNotDriftBetweenConcepts() {
      assertThat(match("fairy tale", ALLERGY)).isEmpty();
    }

    @Test
    @DisplayName("picks the closest term rather than whichever the database returned first")
    void picksTheClosestTerm() {
      // Deliberately ordered so the worse candidate is seen first.
      term("diabetos", "CHRONIC_OTHER_TEST", CHRONIC, "en", "SYNONYM", 900);
      assertThat(match("diabetes", CHRONIC)).containsExactly("CHRONIC_DIABETES");
    }
  }

  @Nested
  @DisplayName("negation and attribution")
  class Negation {

    @Test
    @DisplayName("a denial yields no label, and is reported as a denial rather than a failure")
    void denialYieldsNothing() {
      MedicalTermIndex.MatchResult result = index().match("no peanut", ALLERGY);

      assertThat(result.labels()).isEmpty();
      // The caller needs this to tell "they said no" apart from "we did not understand", which is
      // the difference between recording nothing and recording ALLERGY_OTHER.
      assertThat(result.negationSeen()).isTrue();
    }

    @Test
    @DisplayName("suppresses only what follows the cue in the same clause")
    void suppressesOnlyAfterTheCue() {
      assertThat(match("peanut, but no dairy", ALLERGY)).containsExactly("ALLERGY_PEANUT");
    }

    @Test
    @DisplayName("a negation carries across 'and' within one clause")
    void negationCarriesAcrossAnd() {
      assertThat(match("no peanut and no dairy", ALLERGY)).isEmpty();
    }

    @Test
    @DisplayName("something belonging to a relative is not the student's own condition")
    void suppressesAttribution() {
      assertThat(match("family history of diabetes", CHRONIC)).isEmpty();
      assertThat(match("my mother has diabetes", CHRONIC)).isEmpty();
    }

    @Test
    @DisplayName("an unrelated match is not reported as a denial")
    void plainMatchIsNotADenial() {
      assertThat(index().match("diabetes", CHRONIC).negationSeen()).isFalse();
    }
  }

  @Nested
  @DisplayName("weak terms")
  class Weak {

    @Test
    @DisplayName("fire when nothing more specific was said")
    void fireAsALastResort() {
      assertThat(match("thyroid", CHRONIC)).containsExactly("CHRONIC_THYROID");
    }

    @Test
    @DisplayName("give way to a specific term in the same field")
    void giveWayToSpecificTerms() {
      assertThat(match("hypothyroidism, thyroid checked yearly", CHRONIC))
          .containsExactly("CHRONIC_THYROID");
      assertThat(match("diabetes and thyroid", CHRONIC)).containsExactly("CHRONIC_DIABETES");
    }
  }

  @Nested
  @DisplayName("index construction")
  class Construction {

    @Test
    @DisplayName("drops the second of two concepts claiming one key rather than mislabelling")
    void dropsCollidingKeys() {
      term("peanut", "ALLERGY_SOMETHING_ELSE", ALLERGY, "en", "PREFERRED", 900);

      // The first registration wins; the collision is reported in the log, not silently resolved
      // in favour of whichever row happened to be loaded last.
      assertThat(match("peanut", ALLERGY)).containsExactly("ALLERGY_PEANUT");
    }

    @Test
    @DisplayName("exposes the dataset priority for a label code")
    void exposesPriority() {
      assertThat(index().priorityOf("CHRONIC_DIABETES")).isEqualTo(1350);
      assertThat(index().priorityOf("BMI_OVERWEIGHT")).isNull();
    }

    @Test
    @DisplayName("an empty dictionary matches nothing instead of failing")
    void toleratesAnEmptyDictionary() {
      rows.clear();
      assertThat(match("diabetes", CHRONIC)).isEmpty();
    }

    @Test
    @DisplayName("blank input short-circuits without touching the index")
    void toleratesBlankInput() {
      assertThat(match("", CHRONIC)).isEmpty();
      assertThat(match("   ", CHRONIC)).isEmpty();
    }
  }
}
