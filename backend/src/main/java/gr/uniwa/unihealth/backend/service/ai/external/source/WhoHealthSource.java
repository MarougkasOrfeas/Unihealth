package gr.uniwa.unihealth.backend.service.ai.external.source;

import gr.uniwa.unihealth.backend.config.properties.AiProperties;
import gr.uniwa.unihealth.backend.service.ai.external.SourceAuthority;
import gr.uniwa.unihealth.backend.service.ai.external.TrustedSourceFetcher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * WHO - global outbreak and health news.
 *
 * <p>The backstop. Authoritative and well dated, but the least likely of the three to tell a
 * student in Athens something they can act on, which is why it sits last in
 * {@link SourceAuthority}. Kept because an outbreak reaches WHO before it reaches anyone else.
 *
 * @author omaro
 */
@Component
public class WhoHealthSource extends FeedBackedHealthSource {

  private final String feedUrl;

  public WhoHealthSource(TrustedSourceFetcher fetcher, AiProperties aiProperties,
      @Value("${unihealth.ai.external.sources.who-feed:https://www.who.int/rss-feeds/news-english.xml}")
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
    return "WHO";
  }

  @Override
  public String domain() {
    return "who.int";
  }

  @Override
  public SourceAuthority authority() {
    return SourceAuthority.INTERNATIONAL;
  }
}
