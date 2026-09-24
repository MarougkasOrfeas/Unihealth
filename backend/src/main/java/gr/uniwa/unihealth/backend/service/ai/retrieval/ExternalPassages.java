package gr.uniwa.unihealth.backend.service.ai.retrieval;

import gr.uniwa.unihealth.backend.service.ai.AiMetadata;
import gr.uniwa.unihealth.backend.service.ai.external.ExternalCandidate;
import gr.uniwa.unihealth.backend.service.ai.ingest.Urgency;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Turns a live candidate into the same {@link Document} shape the vetted corpus uses.
 *
 * <p>One shape downstream means {@code GroundedContext} renders both kinds of passage, and the
 * citation path works for both, without a second code path that could drift. The distinction that
 * must survive is carried in metadata rather than in type: {@link AiMetadata#SOURCE_TYPE}.
 *
 * <p>These documents are built for a single turn and thrown away. They are never added to the
 * vector store - that store is for content whose provenance the application controls, and mixing
 * live pages into it would make the local/external distinction unrecoverable.
 *
 * @author omaro
 */
@UtilityClass
public class ExternalPassages {

  public Document toDocument(ExternalCandidate candidate) {
    Map<String, Object> metadata = new HashMap<>();

    metadata.put(AiMetadata.DOC_TYPE, AiMetadata.DOC_TYPE_EXTERNAL);
    metadata.put(AiMetadata.SOURCE_TYPE, AiMetadata.SOURCE_TYPE_LIVE_WEB);
    metadata.put(AiMetadata.TITLE, StringUtils.defaultString(candidate.title()));
    metadata.put(AiMetadata.SOURCE_NAME, StringUtils.defaultString(candidate.sourceName()));
    metadata.put(AiMetadata.SOURCE_URL, StringUtils.defaultString(candidate.url()));

    // Empty rather than absent, so the section label renders predictably for both kinds of
    // passage. An external item has no section - it is a whole article.
    metadata.put(AiMetadata.SECTION, "");
    metadata.put(AiMetadata.SECTION_LABEL, "");

    // Never urgent. The urgency rule exists to stop the NHS "call 999" sections being ranked away,
    // and a news item must not be able to borrow that emphasis.
    metadata.put(AiMetadata.URGENCY, Urgency.NONE.name());

    metadata.put(AiMetadata.PUBLISHED_AT, candidate.publishedAt() == null ? ""
        : candidate.publishedAt().format(DateTimeFormatter.ISO_LOCAL_DATE));
    metadata.put(AiMetadata.RETRIEVED_AT, candidate.retrievedAt() == null ? ""
        : candidate.retrievedAt().toString());

    // Keyed by URL so the same item arriving from two syndicating sources collapses to one
    // passage, exactly as document ids do for the local corpus.
    return new Document("external#" + candidate.url(), candidate.text(), metadata);
  }
}
