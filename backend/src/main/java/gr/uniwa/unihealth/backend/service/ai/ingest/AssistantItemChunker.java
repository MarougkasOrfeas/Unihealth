package gr.uniwa.unihealth.backend.service.ai.ingest;

import gr.uniwa.unihealth.backend.model.UnihealthAssistantItem;
import gr.uniwa.unihealth.backend.service.ai.AiMetadata;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Turns one curated assistant item - an FAQ entry, a campus service, an emergency contact - into a
 * single document.
 *
 * <p>One chunk per item, with no splitting: these are already short, already written by hand, and
 * already scoped to one question. They are also the only Greek text in the corpus, which is why
 * they matter out of proportion to their number. A student asking «πού είναι το ιατρείο;» is
 * answered from here, not from the NHS pages.
 *
 * <p>Unlike the symptom pages these are tenant-owned content, which is the reason the vector store
 * is partitioned per tenant rather than shared.
 *
 * @author omaro
 */
@UtilityClass
public class AssistantItemChunker {

  /**
   * @return the document for this item, or empty when it has no body worth embedding.
   */
  public Optional<Document> chunk(UnihealthAssistantItem item) {
    String body = StringUtils.trimToNull(item.getContent());
    String brief = StringUtils.trimToNull(item.getBrief());

    if (body == null && brief == null) {
      return Optional.empty();
    }

    return Optional.of(new Document(idOf(item), embeddableText(item, brief, body),
        metadataOf(item)));
  }

  /**
   * Keyed by the row id rather than by a slug, because these rows have no stable natural key and
   * are seeded per tenant. Re-seeding a tenant therefore produces new ids, which the corpus
   * fingerprint correctly reads as a changed corpus.
   */
  public String idOf(UnihealthAssistantItem item) {
    return "assistant#" + item.getId();
  }

  /**
   * Title first, then the one-line brief, then the body. The title carries the topic that the body
   * often assumes - an emergency contact's content is a phone number, which on its own is
   * unretrievable by any question a student would think to ask.
   */
  public String embeddableText(UnihealthAssistantItem item, String brief, String body) {
    StringBuilder text = new StringBuilder(StringUtils.defaultString(item.getTitle()));

    if (brief != null) {
      text.append("\n\n").append(brief);
    }
    if (body != null) {
      text.append("\n\n").append(body);
    }

    return text.toString().trim();
  }

  private Map<String, Object> metadataOf(UnihealthAssistantItem item) {
    Map<String, Object> metadata = new HashMap<>();

    metadata.put(AiMetadata.DOC_TYPE, AiMetadata.DOC_TYPE_ASSISTANT);
    metadata.put(AiMetadata.TITLE, StringUtils.defaultString(item.getTitle()));
    metadata.put(AiMetadata.SECTION, item.getSection() != null ? item.getSection().name() : "");
    metadata.put(AiMetadata.SECTION_LABEL,
        item.getSection() != null ? item.getSection().name() : "");
    metadata.put(AiMetadata.SOURCE_NAME, "UniHealth");
    metadata.put(AiMetadata.SOURCE_URL, "");

    // The EMERGENCY section is the curated 112/166/EKAB content. Tagging it the same way the NHS
    // emergency sections are tagged means one rule in the retriever covers both, rather than the
    // university's own emergency card being the thing that gets ranked away.
    metadata.put(AiMetadata.URGENCY, isEmergency(item) ? Urgency.EMERGENCY.name()
        : Urgency.NONE.name());

    return metadata;
  }

  private boolean isEmergency(UnihealthAssistantItem item) {
    return item.getSection() != null && "EMERGENCY".equals(item.getSection().name());
  }
}
