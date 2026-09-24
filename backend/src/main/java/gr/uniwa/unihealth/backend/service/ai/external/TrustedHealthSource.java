package gr.uniwa.unihealth.backend.service.ai.external;

import java.time.LocalDate;
import java.util.List;

/**
 * One health authority the assistant is allowed to consult, and the only thing that knows how to
 * read that authority's index.
 *
 * <p>The allowlist is the set of implementations of this interface. That is a deliberate choice
 * over a configured list of domains: a domain string in a properties file can be edited to point
 * anywhere, whereas an adapter is structurally incapable of reaching a site other than its own -
 * it hardcodes its index URL, declares its domain, and {@link TrustedSourceFetcher} re-checks
 * every request against that declaration. Adding an authority means writing a class, which is a
 * decision someone has to make on purpose.
 *
 * <p><b>Implementations must fail soft.</b> These are public websites that will be restructured
 * without warning. An adapter that can no longer parse its index must return an empty list and log
 * it; it must never throw, because the student asked a health question and the answer to it does
 * not depend on this source being reachable.
 *
 * @author omaro
 */
public interface TrustedHealthSource {

  /** Display name used in citations, e.g. "ECDC". */
  String sourceName();

  /**
   * The registrable domain this source owns, e.g. {@code who.int}. Every URL this adapter produces
   * is checked against it before being fetched.
   */
  String domain();

  SourceAuthority authority();

  /**
   * Lists recent items from this authority's own index.
   *
   * <p>Discovery only: the returned candidates carry titles, dates and snippets, not page bodies.
   * Fetching a body costs a second request and is done by the retriever for the few candidates
   * that survive ranking.
   *
   * @param notBefore items published before this date are not worth returning. An item whose date
   *                  cannot be parsed must be dropped, never guessed - "is there anything recent"
   *                  cannot be answered by an item of unknown age.
   * @return what was found, most recent first. Empty on any failure.
   */
  List<ExternalCandidate> recent(LocalDate notBefore);
}
