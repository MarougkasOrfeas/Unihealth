package gr.uniwa.unihealth.backend.service.ai.retrieval;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This detector decides whether the application talks to a government website, so both directions
 * are worth pinning - but they are not equally costly.
 *
 * <p>A false positive spends one network request and a little latency on a question the local
 * corpus would have answered. A false negative costs nothing at all: the student still gets the
 * grounded local answer. So the cue list is deliberately specific, and the cases below that assert
 * it stays <em>quiet</em> are the ones protecting the common path.
 */
class RecencyDetectorTest {

  private final RecencyDetector detector = new RecencyDetector();

  @Nested
  @DisplayName("fires")
  class Fires {

    @Test
    @DisplayName("on Greek recency wording")
    void greek() {
      assertThat(detector.asksAboutRecent("υπάρχει κάτι πρόσφατο για τη γρίπη;")).isTrue();
      assertThat(detector.asksAboutRecent("έχουν αυξηθεί τα κρούσματα;")).isTrue();
      assertThat(detector.asksAboutRecent("υπάρχει έξαρση φέτος;")).isTrue();
    }

    @Test
    @DisplayName("on English recency wording")
    void english() {
      assertThat(detector.asksAboutRecent("is there anything recent about flu?")).isTrue();
      assertThat(detector.asksAboutRecent("any outbreak I should know about?")).isTrue();
      assertThat(detector.asksAboutRecent("is this year worse than usual?")).isTrue();
    }

    @Test
    @DisplayName("on Greeklish, with no Greeklish authored anywhere")
    void greeklish() {
      // Reached through the transliteration skeleton, the same mechanism SymptomResolver and
      // CrisisGuard rely on. If this breaks, the dataset is not the place to fix it.
      assertThat(detector.asksAboutRecent("yparxei kati prosfato gia ti gripi")).isTrue();
    }

    @Test
    @DisplayName("on a multi-word cue, which a single word would miss")
    void multiWordCue() {
      assertThat(detector.asksAboutRecent("I heard cases are rising, is that true?")).isTrue();
    }
  }

  @Nested
  @DisplayName("stays quiet")
  class StaysQuiet {

    @Test
    @DisplayName("on ordinary health questions, which the local corpus answers better")
    void ordinaryQuestions() {
      assertThat(detector.asksAboutRecent("τι μπορώ να κάνω για τον πονόλαιμο;")).isFalse();
      assertThat(detector.asksAboutRecent("what can I do about a sore throat?")).isFalse();
      assertThat(detector.asksAboutRecent("πώς να κοιμάμαι καλύτερα στις εξετάσεις;")).isFalse();
      assertThat(detector.asksAboutRecent("I have a headache and feel tired")).isFalse();
    }

    @Test
    @DisplayName("on «νέα», which is excluded for being two words at once")
    void excludedAmbiguousWords() {
      // «νέα» is both "new" and "news" and turns up in ordinary phrasing. Including it would open
      // the gate on questions like this one, which the library answers perfectly well.
      assertThat(detector.asksAboutRecent("έχω μια νέα συνταγή για ύπνο")).isFalse();
    }

    @Test
    @DisplayName("on empty input")
    void emptyInput() {
      assertThat(detector.asksAboutRecent(null)).isFalse();
      assertThat(detector.asksAboutRecent("")).isFalse();
      assertThat(detector.asksAboutRecent("   ")).isFalse();
    }
  }
}
