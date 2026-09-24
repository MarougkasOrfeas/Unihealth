package gr.uniwa.unihealth.backend.service.ai.external;

/**
 * How much weight a trusted external source carries relative to the others.
 *
 * <p>Used to break ties between candidates of equal relevance, and only for that. Every source
 * reachable at all is already on the allowlist, so this is a preference order, not a trust
 * boundary - the trust boundary is which adapters exist.
 *
 * <p>Declared most preferred first.
 *
 * @author omaro
 */
public enum SourceAuthority {

  /**
   * The national authority for the country the students are in.
   *
   * <p>Ranked above the international bodies on purpose. A question about «κρούσματα» is almost
   * always a question about Greece, and a WHO bulletin about another continent is a worse answer
   * than an EODY notice even when it scores better on cosine similarity.
   */
  NATIONAL,

  /** EU-level bodies - ECDC. Relevant to Greece, but not specific to it. */
  EUROPEAN,

  /** Global bodies - WHO. Authoritative, least likely to be locally actionable. */
  INTERNATIONAL
}
