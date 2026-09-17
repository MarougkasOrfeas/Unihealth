package gr.uniwa.unihealth.backend.service;

/**
 * Emails a round-up of recent health news to the users who still want it.
 */
public interface NewsDigestService {

  /**
   * @return how many digests were sent.
   */
  int sendDigest();
}
