package gr.uniwa.unihealth.backend.service.ai;

import lombok.experimental.UtilityClass;

/**
 * The metadata keys carried on every embedded document.
 *
 * <p>Shared between the chunkers that write them and the retriever that filters and cites on them,
 * so a rename cannot leave one side silently matching nothing.
 *
 * <p><b>Every value stored under these keys is a String.</b> The store is persisted as JSON and
 * read back on the next boot, and an enum written directly returns as a String - so code that put
 * an enum in and expected an enum out would work until the first restart and then stop. Storing
 * strings on both sides removes the asymmetry rather than documenting it.
 *
 * @author omaro
 */
@UtilityClass
public class AiMetadata {

  /** {@code SYMPTOM} or {@code ASSISTANT}. Which corpus a chunk came from. */
  public static final String DOC_TYPE = "docType";

  /** Symptom slug, used to narrow a search once the question's subject is known. */
  public static final String SLUG = "slug";

  /** Human-readable title of the source record, shown in the citation. */
  public static final String TITLE = "title";

  /** Which part of the record this chunk is, e.g. {@code SEE_DOCTOR_IF}. */
  public static final String SECTION = "section";

  /** The section's display name, e.g. "When to see a GP". Shown in the citation. */
  public static final String SECTION_LABEL = "sectionLabel";

  /** Deep link to the publisher's page, so a citation can be followed. */
  public static final String SOURCE_URL = "sourceUrl";

  /** Publisher name, e.g. "NHS website". Required by the licence, not decoration. */
  public static final String SOURCE_NAME = "sourceName";

  /** {@code NONE}, {@code URGENT} or {@code EMERGENCY} - see {@link ingest.Urgency}. */
  public static final String URGENCY = "urgency";

  /**
   * {@code LOCAL_VETTED} or {@code LIVE_WEB}.
   *
   * <p>The most important key here. It is what keeps a citation honest about whether a claim came
   * from content this application curates and licenses, or from a page fetched seconds ago off a
   * public website. Presenting the two as equivalent would make the whole grounding story
   * unfalsifiable, so nothing downstream is allowed to lose this distinction.
   */
  public static final String SOURCE_TYPE = "sourceType";

  /** ISO-8601 date the external item was published. Absent for local content. */
  public static final String PUBLISHED_AT = "publishedAt";

  /** ISO-8601 instant the external page was fetched, so a citation can say how fresh it is. */
  public static final String RETRIEVED_AT = "retrievedAt";

  public static final String DOC_TYPE_SYMPTOM = "SYMPTOM";
  public static final String DOC_TYPE_ASSISTANT = "ASSISTANT";
  public static final String DOC_TYPE_EXTERNAL = "EXTERNAL";

  /** Curated, licensed content this application ships and controls. */
  public static final String SOURCE_TYPE_LOCAL = "LOCAL_VETTED";

  /** Fetched live from a trusted authority during the turn. Treated as untrusted input. */
  public static final String SOURCE_TYPE_LIVE_WEB = "LIVE_WEB";
}
