package gr.uniwa.unihealth.backend.service.ai.external.source;

import gr.uniwa.unihealth.backend.config.properties.AiProperties;
import gr.uniwa.unihealth.backend.service.ai.external.SourceAuthority;
import gr.uniwa.unihealth.backend.service.ai.external.TrustedSourceFetcher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * ECDC - the European Centre for Disease Prevention and Control.
 *
 * <p>The EU-level view: communicable disease threat reports and news that cover Greece without
 * being specific to it. Ranked below EODY for that reason, and above WHO because a European threat
 * assessment is more likely to be locally actionable than a global one.
 *
 * @author omaro
 */
@Component
public class EcdcHealthSource extends FeedBackedHealthSource {

  private final String feedUrl;

  public EcdcHealthSource(TrustedSourceFetcher fetcher, AiProperties aiProperties,
      @Value("${unihealth.ai.external.sources.ecdc-feed:https://www.ecdc.europa.eu/en/taxonomy/term/1522/feed}")
      String feedUrl) {
    super(fetcher, aiProperties);
    this.feedUrl = feedUrl;
  }

  @Override
  protected String feedUrl() {
    return feedUrl;
  }

  @Override
  public String sourceName() {
    return "ECDC";
  }

  @Override
  public String domain() {
    return "ecdc.europa.eu";
  }

  @Override
  public SourceAuthority authority() {
    return SourceAuthority.EUROPEAN;
  }
}
