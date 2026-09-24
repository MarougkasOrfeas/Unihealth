package gr.uniwa.unihealth.backend.service.ai.external;

import java.time.Instant;
import java.time.LocalDate;

/**
 * One item found on a trusted authority's index, before it is ranked or its page is read.
 *
 * <p>Discovery is deliberately separated from fetching: an index listing yields perhaps twenty of
 * these cheaply, and only the best one or two ever have their full page downloaded. That is what
 * keeps a live lookup inside a three-second budget.
 *
 * @param title       the item's headline, as published.
 * @param url         absolute, and on the owning adapter's own domain - re-checked before fetching.
 * @param publishedAt publication date. Mandatory: an item whose date cannot be parsed is dropped
 *                    rather than guessed, because the entire reason this feature exists is to
 *                    answer "is there anything recent", and an undated item cannot.
 * @param snippet     summary or lead paragraph from the index. May be blank; ranking falls back to
 *                    the title.
 * @param sourceName  display name for the citation, e.g. "ECDC".
 * @param authority   used to break ties between equally relevant candidates.
 * @param body        full page text, populated only after the candidate survives ranking. Null
 *                    until then.
 * @param retrievedAt when this was fetched, so a citation can say how fresh the lookup itself was.
 *
 * @author omaro
 */
public record ExternalCandidate(String title, String url, LocalDate publishedAt, String snippet,
                                String sourceName, SourceAuthority authority, String body,
                                Instant retrievedAt) {

  /** What an adapter returns from an index listing: everything except the page body. */
  public static ExternalCandidate discovered(String title, String url, LocalDate publishedAt,
      String snippet, String sourceName, SourceAuthority authority) {
    return new ExternalCandidate(title, url, publishedAt, snippet, sourceName, authority, null,
        Instant.now());
  }

  /** The same candidate once its page has been read. */
  public ExternalCandidate withBody(String pageBody) {
    return new ExternalCandidate(title, url, publishedAt, snippet, sourceName, authority, pageBody,
        Instant.now());
  }

  /**
   * @return the text to embed for ranking, and to show the model when no page body was fetched.
   */
  public String text() {
    if (body != null && !body.isBlank()) {
      return body;
    }
    return snippet == null || snippet.isBlank() ? title : title + "\n\n" + snippet;
  }
}
