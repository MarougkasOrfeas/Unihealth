package gr.uniwa.unihealth.backend.service.ai.external;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * The host check is the allowlist. Everything else in this feature assumes it holds.
 *
 * <p>These cases run without a network: they exercise the decision, not the transport. The
 * transport-level guarantees - redirects refused, oversized bodies refused, non-markup refused -
 * are asserted by reading {@link TrustedSourceFetcher}, and the one that can be tested purely is
 * the one most likely to be got wrong by a plausible-looking one-line change.
 */
class TrustedSourceFetcherTest {

  /** Never reached: every case below is refused before the cache is consulted. */
  private final TrustedSourceFetcher fetcher = new TrustedSourceFetcher(
      "UniHealthBot/1.0 (+https://www.uniwa.gr; test)", mock(StringRedisTemplate.class));

  private static final Duration TTL = Duration.ofMinutes(1);

  @Test
  @DisplayName("the exact domain is allowed")
  void exactDomain() {
    assertThat(fetcher.isOnDomain(URI.create("https://who.int/news/item/1"), "who.int")).isTrue();
  }

  @Test
  @DisplayName("a subdomain is allowed")
  void subdomain() {
    assertThat(fetcher.isOnDomain(URI.create("https://www.who.int/rss"), "who.int")).isTrue();
    assertThat(fetcher.isOnDomain(URI.create("https://a.b.eody.gov.gr/x"), "eody.gov.gr")).isTrue();
  }

  @Test
  @DisplayName("a domain that merely ends with the allowed one is refused")
  void lookalikeDomainRefused() {
    // The dot in the suffix check is what makes this fail. Without it "notwho.int" ends with
    // "who.int" and an attacker only has to register one domain to be inside the allowlist.
    assertThat(fetcher.isOnDomain(URI.create("https://notwho.int/x"), "who.int")).isFalse();
    assertThat(fetcher.isOnDomain(URI.create("https://evilwho.int/x"), "who.int")).isFalse();
  }

  @Test
  @DisplayName("an unrelated domain is refused")
  void unrelatedDomainRefused() {
    assertThat(fetcher.isOnDomain(URI.create("https://example.com/x"), "who.int")).isFalse();
  }

  @Test
  @DisplayName("the allowed domain appearing elsewhere in the URL does not help")
  void domainInPathOrUserInfoRefused() {
    // Classic bypasses: the real host is example.com in all three.
    assertThat(fetcher.isOnDomain(URI.create("https://example.com/who.int"), "who.int")).isFalse();
    assertThat(fetcher.isOnDomain(URI.create("https://example.com?x=who.int"), "who.int"))
        .isFalse();
    assertThat(fetcher.isOnDomain(URI.create("https://who.int@example.com/"), "who.int")).isFalse();
  }

  @Test
  @DisplayName("plain http is refused even on the right domain")
  void httpRefused() {
    // These are public health authorities in 2026; there is no reason to read them in clear, and
    // allowing it would make the content tamperable in transit by anyone on the path.
    assertThat(fetcher.isOnDomain(URI.create("http://who.int/news"), "who.int")).isFalse();
  }

  @Test
  @DisplayName("a URL with no host is refused rather than throwing")
  void hostlessUrlRefused() {
    assertThat(fetcher.isOnDomain(URI.create("file:///etc/passwd"), "who.int")).isFalse();
    assertThat(fetcher.isOnDomain(URI.create("/relative/path"), "who.int")).isFalse();
  }

  @Test
  @DisplayName("host comparison ignores case")
  void caseInsensitive() {
    assertThat(fetcher.isOnDomain(URI.create("https://WWW.WHO.INT/x"), "who.int")).isTrue();
  }

  @Test
  @DisplayName("a null or empty expected domain refuses everything")
  void missingExpectedDomainRefuses() {
    // TrustedWebRetrieverImpl passes an empty domain when no adapter claims a candidate. That has
    // to fail closed, or an unclaimed candidate would be fetchable from anywhere.
    assertThat(fetcher.isOnDomain(URI.create("https://who.int/x"), "")).isFalse();
    assertThat(fetcher.isOnDomain(URI.create("https://who.int/x"), null)).isFalse();
  }

  @Test
  @DisplayName("a malformed URL is refused without throwing")
  void malformedUrlRefused() {
    assertThat(fetcher.fetch("http://  spaces  ", "who.int", TTL)).isEmpty();
  }

  @Test
  @DisplayName("an off-domain URL is refused before any request is attempted")
  void offDomainNeverFetched() {
    // No network is involved: the host check rejects it first. If this ever started making a
    // request the test would hang rather than fail, which is itself the signal.
    assertThat(fetcher.fetch("https://example.com/anything", "who.int", TTL)).isEmpty();
  }
}
