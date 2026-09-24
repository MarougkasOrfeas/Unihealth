package gr.uniwa.unihealth.backend.service.ai.external;

import crawlercommons.robots.BaseRobotRules;
import crawlercommons.robots.SimpleRobotRulesParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The only way this application reaches a trusted health authority, and the only place the domain
 * allowlist is enforced.
 *
 * <p>Descended from {@code PoliteHttpFetcher} in {@code data/nhs-ingest}, whose javadoc asks for
 * exactly this consolidation. What is kept from it: robots.txt honoured via crawler-commons and
 * cached per host for a day, failing <em>closed</em> when robots.txt cannot be read; a
 * self-identifying User-Agent so an administrator who wants us to stop has an obvious way to say
 * so; and failure returned as {@link Optional#empty()} rather than thrown, so one unreachable
 * source can never fail a student's question.
 *
 * <p>What had to change is everything that assumed nobody was waiting. That fetcher states
 * "nothing here runs while a student is waiting" and is built on it: thirty-second timeouts, a
 * one-request-per-second sleep inside a {@code synchronized} block, unbounded response bodies, and
 * redirects followed automatically. On a request path each of those is a defect.
 *
 * <p><b>Three ways a domain allowlist can be bypassed, and what stops each here.</b>
 *
 * <ol>
 *   <li><b>A redirect off the domain.</b> {@code Redirect.NORMAL} would follow a 302 from
 *       {@code who.int} to anywhere at all, and the allowlist would be decorative. Redirects are
 *       therefore <em>not</em> followed; a redirect response is refused outright.</li>
 *   <li><b>An unbounded body.</b> A multi-gigabyte response on a request thread is a denial of
 *       service. Bodies are read through a hard byte cap.</li>
 *   <li><b>A URL that is not on the domain at all.</b> Every fetch re-checks the host against the
 *       caller's declared domain, so an adapter cannot reach outside its own site even if an index
 *       page it parsed contained a link to somewhere else.</li>
 * </ol>
 *
 * <p>There is no rate limiting here by design. Blocking to be polite is the one thing a request
 * thread cannot do - and {@code Thread.sleep} inside {@code synchronized} would pin a virtual
 * thread's carrier, which this application uses throughout. Politeness comes from caching instead:
 * repeated questions are served from Redis and never reach the network at all.
 *
 * @author omaro
 */
@Slf4j
@Component
public class TrustedSourceFetcher {

  /**
   * Deliberately tight. The whole external step has a three-second budget, and a source that
   * cannot answer within it is worth less than the latency it costs.
   */
  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(3);

  /** Robots rules are re-read daily, as in the ingest fetcher. */
  private static final Duration ROBOTS_TTL = Duration.ofHours(24);

  /**
   * Generous for an article, ruinous for nothing. Public health pages are tens of kilobytes; a
   * response an order of magnitude past that is not a page we want to read on a request thread.
   */
  private static final int MAX_BODY_BYTES = 2 * 1024 * 1024;

  private static final String CACHE_PREFIX = "ai:ext:page:";

  /** Only markup is useful here. A PDF or an archive is not something we will parse. */
  private static final List<String> ACCEPTABLE_CONTENT_TYPES =
      List.of("text/html", "application/xhtml", "application/xml", "text/xml", "application/rss",
          "application/atom");

  private final HttpClient httpClient = HttpClient.newBuilder()
      .connectTimeout(CONNECT_TIMEOUT)
      // Never follow a redirect. See the class javadoc: this is what keeps the allowlist real.
      .followRedirects(HttpClient.Redirect.NEVER)
      .build();

  private final SimpleRobotRulesParser robotsParser = new SimpleRobotRulesParser();
  private final Map<String, CachedRobots> robotsCache = new ConcurrentHashMap<>();

  private final String userAgent;
  private final String robotName;
  private final StringRedisTemplate redis;

  public TrustedSourceFetcher(
      @Value("${unihealth.ai.external.user-agent:UniHealthBot/1.0 (+https://www.uniwa.gr; thesis project)}")
      String userAgent, StringRedisTemplate redis) {
    this.userAgent = userAgent;
    this.robotName = toRobotName(userAgent);
    this.redis = redis;
  }

  /**
   * Fetches a page, refusing anything that is not plainly on the expected domain.
   *
   * @param url            the page to read.
   * @param expectedDomain the adapter's own domain. A host that is neither this nor a subdomain of
   *                       it is refused without a request being made.
   * @param ttl            how long the body may be reused. Index listings take a short ttl because
   *                       they are the thing that changes; article pages take a long one.
   * @return the body, or empty when the URL is off-domain, disallowed by robots.txt, too large,
   *         the wrong content type, a redirect, or simply unreachable.
   */
  public Optional<String> fetch(String url, String expectedDomain, Duration ttl) {
    URI uri;
    try {
      uri = URI.create(url);
    } catch (IllegalArgumentException e) {
      log.warn("Refusing a malformed external URL [{}].", url);
      return Optional.empty();
    }

    if (!isOnDomain(uri, expectedDomain)) {
      // Not a warning about the network - a warning about our own parsing. An adapter that
      // produced this URL has followed a link off its own site.
      log.warn("Refusing [{}]: not on the expected domain [{}].", url, expectedDomain);
      return Optional.empty();
    }

    // Consulted only after the domain check, so a cached body can never be served for a URL that
    // would not be fetchable now.
    Optional<String> cached = fromCache(url);
    if (cached.isPresent()) {
      return cached;
    }

    if (!isAllowedByRobots(uri)) {
      log.info("robots.txt disallows [{}] for [{}]; skipping.", url, robotName);
      return Optional.empty();
    }

    Optional<String> body = send(uri);
    body.ifPresent(content -> toCache(url, content, ttl));

    return body;
  }

  /**
   * Not keyed by tenant, deliberately.
   *
   * <p>These are public pages and identical for every university, so a per-tenant key would
   * multiply the traffic to three public services by the number of tenants for no benefit at all.
   * Nothing student-specific is stored here - only what the authority published.
   */
  private Optional<String> fromCache(String url) {
    try {
      return Optional.ofNullable(redis.opsForValue().get(cacheKey(url)));
    } catch (Exception e) {
      // A cache miss is the safe interpretation of a broken cache: worst case we fetch again.
      log.debug("External page cache unavailable: {}", e.getMessage());
      return Optional.empty();
    }
  }

  private void toCache(String url, String body, Duration ttl) {
    try {
      redis.opsForValue().set(cacheKey(url), body, ttl);
    } catch (Exception e) {
      log.debug("Could not cache an external page: {}", e.getMessage());
    }
  }

  /** Hashed so a long URL cannot produce an unwieldy key, and so the key set stays uniform. */
  private String cacheKey(String url) {
    return CACHE_PREFIX + DigestUtils.sha256Hex(url);
  }

  /**
   * Host must equal the domain or be a subdomain of it.
   *
   * <p>The dot in the suffix check is load-bearing: without it {@code notwho.int} would pass a
   * check for {@code who.int}, which is exactly the kind of near-miss an allowlist exists to catch.
   */
  public boolean isOnDomain(URI uri, String expectedDomain) {
    if (!"https".equalsIgnoreCase(uri.getScheme())) {
      return false;
    }

    String host = uri.getHost();
    if (host == null || expectedDomain == null) {
      return false;
    }

    String normalised = host.toLowerCase(Locale.ROOT);
    String domain = expectedDomain.toLowerCase(Locale.ROOT);

    return normalised.equals(domain) || normalised.endsWith("." + domain);
  }

  private Optional<String> send(URI uri) {
    HttpRequest request = HttpRequest.newBuilder(uri)
        .timeout(REQUEST_TIMEOUT)
        .header("User-Agent", userAgent)
        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9")
        .GET()
        .build();

    try {
      HttpResponse<InputStream> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

      int status = response.statusCode();

      if (status >= 300 && status < 400) {
        // Not followed on purpose. A source that has moved needs its adapter updating, which is a
        // deliberate act, not something a redirect should decide for us at runtime.
        log.info("Refusing to follow a redirect from [{}] (HTTP {}).", uri, status);
        return Optional.empty();
      }

      if (status < 200 || status >= 300) {
        log.info("Fetch of [{}] returned HTTP {}.", uri, status);
        return Optional.empty();
      }

      if (!isAcceptableContentType(response)) {
        return Optional.empty();
      }

      return readCapped(response.body(), uri);

    } catch (IOException e) {
      log.info("Fetch of [{}] failed: {}", uri, e.getMessage());
      return Optional.empty();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return Optional.empty();
    }
  }

  private boolean isAcceptableContentType(HttpResponse<InputStream> response) {
    String contentType = response.headers().firstValue("Content-Type")
        .orElse("").toLowerCase(Locale.ROOT);

    boolean acceptable = ACCEPTABLE_CONTENT_TYPES.stream().anyMatch(contentType::contains);
    if (!acceptable) {
      log.info("Refusing [{}]: content type [{}].", response.uri(), contentType);
    }
    return acceptable;
  }

  /**
   * Reads at most {@link #MAX_BODY_BYTES}, and refuses rather than truncates.
   *
   * <p>Truncating would be worse than refusing: half a page parses into plausible-looking nonsense,
   * and the caller has no way to tell that it is looking at a fragment.
   */
  private Optional<String> readCapped(InputStream body, URI uri) throws IOException {
    try (body) {
      byte[] bytes = body.readNBytes(MAX_BODY_BYTES + 1);

      if (bytes.length > MAX_BODY_BYTES) {
        log.warn("Refusing [{}]: response exceeds {} bytes.", uri, MAX_BODY_BYTES);
        return Optional.empty();
      }

      return Optional.of(new String(bytes, StandardCharsets.UTF_8));
    }
  }

  private boolean isAllowedByRobots(URI uri) {
    BaseRobotRules rules = robotsFor(uri);
    return rules == null || rules.isAllowed(uri.toString());
  }

  private BaseRobotRules robotsFor(URI uri) {
    String host = uri.getScheme() + "://" + uri.getAuthority();

    CachedRobots cached = robotsCache.get(host);
    if (cached != null && cached.isFresh()) {
      return cached.rules();
    }

    BaseRobotRules rules = loadRobots(host);
    robotsCache.put(host, new CachedRobots(rules, Instant.now()));
    return rules;
  }

  private BaseRobotRules loadRobots(String host) {
    String robotsUrl = host + "/robots.txt";

    try {
      HttpRequest request = HttpRequest.newBuilder(URI.create(robotsUrl))
          .timeout(REQUEST_TIMEOUT)
          .header("User-Agent", userAgent)
          .GET()
          .build();

      HttpResponse<byte[]> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

      // RFC 9309: 4xx means no restrictions, 5xx means treat the whole site as disallowed.
      return robotsParser.parseContent(robotsUrl, response.body(),
          response.headers().firstValue("Content-Type").orElse(null), List.of(robotName));

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return robotsParser.failedFetch(503);
    } catch (Exception e) {
      // Fails closed, as in the ingest fetcher. Being unable to ask permission is not permission.
      log.warn("Could not read {} ({}); treating the host as disallowed.", robotsUrl,
          e.getMessage());
      return robotsParser.failedFetch(503);
    }
  }

  /**
   * RFC 9309 matches robots.txt groups on the bare product token, not the full header, and
   * crawler-commons rejects anything carrying a version or a URL - which fails closed and silently
   * skips the whole host.
   */
  private static String toRobotName(String userAgent) {
    String token = userAgent.split("[/\\s]", 2)[0]
        .toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9_-]", "");

    return token.isBlank() ? "unihealthbot" : token;
  }

  private record CachedRobots(BaseRobotRules rules, Instant fetchedAt) {
    boolean isFresh() {
      return Instant.now().isBefore(fetchedAt.plus(ROBOTS_TTL));
    }
  }
}
