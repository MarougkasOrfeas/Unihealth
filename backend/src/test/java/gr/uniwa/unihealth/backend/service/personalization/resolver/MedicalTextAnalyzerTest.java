package gr.uniwa.unihealth.backend.service.personalization.resolver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The analyzer decides what counts as "the same word", so its rules are the ones most likely to be
 * changed by someone who has not thought through the Greek consequences. These tests pin the
 * behaviour that other parts of the system quietly depend on.
 */
class MedicalTextAnalyzerTest {

  @Nested
  @DisplayName("normalize")
  class Normalize {

    @Test
    @DisplayName("strips accents and lowercases, in both scripts")
    void stripsAccents() {
      assertThat(MedicalTextAnalyzer.normalize("Διαβήτης")).isEqualTo("διαβητησ");
      assertThat(MedicalTextAnalyzer.normalize("ΆΣΘΜΑ")).isEqualTo("ασθμα");
      assertThat(MedicalTextAnalyzer.normalize("Diabetes")).isEqualTo("diabetes");
    }

    @Test
    @DisplayName("folds word-final sigma, so ...ίες and ...ίεσ tokenise alike")
    void foldsFinalSigma() {
      assertThat(MedicalTextAnalyzer.normalize("αλλεργίες"))
          .isEqualTo(MedicalTextAnalyzer.normalize("αλλεργίεσ"))
          .endsWith("σ");
    }

    @Test
    @DisplayName("keeps Greek characters rather than deleting them")
    void keepsGreek() {
      // The punctuation stripper was once [^a-z0-9\s], which reduced any Greek answer to the empty
      // string. Every Greek term in the dictionary depends on this not regressing.
      assertThat(MedicalTextAnalyzer.normalize("υπέρταση")).isNotEmpty().isEqualTo("υπερταση");
    }

    @Test
    @DisplayName("turns an apostrophe into a word break, so no special case is needed for Crohn's")
    void splitsPossessive() {
      assertThat(MedicalTextAnalyzer.normalize("Crohn's disease")).isEqualTo("crohn s disease");
    }
  }

  @Nested
  @DisplayName("Greek stemming")
  class GreekStemming {

    @ParameterizedTest(name = "{0} and {1} share a stem")
    @CsvSource({
        "διαβητησ,  διαβητη",     // nominative / accusative
        "διαβητησ,  διαβητου",    // nominative / genitive
        "αλλεργια,  αλλεργιων",
        "ξηροι,     ξηρων",
        "ξηροι,     ξηρουσ",
        "καρποι,    καρπουσ",
        "υπερταση,  υπερτασησ",
    })
    @DisplayName("unifies case endings, which is the whole point of stemming Greek")
    void unifiesCaseEndings(String a, String b) {
      assertThat(MedicalTextAnalyzer.greekStem(a)).isEqualTo(MedicalTextAnalyzer.greekStem(b));
    }

    @ParameterizedTest
    @ValueSource(strings = {"χαπ", "αυγο", "αυγα", "ψαρι"})
    @DisplayName("leaves short words alone rather than guessing at their ending")
    void leavesShortWordsAlone(String word) {
      assertThat(MedicalTextAnalyzer.greekStem(word)).isEqualTo(word);
    }

    @Test
    @DisplayName("never strips a suffix that would leave a stem below the floor")
    void respectsTheStemFloor() {
      // τόνος must not collapse to the three-letter τον, which is the definite article.
      assertThat(MedicalTextAnalyzer.greekStem("τονοσ")).hasSizeGreaterThanOrEqualTo(3);
      assertThat(MedicalTextAnalyzer.greekStem("σογια")).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("does not strip derivational endings, which change meaning")
    void leavesDerivationAlone() {
      // ασθματικός is an adjective, not a case form of άσθμα, so it must stay a separate entry the
      // dataset can decide about rather than being folded in by a rule.
      assertThat(MedicalTextAnalyzer.greekStem("ασθματικοσ"))
          .isNotEqualTo(MedicalTextAnalyzer.greekStem("ασθμα"));
    }
  }

  @Nested
  @DisplayName("English stemming")
  class EnglishStemming {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
        "peanuts,   peanut",
        "almonds,   almond",
        "allergies, allergy",
        "tomatoes,  tomato",
    })
    void stripsRegularPlurals(String input, String expected) {
      assertThat(MedicalTextAnalyzer.englishStem(input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("leaves a double-s ending intact")
    void keepsDoubleS() {
      assertThat(MedicalTextAnalyzer.englishStem("illness")).isEqualTo("illness");
    }
  }

  @Nested
  @DisplayName("Greeklish skeleton")
  class Skeleton {

    @Test
    @DisplayName("Greek and its Latin-letter spellings converge on one form")
    void greekAndGreeklishConverge() {
      String fromGreek = MedicalTextAnalyzer.skeleton("διαβητησ", true);

      assertThat(MedicalTextAnalyzer.skeleton("diavitis", false)).isEqualTo(fromGreek);
      assertThat(MedicalTextAnalyzer.skeleton("diavhths", false)).isEqualTo(fromGreek);
    }

    @Test
    @DisplayName("the vowel digraph rules make ksiroi karpoi reachable")
    void foldsVowelDigraphs() {
      // Greek has already collapsed οι to i by this point, so the Latin side has to collapse "oi"
      // the same way or the two keys never line up.
      assertThat(MedicalTextAnalyzer.skeleton("καρποι", true))
          .isEqualTo(MedicalTextAnalyzer.skeleton("karpoi", false));
    }

    @Test
    @DisplayName("y and h both stand in for the same Greek vowels")
    void foldsLatinVowelVariants() {
      assertThat(MedicalTextAnalyzer.skeleton("ypertasi", false))
          .isEqualTo(MedicalTextAnalyzer.skeleton("ipertasi", false));
    }
  }

  @Nested
  @DisplayName("tokenize")
  class Tokenize {

    @Test
    @DisplayName("punctuation opens a new clause")
    void punctuationOpensClause() {
      List<MedicalTextAnalyzer.Token> tokens = MedicalTextAnalyzer.tokenize("peanuts, no dairy");

      assertThat(tokens).extracting(MedicalTextAnalyzer.Token::surface)
          .containsExactly("peanuts", "no", "dairy");
      assertThat(tokens.get(0).clause()).isNotEqualTo(tokens.get(2).clause());
    }

    @Test
    @DisplayName("a contrastive conjunction opens a new clause, but 'and' does not")
    void contrastiveConjunctionOpensClause() {
      // "no nuts and no eggs" is one negated thought and must stay one clause, otherwise the
      // negation stops reaching the eggs.
      List<MedicalTextAnalyzer.Token> and = MedicalTextAnalyzer.tokenize("no nuts and no eggs");
      assertThat(and).extracting(MedicalTextAnalyzer.Token::clause).containsOnly(0);

      List<MedicalTextAnalyzer.Token> but = MedicalTextAnalyzer.tokenize("nuts but no eggs");
      assertThat(but.get(0).clause()).isNotEqualTo(but.get(but.size() - 1).clause());
    }

    @Test
    @DisplayName("routes each token by its own script, so a mixed sentence still works")
    void routesPerToken() {
      List<MedicalTextAnalyzer.Token> tokens = MedicalTextAnalyzer.tokenize("Έχω asthma και υπέρταση");

      assertThat(tokens).extracting(MedicalTextAnalyzer.Token::greek)
          .containsExactly(true, false, true, true);
    }

    @Test
    @DisplayName("a digit-only token is neither script and passes through")
    void keepsDigits() {
      assertThat(MedicalTextAnalyzer.tokenize("type 2 diabetes"))
          .extracting(MedicalTextAnalyzer.Token::surface)
          .containsExactly("type", "2", "diabetes");
    }

    @Test
    @DisplayName("blank input yields nothing rather than a token of empty string")
    void handlesBlankInput() {
      assertThat(MedicalTextAnalyzer.tokenize("   ")).isEmpty();
      assertThat(MedicalTextAnalyzer.tokenize(null)).isEmpty();
      assertThat(MedicalTextAnalyzer.tokenize("...")).isEmpty();
    }
  }
}
