package gr.uniwa.unihealth.data.nhs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Parsing tests against real pages saved from nhs.uk.
 *
 * <p>The markup is the one part of this feature that can change without warning, so the fixtures
 * are whole pages rather than trimmed snippets: if the NHS restructures its causes table or renames
 * a health aspect, these fail rather than the corpus quietly going empty on the next ingestion.
 *
 * @author omaro
 */
class NhsSymptomParserTest {

  private NhsSymptomParser parser;

  @BeforeEach
  void setUp() {
    parser = new NhsSymptomParser(new ObjectMapper());
  }

  @Test
  @DisplayName("reads the article sections from the schema.org JSON-LD")
  void readsSections() throws IOException {
    NhsSnapshot.Symptom symptom = parser.parse(fixture("chest-pain"), "chest-pain");

    assertThat(symptom).isNotNull();
    assertThat(symptom.title()).isEqualTo("Chest pain");
    assertThat(symptom.synonyms()).isEqualTo("Heart pain");
    assertThat(symptom.sourceUrl()).isEqualTo("https://www.nhs.uk/symptoms/chest-pain/");

    // The overview aspect leads with the in-page navigation; those bare labels must not survive.
    assertThat(symptom.overviewText())
        .startsWith("Common causes of chest pain")
        .doesNotContain("\nCauses\n");
    assertThat(symptom.overviewText().lines().findFirst()).get()
        .isNotEqualTo("Causes");
  }

  @Test
  @DisplayName("separates the three triage levels the NHS publishes")
  void separatesTriageLevels() throws IOException {
    NhsSnapshot.Symptom symptom = parser.parse(fixture("sore-throat"), "sore-throat");

    assertThat(symptom.emergencyText()).startsWith("Call 999 or go to A&E now if:");
    assertThat(symptom.urgentText()).startsWith("Ask for an urgent GP appointment");
    assertThat(symptom.seeDoctorIfText()).contains("See a GP if:");

    // Bullets have to come through as "- " lines, because that is what the detail page renders.
    assertThat(symptom.symptomsText()).contains("\n- a dry, scratchy throat");
  }

  @Test
  @DisplayName("turns the causes table into factors and linked conditions")
  void readsCausesTable() throws IOException {
    NhsSnapshot.Symptom symptom = parser.parse(fixture("chest-pain"), "chest-pain");

    assertThat(symptom.causes()).hasSize(5);

    NhsSnapshot.Cause heartburn = symptom.causes().getFirst();
    assertThat(heartburn.causeText()).isEqualTo("Heartburn or indigestion");
    assertThat(heartburn.factors()).containsExactly(
        "Starts after eating",
        "bringing up food or bitter tasting fluids",
        "feeling full and bloated");
    assertThat(heartburn.conditions())
        .extracting(NhsSnapshot.ConditionRef::name)
        .containsExactly("Heartburn", "indigestion");
    assertThat(heartburn.conditions().getFirst().url())
        .isEqualTo("https://www.nhs.uk/conditions/heartburn-and-acid-reflux/");
  }

  @Test
  @DisplayName("keeps a leading adjective attached to the phrase it belongs to")
  void doesNotSplitInsideAPhrase() {
    // "An often sharp" on its own is not something a reader could tick.
    assertThat(parser.splitFactors(
        "An often sharp, continuous pain triggered by worries, sweating, dizziness"))
        .containsExactly(
            "An often sharp, continuous pain triggered by worries",
            "sweating",
            "dizziness");

    assertThat(parser.splitFactors("Sore, blurry or watery eyes"))
        .containsExactly("Sore, blurry or watery eyes");

    // Repeated adjectives fold in one after another.
    assertThat(parser.splitFactors("Itchy, flaky, sticky or swollen eyelid"))
        .containsExactly("Itchy, flaky, sticky or swollen eyelid");
  }

  @Test
  @DisplayName("does not split inside brackets")
  void doesNotSplitInsideBrackets() {
    assertThat(parser.splitFactors(
        "Swelling around a joint and a high temperature (or you feel hot, cold or shivery)"))
        .containsExactly(
            "Swelling around a joint and a high temperature (or you feel hot, cold or shivery)");
  }

  @Test
  @DisplayName("ignores tables that describe treatments rather than causes")
  void ignoresTreatmentTables() throws IOException {
    // The sore throat page carries no causes table, only prose and self-care content.
    NhsSnapshot.Symptom symptom = parser.parse(fixture("sore-throat"), "sore-throat");

    assertThat(symptom.causes()).isEmpty();
  }

  @Test
  @DisplayName("flattens NHS markup into the bullet format the detail page renders")
  void flattensMarkup() {
    String lines = parser.htmlToLines("<p>Intro line.</p><ul><li>first</li><li>second</li></ul>");

    assertThat(lines.lines().toList())
        .containsExactly("Intro line.", "- first", "- second");
  }

  @Test
  @DisplayName("skips a page with no usable JSON-LD rather than half-ingesting it")
  void skipsPagesWithoutJsonLd() {
    assertThat(parser.parse("<html><body><p>nothing here</p></body></html>", "mystery")).isNull();
  }

  private String fixture(String slug) throws IOException {
    try (var in = getClass().getClassLoader().getResourceAsStream("nhs/" + slug + ".html")) {
      if (in == null) {
        throw new IOException("Missing fixture: " + slug);
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @Test
  @DisplayName("drops clauses too short to mean anything")
  void dropsEmptyClauses() {
    List<String> factors = parser.splitFactors("Pain, , a, tenderness");

    assertThat(factors).containsExactly("Pain", "tenderness");
  }
}
