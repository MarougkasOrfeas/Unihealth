package gr.uniwa.unihealth.backend.service.ai.ingest;

import gr.uniwa.unihealth.backend.model.DataSource;
import gr.uniwa.unihealth.backend.model.SymptomItem;
import gr.uniwa.unihealth.backend.service.ai.AiMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Chunking mistakes do not throw. They retrieve badly, months later, in a language nobody on the
 * team reads. So the contract is pinned here instead: what becomes a chunk, what the embedded text
 * looks like, and what metadata a citation can rely on.
 */
class SymptomChunkerTest {

  private static SymptomItem soreThroat() {
    DataSource nhs = new DataSource();
    nhs.setName("NHS website");

    SymptomItem symptom = new SymptomItem();
    symptom.setTitle("Sore throat");
    symptom.setSlug("sore-throat");
    symptom.setSynonyms("throat pain, pharyngitis");
    symptom.setSourceUrl("https://www.nhs.uk/symptoms/sore-throat/");
    symptom.setSource(nhs);
    symptom.setBrief("Sore throats are very common and usually nothing to worry about.");
    symptom.setDoText("rest and drink plenty of water, suck ice cubes, gargle with warm salty "
        + "water, avoid smoking or smoky places");
    return symptom;
  }

  @Test
  @DisplayName("one chunk per populated section, and nothing for the empty ones")
  void onlyPopulatedSections() {
    List<Document> chunks = SymptomChunker.chunk(soreThroat());

    assertThat(chunks).hasSize(2);
    assertThat(chunks).extracting(d -> d.getMetadata().get(AiMetadata.SECTION))
        .containsExactly("BRIEF", "DO");
  }

  @Test
  @DisplayName("the embedded text names the symptom, so a passage is reachable by its topic")
  void headerPrefixCarriesTheTopic() {
    Document doText = SymptomChunker.chunk(soreThroat()).get(1);

    // Without the header this chunk is "rest and drink plenty of water..." and never says throat,
    // which makes it unreachable by the one question it exists to answer.
    assertThat(doText.getText())
        .startsWith("Sore throat (throat pain, pharyngitis) - What you can do")
        .contains("gargle with warm salty water");
  }

  @Test
  @DisplayName("ids are derived from the slug, so re-ingestion does not churn the corpus")
  void stableIds() {
    assertThat(SymptomChunker.chunk(soreThroat()))
        .extracting(Document::getId)
        .containsExactly("sore-throat#BRIEF", "sore-throat#DO");
  }

  @Test
  @DisplayName("citation metadata is populated for every chunk")
  void citationMetadata() {
    for (Document chunk : SymptomChunker.chunk(soreThroat())) {
      assertThat(chunk.getMetadata())
          .containsEntry(AiMetadata.DOC_TYPE, AiMetadata.DOC_TYPE_SYMPTOM)
          .containsEntry(AiMetadata.SLUG, "sore-throat")
          .containsEntry(AiMetadata.TITLE, "Sore throat")
          .containsEntry(AiMetadata.SOURCE_NAME, "NHS website")
          .containsEntry(AiMetadata.SOURCE_URL, "https://www.nhs.uk/symptoms/sore-throat/");
    }
  }

  @Test
  @DisplayName("metadata values are all strings, so a save/load round trip cannot change types")
  void metadataIsAllStrings() {
    // The store is persisted as JSON. An enum written here would read back as a String on the next
    // boot, so anything comparing it to an enum would work once and then quietly stop.
    for (Document chunk : SymptomChunker.chunk(soreThroat())) {
      assertThat(chunk.getMetadata().values()).allMatch(String.class::isInstance);
    }
  }

  @Test
  @DisplayName("a short urgency section survives while a short ordinary one is dropped")
  void urgencySectionsAreNeverTooShort() {
    SymptomItem symptom = soreThroat();
    symptom.setBrief(null);
    symptom.setDoText(null);
    symptom.setSymptomsText("Short.");
    symptom.setEmergencyText("Call 999 now.");

    List<Document> chunks = SymptomChunker.chunk(symptom);

    // "Short." is below the floor and goes; the emergency line is below it too and stays, because
    // the retriever is required to surface it regardless of score.
    assertThat(chunks).hasSize(1);
    assertThat(chunks.getFirst().getMetadata())
        .containsEntry(AiMetadata.SECTION, "EMERGENCY")
        .containsEntry(AiMetadata.URGENCY, Urgency.EMERGENCY.name());
  }

  @Test
  @DisplayName("a symptom with no text at all produces nothing rather than an empty chunk")
  void emptySymptom() {
    SymptomItem bare = new SymptomItem();
    bare.setTitle("Nothing");
    bare.setSlug("nothing");

    assertThat(SymptomChunker.chunk(bare)).isEmpty();
  }

  @Test
  @DisplayName("a symptom with no synonyms still gets a clean header")
  void headerWithoutSynonyms() {
    SymptomItem symptom = soreThroat();
    symptom.setSynonyms(null);

    assertThat(SymptomChunker.chunk(symptom).getFirst().getText())
        .startsWith("Sore throat - Summary");
  }
}
