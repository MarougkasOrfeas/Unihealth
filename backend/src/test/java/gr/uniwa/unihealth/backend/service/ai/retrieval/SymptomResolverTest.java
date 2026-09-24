package gr.uniwa.unihealth.backend.service.ai.retrieval;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A wrong slug is worse than no slug: it narrows retrieval to the wrong page and produces a
 * confident answer about something the student did not ask. So the interesting assertions here are
 * the ones where the resolver is expected to decline.
 */
class SymptomResolverTest {

  private final SymptomResolver resolver = new SymptomResolver();

  @Test
  @DisplayName("resolves a Greek lay phrase to the English page")
  void greekLayPhrase() {
    assertThat(resolver.resolve("πονάει ο λαιμός μου, τι να κάνω;")).contains("sore-throat");
    assertThat(resolver.resolve("έχω πονοκέφαλο από χθες")).contains("headaches");
    assertThat(resolver.resolve("με πονάει η κοιλιά μου")).contains("stomach-ache");
  }

  @Test
  @DisplayName("resolves a clinical Greek term")
  void greekClinicalTerm() {
    assertThat(resolver.resolve("έχω δυσφαγία")).contains("swallowing-problems-dysphagia");
    assertThat(resolver.resolve("τι είναι η αιματουρία;")).contains("blood-in-urine");
  }

  @Test
  @DisplayName("resolves Greeklish through the transliteration skeleton")
  void greeklish() {
    // No Greeklish is authored anywhere: the skeleton key is what reaches it from the Greek, which
    // is the whole reason this resolver reuses MedicalTextAnalyzer instead of matching strings.
    assertThat(resolver.resolve("exo ponokefalo")).contains("headaches");
  }

  @Test
  @DisplayName("English titles and synonyms are registered from the corpus, not the alias file")
  void englishFromCorpus() {
    resolver.registerEnglish("chest-pain", "Chest pain", "Heart pain");

    assertThat(resolver.resolve("I have chest pain")).contains("chest-pain");
    assertThat(resolver.resolve("what is heart pain?")).contains("chest-pain");
  }

  @Test
  @DisplayName("longest phrase wins, so a compound name is not read as its first word")
  void maximalMunch() {
    // «πόνος στο στήθος» must not be resolved by some shorter key that happens to overlap it.
    assertThat(resolver.resolve("έχω πόνο στο στήθος")).contains("chest-pain");
  }

  @Test
  @DisplayName("declines rather than guessing when nothing matches")
  void declinesOnNoMatch() {
    assertThat(resolver.resolve("πώς μπορώ να κοιμάμαι καλύτερα;")).isEmpty();
    assertThat(resolver.resolve("how do I manage exam stress?")).isEmpty();
    assertThat(resolver.resolve("")).isEmpty();
    assertThat(resolver.resolve(null)).isEmpty();
  }

  @Test
  @DisplayName("a bare short word is never a lookup key")
  void shortWordsAreNotKeys() {
    // «πόνος» stems to «πον». Registering that would make every sentence mentioning any pain
    // resolve to whichever page was indexed first - the same trap MedicalTermIndex documents for
    // «τόνος» and the Greek article.
    assertThat(resolver.resolve("έχω έναν πόνο")).isEmpty();
  }

  @Test
  @DisplayName("breast pain does not collide with chest pain")
  void noCollisionBetweenBreastAndChest() {
    // Deliberate in the dataset: every Greek speaker says «πόνος στο στήθος» for chest pain, so
    // breast pain is authored as «μαστοδυνία» instead. Getting this backwards would route a
    // possible cardiac symptom to a breast page.
    assertThat(resolver.resolve("πόνος στο στήθος")).contains("chest-pain");
    assertThat(resolver.resolve("έχω μαστοδυνία")).contains("breast-pain");
  }

  @Test
  @DisplayName("names a question's subject in Greek, for the conversation title")
  void namesTheSubject() {
    // What the history rail is titled with. The slug is English and so is the NHS page, but the
    // interface is Greek-first, so the name has to come from the alias file.
    assertThat(resolver.topicOf("έχω πονοκέφαλο από χθες")).contains("πονοκέφαλος");
    assertThat(resolver.topicOf("πονάει ο λαιμός μου, τι να κάνω;")).contains("πονόλαιμος");
  }

  @Test
  @DisplayName("the name is the lay word, whichever synonym the student happened to type")
  void namesWithTheLayWord() {
    // The dataset authors lay words before clinical ones precisely because students type the
    // former, and the first entry is what becomes the name. A student who typed «κεφαλαλγία»
    // still gets a chat titled «πονοκέφαλος», which is the one they will recognise a week later.
    assertThat(resolver.topicOf("τι προκαλεί την κεφαλαλγία;")).contains("πονοκέφαλος");
  }

  @Test
  @DisplayName("declining to resolve means declining to name")
  void namesNothingWhenNothingResolves() {
    // Most questions land here - sleep, stress, nutrition - and the caller falls back to what the
    // student typed. Inventing a subject would be worse than having none.
    assertThat(resolver.topicOf("πώς διαχειρίζομαι το άγχος των εξετάσεων;")).isEmpty();
    assertThat(resolver.nameFor("not-a-symptom")).isEmpty();
  }

  @Test
  @DisplayName("Greek wins the name even for a symptom the corpus also registers in English")
  void greekBeatsTheEnglishTitle() {
    // registerEnglish runs during ingest, after the alias file is indexed. It must only supply a
    // name for a symptom the file does not cover, never overwrite one it does.
    resolver.registerEnglish("headaches", "Headaches", "Head pain");

    assertThat(resolver.nameFor("headaches")).contains("πονοκέφαλος");
  }

  @Test
  @DisplayName("a symptom the alias file does not cover is named from the corpus instead")
  void fallsBackToTheEnglishTitle() {
    // Better than showing the raw slug, which is what the rail would otherwise display.
    resolver.registerEnglish("a-symptom-with-no-greek", "Some symptom", "");

    assertThat(resolver.nameFor("a-symptom-with-no-greek")).contains("Some symptom");
  }
}
