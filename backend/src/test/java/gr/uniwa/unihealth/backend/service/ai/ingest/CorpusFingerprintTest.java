package gr.uniwa.unihealth.backend.service.ai.ingest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The fingerprint decides whether a boot re-embeds 565 chunks or loads them from disk in
 * milliseconds. Both ways of being wrong are quiet: too sensitive and every restart pays a full
 * re-embed while appearing to work, too blunt and an edited corpus is answered from stale vectors.
 */
class CorpusFingerprintTest {

  private static Document doc(String id, String text) {
    return new Document(id, text, Map.of("k", "v"));
  }

  @Test
  @DisplayName("identical corpora fingerprint identically")
  void stable() {
    List<Document> first = List.of(doc("a#BRIEF", "one"), doc("b#BRIEF", "two"));
    List<Document> second = List.of(doc("a#BRIEF", "one"), doc("b#BRIEF", "two"));

    assertThat(CorpusFingerprint.of(first)).isEqualTo(CorpusFingerprint.of(second));
  }

  @Test
  @DisplayName("order does not matter")
  void orderIndependent() {
    // A repository call with no ORDER BY may return rows in any order. If that changed the
    // fingerprint, every boot would re-embed the whole corpus and the gate would look like it was
    // working.
    List<Document> ascending = List.of(doc("a#BRIEF", "one"), doc("b#BRIEF", "two"));
    List<Document> descending = List.of(doc("b#BRIEF", "two"), doc("a#BRIEF", "one"));

    assertThat(CorpusFingerprint.of(ascending)).isEqualTo(CorpusFingerprint.of(descending));
  }

  @Test
  @DisplayName("edited text changes the fingerprint")
  void detectsEditedText() {
    assertThat(CorpusFingerprint.of(List.of(doc("a#BRIEF", "one"))))
        .isNotEqualTo(CorpusFingerprint.of(List.of(doc("a#BRIEF", "one edited"))));
  }

  @Test
  @DisplayName("a renamed id changes the fingerprint even when the text is identical")
  void detectsRenamedId() {
    // This is what makes a change to the chunking rules - a new section, a different id scheme -
    // invalidate the cache. A checksum of the source file alone would miss it entirely.
    assertThat(CorpusFingerprint.of(List.of(doc("a#BRIEF", "one"))))
        .isNotEqualTo(CorpusFingerprint.of(List.of(doc("a#OVERVIEW", "one"))));
    }

  @Test
  @DisplayName("added and removed documents change the fingerprint")
  void detectsSizeChange() {
    List<Document> one = List.of(doc("a#BRIEF", "one"));
    List<Document> two = List.of(doc("a#BRIEF", "one"), doc("b#BRIEF", "two"));

    assertThat(CorpusFingerprint.of(one)).isNotEqualTo(CorpusFingerprint.of(two));
  }

  @Test
  @DisplayName("an empty corpus fingerprints without blowing up")
  void emptyCorpus() {
    assertThat(CorpusFingerprint.of(List.of())).isNotBlank();
  }
}
