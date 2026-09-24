package gr.uniwa.unihealth.backend.service.ai.external.source;

import gr.uniwa.unihealth.backend.config.properties.AiProperties;
import gr.uniwa.unihealth.backend.service.ai.external.SourceAuthority;
import gr.uniwa.unihealth.backend.service.ai.external.TrustedSourceFetcher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * ΕΟΔΥ - the Greek National Public Health Organisation.
 *
 * <p>The most important of the three by some distance, and the reason this feature is not simply
 * "read the NHS more often". A student asking «υπάρχει έξαρση κρουσμάτων;» is asking about Greece,
 * and neither the UK NHS corpus nor a WHO bulletin about another continent answers that. EODY also
 * publishes in Greek, which is the language the question arrives in.
 *
 * @author omaro
 */
@Component
public class EodyHealthSource extends FeedBackedHealthSource {

  private final String feedUrl;

  public EodyHealthSource(TrustedSourceFetcher fetcher, AiProperties aiProperties,
      @Value("${unihealth.ai.external.sources.eody-feed:https://eody.gov.gr/feed/}")
      String feedUrl) {
    super(fetcher, aiProperties);
    this.feedUrl = feedUrl;
  }

  /**
   * Configurable because a public site may move its feed, and correcting a URL should not need a
   * recompile. This is not a hole in the allowlist: {@link TrustedSourceFetcher} checks every
   * request against {@link #domain()}, which is hardcoded below, so a mistyped or even malicious
   * value here can only ever fail to fetch - never reach somewhere else.
   */
  @Override
  protected String feedUrl() {
    return feedUrl;
  }

  @Override
  public String sourceName() {
    return "ΕΟΔΥ";
  }

  @Override
  public String domain() {
    return "eody.gov.gr";
  }

  @Override
  public SourceAuthority authority() {
    return SourceAuthority.NATIONAL;
  }
}
