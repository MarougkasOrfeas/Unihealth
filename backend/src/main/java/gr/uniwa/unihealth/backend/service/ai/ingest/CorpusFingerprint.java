package gr.uniwa.unihealth.backend.service.ai.ingest;

import lombok.experimental.UtilityClass;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.ai.document.Document;

import java.util.List;

/**
 * A checksum of the projected corpus, used to decide whether embeddings must be rebuilt.
 *
 * <p>The pattern is borrowed from {@code LabelKeywordInitData}, which checksums
 * {@code medical-terms.json} against {@code t_reference_data_version} so that editing the file and
 * restarting is the whole workflow. The difference here is <em>what</em> is checksummed: there is
 * no single source file, because the assistant items are seeded from Java and the symptom rows come
 * from the database. So the fingerprint is taken over the documents themselves, after chunking.
 *
 * <p>That is also the stronger choice. It covers every input that can change what gets embedded -
 * the snapshot, the seeder, and the chunking rules in {@link SymptomChunker} - so editing a header
 * prefix or adding a section invalidates the cache exactly as an edited source file would. A
 * file-based checksum would miss all three.
 *
 * @author omaro
 */
@UtilityClass
public class CorpusFingerprint {

  /**
   * @param documents the chunked corpus, in any order.
   * @return a hex digest that changes whenever any document's id or text changes, and when
   *         documents are added or removed.
   */
  public String of(List<Document> documents) {
    // Sorted, so the fingerprint describes the corpus rather than the order the rows happened to
    // come back in. Without this a plain repository call with no ORDER BY would produce a different
    // checksum on every boot and re-embed the whole corpus each time - the exact cost this gate
    // exists to avoid, and it would look like it was working.
    String canonical = documents.stream()
        .map(document -> document.getId() + ':' + DigestUtils.md5Hex(text(document)))
        .sorted()
        .reduce(new StringBuilder(), (builder, line) -> builder.append(line).append('\n'),
            StringBuilder::append)
        .toString();

    return DigestUtils.md5Hex(canonical);
  }

  private String text(Document document) {
    return document.getText() == null ? "" : document.getText();
  }
}
