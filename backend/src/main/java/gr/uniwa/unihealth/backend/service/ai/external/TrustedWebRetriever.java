package gr.uniwa.unihealth.backend.service.ai.external;

import gr.uniwa.unihealth.backend.service.ai.AiTurnProgress;

import java.util.List;

/**
 * Consults the trusted health authorities for information the vetted corpus cannot hold.
 *
 * <p>An interface with one implementation, because it is the seam a different discovery mechanism
 * would slot into. The adapters answer "what has this authority published lately" well and cost
 * nothing; a site-restricted search API would answer arbitrary questions better and cost a key and
 * a quota. Should the former prove too narrow, the latter becomes a second implementation and
 * nothing upstream changes — the citation contract and the injection containment are properties of
 * the caller, not of how the pages were found.
 *
 * @author omaro
 */
public interface TrustedWebRetriever {

  /**
   * @param question what the student asked, used to rank candidates.
   * @param progress where to report that pages are being read, since that is the slow part.
   * @return the best few recent items, with page bodies where they could be fetched in time.
   *         Empty whenever nothing qualified, nothing was reachable, or the budget ran out —
   *         never an exception, because a slow government website must not fail a health question.
   */
  List<ExternalCandidate> retrieve(String question, AiTurnProgress progress);
}
