package gr.uniwa.unihealth.backend.service.ai.retrieval;

import gr.uniwa.unihealth.backend.dto.ai.CitationDTO;
import gr.uniwa.unihealth.backend.service.ai.AiMetadata;
import gr.uniwa.unihealth.backend.service.ai.ingest.Urgency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This is the exact text handed to the model, so it is worth pinning. A silently malformed context
 * does not throw; it produces an answer that ignores the library and looks entirely normal.
 */
class GroundedContextTest {

  private static Document passage(String title, String section, Urgency urgency, String text) {
    Map<String, Object> metadata = new HashMap<>();
    metadata.put(AiMetadata.TITLE, title);
    metadata.put(AiMetadata.SECTION_LABEL, section);
    metadata.put(AiMetadata.URGENCY, urgency.name());
    metadata.put(AiMetadata.SOURCE_NAME, "NHS website");
    metadata.put(AiMetadata.SOURCE_URL, "https://www.nhs.uk/symptoms/sore-throat/");
    return new Document("sore-throat#X", text, metadata);
  }

  @Test
  @DisplayName("passages are numbered and labelled so the model can name what it used")
  void numbersAndLabelsPassages() {
    String context = GroundedContext.render(
        List.of(passage("Sore throat", "What you can do", Urgency.NONE, "Drink plenty of water.")),
        List.of());

    assertThat(context)
        .contains("[1] Sore throat - What you can do")
        .contains("Drink plenty of water.");
  }

  @Test
  @DisplayName("an urgent passage is marked, because it is present by rule rather than by score")
  void marksUrgentPassages() {
    // Without the marker a model ranking by apparent relevance buries the "call 999" passage under
    // the reassuring ones - which is exactly the case it was force-included for.
    String context = GroundedContext.render(
        List.of(passage("Chest pain", "Call 999 or go to A&E", Urgency.EMERGENCY, "Call 999.")),
        List.of());

    assertThat(context).contains("[URGENT - mention this before anything else]");
  }

  @Test
  @DisplayName("profile labels are appended as ambient context")
  void appendsProfileLabels() {
    String context = GroundedContext.render(List.of(), List.of("ALLERGY_PEANUT", "CHRONIC_ASTHMA"));

    assertThat(context).contains("ALLERGY_PEANUT, CHRONIC_ASTHMA");
  }

  @Test
  @DisplayName("nothing to add renders nothing, rather than an empty heading")
  void emptyRendersEmpty() {
    assertThat(GroundedContext.render(List.of(), List.of())).isEmpty();
  }

  @Test
  @DisplayName("citations come from metadata and carry the publisher and link")
  void citationsFromMetadata() {
    List<CitationDTO> citations = GroundedContext.citations(
        List.of(passage("Sore throat", "What you can do", Urgency.NONE, "text")));

    assertThat(citations).singleElement().satisfies(citation -> {
      assertThat(citation.title()).isEqualTo("Sore throat");
      assertThat(citation.sectionLabel()).isEqualTo("What you can do");
      assertThat(citation.sourceName()).isEqualTo("NHS website");
      assertThat(citation.sourceUrl()).isEqualTo("https://www.nhs.uk/symptoms/sore-throat/");
      assertThat(citation.urgent()).isFalse();
    });
  }

  @Test
  @DisplayName("two passages from one section cite once")
  void citationsAreDistinct() {
    Document first = passage("Sore throat", "What you can do", Urgency.NONE, "one");
    Document second = passage("Sore throat", "What you can do", Urgency.NONE, "two");

    assertThat(GroundedContext.citations(List.of(first, second))).hasSize(1);
  }

  @Test
  @DisplayName("missing metadata degrades to blanks rather than throwing")
  void toleratesMissingMetadata() {
    Document bare = new Document("id", "text", Map.of());

    assertThat(GroundedContext.render(List.of(bare), List.of())).contains("[1]");
    assertThat(GroundedContext.citations(List.of(bare))).singleElement()
        .satisfies(citation -> assertThat(citation.urgent()).isFalse());
  }
}
