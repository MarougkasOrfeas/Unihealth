package gr.uniwa.unihealth.data.nhs;

import crawlercommons.robots.BaseRobotRules;
import crawlercommons.robots.SimpleRobotRulesParser;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
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
 * Fetches remote pages politely.
 *
 * <p>This is the only place the ingestion pipeline talks to the outside world, so that politeness
 * is a property of the system rather than something each ingester has to remember:
 *
 * <ul>
 *   <li><b>robots.txt is honoured</b> (RFC 9309, via crawler-commons), cached per host for a day.
 *       This matters here: nhs.uk allows {@code /symptoms/} but disallows {@code /Conditions/},
 *       and the conditions pages are deliberately never fetched.</li>
 *   <li><b>One request per second per host</b>, serialised.</li>
 *   <li><b>A self-identifying User-Agent</b> with a contact address, so an administrator who wants
 *       us to stop has an obvious way to say so.</li>
 * </ul>
 *
 * <p>Nothing here runs while a student is waiting - the only caller is a developer-triggered
 * ingestion run.
 *
 * <p>Note: the shelved AI changelist adds a near-identical {@code service.ai.ingest.SourceFetcher}.
 * If that work lands, collapse the two rather than keeping both.
 *
 * @author omaro
 */
@Slf4j
public class PoliteHttpFetcher {

  private static final Duration ROBOTS_TTL = Duration.ofHours(24);
  private static final Duration TIMEOUT = Duration.ofSeconds(30);
  private static final long DELAY_MS = 1_000;

  private final HttpClient httpClient = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(10))
      .followRedirects(HttpClient.Redirect.NORMAL)
      .build();

  private final SimpleRobotRulesParser robotsParser = new SimpleRobotRulesParser();
  private final Map<String, CachedRobots> robotsCache = new ConcurrentHashMap<>();
  private final Map<String, Object> hostLocks = new ConcurrentHashMap<>();
  private final Map<String, Long> lastRequestAt = new ConcurrentHashMap<>();

  private final String userAgent;

  /**
   * The bare product token from the User-Agent, e.g. {@code unihealthbot}. RFC 9309 matches
   * robots.txt groups on this, not on the full header, and crawler-commons rejects anything with a
   * version or URL in it - which fails closed and silently skips the whole host.
   */
  private final String robotName;

  public PoliteHttpFetcher(String userAgent) {
    this.userAgent = userAgent;
    this.robotName = toRobotName(userAgent);
  }

  private static String toRobotName(String userAgent) {
    String token = userAgent.split("[/\\s]", 2)[0].toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
    return token.isBlank() ? "unihealthbot" : token;
  }

  /**
   * @param url the page to fetch
   * @return the body, or empty when robots.txt disallows it or the fetch failed
   */
  public Optional<String> fetch(String url) {
    URI uri;
    try {
      uri = URI.create(url);
    } catch (IllegalArgumentException e) {
      log.warn("Skipping malformed ingest URL [{}].", url);
      return Optional.empty();
    }

    if (!isAllowed(uri)) {
      log.info("robots.txt disallows [{}] for our user agent; skipping.", url);
      return Optional.empty();
    }

    // Serialise per host so the delay below is actually observed under concurrency.
    Object lock = hostLocks.computeIfAbsent(uri.getHost(), host -> new Object());
    synchronized (lock) {
      throttle(uri.getHost());
      return doFetch(uri);
    }
  }

  private Optional<String> doFetch(URI uri) {
    HttpRequest request = HttpRequest.newBuilder(uri)
        .timeout(TIMEOUT)
        .header("User-Agent", userAgent)
        .GET()
        .build();

    try {
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

      int status = response.statusCode();
      if (status == 429 || status == 503) {
        // Back off rather than retrying into a rate limiter.
        log.warn("Source [{}] asked us to slow down (HTTP {}); abandoning this fetch.", uri, status);
        return Optional.empty();
      }
      if (status < 200 || status >= 300) {
        log.warn("Fetch of [{}] returned HTTP {}.", uri, status);
        return Optional.empty();
      }

      return Optional.of(response.body());

    } catch (IOException e) {
      log.warn("Fetch of [{}] failed: {}", uri, e.getMessage());
      return Optional.empty();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return Optional.empty();
    }
  }

  private boolean isAllowed(URI uri) {
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
          .timeout(TIMEOUT)
          .header("User-Agent", userAgent)
          .GET()
          .build();

      HttpResponse<byte[]> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

      // RFC 9309: 4xx means no restrictions; 5xx means treat the whole site as disallowed.
      return robotsParser.parseContent(robotsUrl, response.body(),
          response.headers().firstValue("Content-Type").orElse(null), List.of(robotName));

    } catch (Exception e) {
      log.warn("Could not read {} ({}); treating the host as disallowed for safety.",
          robotsUrl, e.getMessage());
      return robotsParser.failedFetch(503);
    }
  }

  private void throttle(String host) {
    Long previous = lastRequestAt.get(host);

    if (previous != null) {
      long wait = DELAY_MS - (System.currentTimeMillis() - previous);
      if (wait > 0) {
        try {
          Thread.sleep(wait);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }

    lastRequestAt.put(host, System.currentTimeMillis());
  }

  private record CachedRobots(BaseRobotRules rules, Instant fetchedAt) {

    boolean isFresh() {
      return Instant.now().isBefore(fetchedAt.plus(ROBOTS_TTL));
    }
  }
}
