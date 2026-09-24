package gr.uniwa.unihealth.backend.service.ai.ingest;

import gr.uniwa.unihealth.backend.model.DataSource;
import gr.uniwa.unihealth.backend.model.SymptomItem;
import gr.uniwa.unihealth.backend.service.ai.AiMetadata;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns a symptom record into one embeddable document per populated section.
 *
 * <p>Deliberately pure and static: no Spring bean, no repository, no embedding model. That is what
 * lets the whole chunking contract - ids, header prefixes, metadata, which sections are skipped -
 * be pinned by an ordinary unit test before Ollama is involved at all, which matters because a
 * chunking mistake is invisible at runtime. It does not throw; it just retrieves badly.
 *
 * @author omaro
 */
@UtilityClass
public class SymptomChunker {

  /**
   * Below this many characters a section is not worth embedding on its own: a stub like "See a GP."
   * carries no retrievable meaning and only dilutes the neighbourhood around a real passage.
   */
  private static final int MIN_SECTION_CHARS = 40;

  /**
   * @return one document per non-blank section, in declaration order. Empty when the symptom has
   *         no usable text at all.
   */
  public List<Document> chunk(SymptomItem symptom) {
    List<Document> documents = new ArrayList<>();

    for (SymptomSection section : SymptomSection.values()) {
      String body = StringUtils.trimToNull(section.textOf(symptom));

      // Urgency sections are kept whatever their length. They are short by nature - "call 999 if
      // you have chest pain" is the whole message - and the retriever is required to surface them
      // regardless of score, so dropping one here would silently defeat that rule.
      if (body == null || (body.length() < MIN_SECTION_CHARS
          && section.urgency() == Urgency.NONE)) {
        continue;
      }

      documents.add(new Document(idOf(symptom, section), embeddableText(symptom, section, body),
          metadataOf(symptom, section)));
    }

    return documents;
  }

  /**
   * Stable across re-ingestion, because it is derived from the slug rather than from a row id. That
   * is what makes the corpus fingerprint meaningful: a document whose text has not changed keeps
   * the same identity, so a re-embed can be skipped honestly rather than hopefully.
   */
  public String idOf(SymptomItem symptom, SymptomSection section) {
    return symptom.getSlug() + "#" + section.name();
  }

  /**
   * What actually gets embedded: the body under a header naming the symptom and the section.
   *
   * <p>The header is the highest-value few lines in the retrieval path. Without it the "What you
   * can do" chunk for a sore throat reads "rest, drink plenty of water, suck ice cubes" and never
   * mentions throats - so it is unreachable by any query about a sore throat, which is precisely
   * the query it should answer. Synonyms ride along for the same reason: they are how "heart pain"
   * reaches the chest pain page.
   */
  public String embeddableText(SymptomItem symptom, SymptomSection section, String body) {
    StringBuilder header = new StringBuilder(symptom.getTitle());

    String synonyms = StringUtils.trimToNull(symptom.getSynonyms());
    if (synonyms != null) {
      header.append(" (").append(synonyms).append(')');
    }

    return header.append(" - ").append(section.label()).append("\n\n").append(body).toString();
  }

  private Map<String, Object> metadataOf(SymptomItem symptom, SymptomSection section) {
    Map<String, Object> metadata = new HashMap<>();

    metadata.put(AiMetadata.DOC_TYPE, AiMetadata.DOC_TYPE_SYMPTOM);
    metadata.put(AiMetadata.SLUG, symptom.getSlug());
    metadata.put(AiMetadata.TITLE, symptom.getTitle());
    metadata.put(AiMetadata.SECTION, section.name());
    metadata.put(AiMetadata.SECTION_LABEL, section.label());
    metadata.put(AiMetadata.URGENCY, section.urgency().name());

    // Attribution is a licence condition of the NHS content, so the citation has to be able to
    // name the publisher and link back. Both are read from the record rather than hardcoded,
    // because t_data_source is what the ingest populated and what a second source would populate
    // differently.
    metadata.put(AiMetadata.SOURCE_URL, StringUtils.defaultString(symptom.getSourceUrl()));

    DataSource source = symptom.getSource();
    metadata.put(AiMetadata.SOURCE_NAME,
        source != null ? StringUtils.defaultString(source.getName()) : "");

    return metadata;
  }
}
