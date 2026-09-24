package gr.uniwa.unihealth.backend.service.ai.safety;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The rule that matters here is asymmetric, and both directions are tested on purpose.
 *
 * <p>A missed crisis message is answered by an 8B model improvising about suicide. A false positive
 * shows a student a card of helpline numbers they did not need. Those are not the same size of
 * mistake, so the guard is tuned to fire, and these tests pin down the narrow set of cases where it
 * must nonetheless stay quiet - an explicit denial, and nothing else.
 */
class CrisisGuardTest {

  private final CrisisGuard guard = new CrisisGuard();

  private Optional<CrisisTier> tierOf(String message) {
    return guard.screen(message).map(CrisisAssessment::tier);
  }

  @Nested
  @DisplayName("self-harm")
  class SelfHarm {

    @Test
    @DisplayName("fires on Greek")
    void greek() {
      assertThat(tierOf("θέλω να πεθάνω")).contains(CrisisTier.SELF_HARM);
      assertThat(tierOf("σκέφτομαι την αυτοκτονία")).contains(CrisisTier.SELF_HARM);
    }

    @Test
    @DisplayName("fires on English")
    void english() {
      assertThat(tierOf("I want to kill myself")).contains(CrisisTier.SELF_HARM);
      assertThat(tierOf("i think i am suicidal")).contains(CrisisTier.SELF_HARM);
    }

    @Test
    @DisplayName("a phrase carrying its own negation still fires")
    void selfNegatingPhraseStillFires() {
      // «δεν» opens the matched span rather than preceding it, so the denial rule must not eat it.
      // Getting this backwards silently disables one of the commonest ways the thought is worded.
      assertThat(tierOf("δεν θέλω να ζω άλλο")).contains(CrisisTier.SELF_HARM);
      assertThat(tierOf("I don't want to live anymore")).contains(CrisisTier.SELF_HARM);
    }

    @Test
    @DisplayName("an explicit denial does not fire")
    void explicitDenialStaysQuiet() {
      assertThat(tierOf("I am not suicidal, I just feel low")).isEmpty();
      assertThat(tierOf("δεν είμαι αυτοκτονικός, απλώς έχω άγχος")).isEmpty();
    }

    @Test
    @DisplayName("a denial in one clause does not silence the next")
    void denialIsClauseScoped() {
      // "but" opens a new clause in MedicalTextAnalyzer, which is what keeps the denial from
      // reaching across the comma and suppressing the half that matters.
      assertThat(tierOf("I am not sleeping well, but I want to die"))
          .contains(CrisisTier.SELF_HARM);
    }

    @Test
    @DisplayName("someone else's crisis still fires")
    void attributionDoesNotSuppress() {
      // Deliberate, and the opposite of what MedicalTermIndex does for labels: the student needs
      // the same helpline number whether the person at risk is them or their friend.
      assertThat(tierOf("ο φίλος μου θέλει να αυτοκτονήσει")).contains(CrisisTier.SELF_HARM);
      assertThat(tierOf("my brother said he wants to die")).contains(CrisisTier.SELF_HARM);
    }
  }

  @Nested
  @DisplayName("medical emergency")
  class MedicalEmergency {

    @Test
    @DisplayName("fires on Greek and English")
    void bothLanguages() {
      assertThat(tierOf("έχω πόνο στο στήθος")).contains(CrisisTier.MEDICAL_EMERGENCY);
      assertThat(tierOf("I have chest pain")).contains(CrisisTier.MEDICAL_EMERGENCY);
    }

    @Test
    @DisplayName("fires when the phrase begins with a negation")
    void negatedPhrasing() {
      // Same trap as «δεν θέλω να ζω»: the phrase is authored with its «δεν» so the span starts
      // there. "I cannot breathe" is not a denial of breathing difficulty.
      assertThat(tierOf("δεν μπορώ να αναπνεύσω")).contains(CrisisTier.MEDICAL_EMERGENCY);
      assertThat(tierOf("I can't breathe properly")).contains(CrisisTier.MEDICAL_EMERGENCY);
    }

    @Test
    @DisplayName("loses to self-harm when a message carries both")
    void selfHarmWins() {
      assertThat(tierOf("I have chest pain and I want to kill myself"))
          .contains(CrisisTier.SELF_HARM);
    }
  }

  @Nested
  @DisplayName("ordinary messages")
  class OrdinaryMessages {

    @Test
    @DisplayName("everyday health questions are left alone")
    void ordinaryQuestionsPass() {
      assertThat(tierOf("πονάει ο λαιμός μου, τι να κάνω;")).isEmpty();
      assertThat(tierOf("how do I sleep better during exams?")).isEmpty();
      assertThat(tierOf("I have a headache and feel tired")).isEmpty();
    }

    @Test
    @DisplayName("empty and blank input is not a crisis")
    void emptyInput() {
      assertThat(guard.screen(null)).isEmpty();
      assertThat(guard.screen("")).isEmpty();
      assertThat(guard.screen("   ")).isEmpty();
    }
  }

  @Nested
  @DisplayName("dataset")
  class Dataset {

    @Test
    @DisplayName("every tier carries a lexicon key, so no match can return raw text")
    void everyTierHasAMessageKey() {
      for (CrisisTier tier : CrisisTier.values()) {
        String probe = tier == CrisisTier.SELF_HARM ? "I want to kill myself" : "chest pain";
        assertThat(guard.screen(probe)).hasValueSatisfying(assessment ->
            assertThat(assessment.messageKey()).isNotBlank().startsWith("ai.safety."));
      }
    }
  }
}
