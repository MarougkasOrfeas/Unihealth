package gr.uniwa.unihealth.backend.service.ai.external;

import gr.uniwa.unihealth.backend.config.context.AuthenticationContext;
import gr.uniwa.unihealth.backend.config.context.TenantContext;
import gr.uniwa.unihealth.backend.config.properties.AiProperties;
import gr.uniwa.unihealth.backend.service.ai.retrieval.RecencyDetector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Decides whether a turn is allowed to leave the machine.
 *
 * <p>All of the conditions live here rather than being spread through the retriever, because "when
 * does this application talk to a government website" is a question that should have one answer in
 * one file. Every condition must hold; any one of them closes the gate.
 *
 * <p>An unconditional lookup would be wrong three times over: it would add seconds to every turn on
 * hardware that has none to spare, it would put lower-quality information in front of questions the
 * vetted corpus already answers better, and it would point a multi-user application at three public
 * services that did not ask for the traffic.
 *
 * @author omaro
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalGate {

  private static final String RATE_KEY_PREFIX = "ai:ext:rate:";

  /**
   * The window over which lookups are counted. An hour rather than a day: a burst is what needs
   * catching, and a student who asks a few recency questions across a day is behaving normally.
   */
  private static final Duration RATE_WINDOW = Duration.ofHours(1);

  private final RecencyDetector recencyDetector;
  private final AiProperties aiProperties;
  private final StringRedisTemplate redis;
  private final AuthenticationContext authenticationContext;
  private final TenantContext tenantContext;

  /**
   * @param question       what the student asked.
   * @param localPassages  what the vetted corpus already found.
   * @param healthTopical  whether the question resolved to a symptom or matched a medical concept.
   * @return true when a live lookup is warranted.
   */
  public boolean shouldConsultAuthorities(String question, List<Document> localPassages,
      boolean healthTopical) {

    if (!aiProperties.getExternal().isEnabled()) {
      return false;
    }

    // Stops «τι καιρό κάνει;» and «πώς σε λένε;» from reaching WHO. Deliberately checked before
    // recency: "what is happening right now" is only our business when it is about health.
    if (!healthTopical) {
      return false;
    }

    boolean recencyAsked = recencyDetector.asksAboutRecent(question);
    boolean coverageWeak = isCoverageWeak(localPassages);

    if (!recencyAsked && !coverageWeak) {
      return false;
    }

    // Checked last, and only once the question has actually earned a lookup, so an ordinary
    // conversation never consumes a student's budget.
    if (!withinRateLimit()) {
      log.info("External lookup suppressed: rate limit reached.");
      return false;
    }

    log.debug("External lookup allowed (recency={}, weakCoverage={}).", recencyAsked, coverageWeak);
    return true;
  }

  /**
   * True when the vetted corpus had little or nothing to say.
   *
   * <p>The second signal, independent of wording: a question the local library cannot answer is
   * worth taking outside even when it never uses the word "recent". Scores come from the vector
   * store, so the floor must be set from observed values rather than guessed.
   */
  private boolean isCoverageWeak(List<Document> localPassages) {
    if (localPassages == null || localPassages.isEmpty()) {
      return true;
    }

    double best = localPassages.stream()
        .map(Document::getScore)
        .filter(Objects::nonNull)
        .mapToDouble(Double::doubleValue)
        .max()
        .orElse(0d);

    return best < aiProperties.getExternal().getWeakCoverageScore();
  }

  /**
   * A fixed window counted in Redis, keyed per tenant and per student.
   *
   * <p>Fixed window rather than sliding: the point is to stop a script turning the assistant into a
   * crawler, not to meter usage precisely, and the simpler structure is one {@code INCR}.
   */
  private boolean withinRateLimit() {
    String key = RATE_KEY_PREFIX + tenantContext.getCurrentTenant() + ':'
        + authenticationContext.getCurrentUsername();

    try {
      Long count = redis.opsForValue().increment(key);

      if (count != null && count == 1L) {
        // Only the first write in a window sets the expiry, so the window does not slide forward
        // with every request and become permanent.
        redis.expire(key, RATE_WINDOW);
      }

      return count == null || count <= aiProperties.getExternal().getMaxLookupsPerHour();

    } catch (Exception e) {
      // Redis being unavailable must not silently remove the rate limit. Failing closed costs a
      // recency answer; failing open points the application at three public services with no cap.
      log.warn("Could not check the external lookup rate limit ({}); suppressing the lookup.",
          e.getMessage());
      return false;
    }
  }
}
