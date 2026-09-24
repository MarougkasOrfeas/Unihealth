package gr.uniwa.unihealth.backend.service.ai.external;

import gr.uniwa.unihealth.backend.config.context.AuthenticationContext;
import gr.uniwa.unihealth.backend.config.context.TenantContext;
import gr.uniwa.unihealth.backend.config.properties.AiProperties;
import gr.uniwa.unihealth.backend.service.ai.retrieval.RecencyDetector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * "When does this application talk to a government website" has exactly one answer, and this is the
 * test that holds it to it.
 *
 * <p>Each condition is exercised alone, because the interesting failure is not the gate refusing
 * too much - that just costs recency - but one condition quietly ceasing to be checked while the
 * others still pass.
 */
class ExternalGateTest {

  private RecencyDetector recencyDetector;
  private AiProperties aiProperties;
  private StringRedisTemplate redis;
  private ValueOperations<String, String> valueOps;
  private ExternalGate gate;

  /** A passage carrying a similarity score, which is what "weak coverage" is measured on. */
  private static Document scored(double score) {
    return Document.builder().id("x").text("text").metadata(Map.of()).score(score).build();
  }

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    recencyDetector = mock(RecencyDetector.class);
    redis = mock(StringRedisTemplate.class);
    valueOps = mock(ValueOperations.class);

    AuthenticationContext authenticationContext = mock(AuthenticationContext.class);
    TenantContext tenantContext = mock(TenantContext.class);

    when(authenticationContext.getCurrentUsername()).thenReturn("student");
    when(tenantContext.getCurrentTenant()).thenReturn("UNIWA_C1");

    when(redis.opsForValue()).thenReturn(valueOps);
    when(valueOps.increment(anyString())).thenReturn(1L);

    aiProperties = new AiProperties();
    aiProperties.getExternal().setEnabled(true);
    aiProperties.getExternal().setWeakCoverageScore(0.45);
    aiProperties.getExternal().setMaxLookupsPerHour(10);

    gate = new ExternalGate(recencyDetector, aiProperties, redis, authenticationContext,
        tenantContext);
  }

  @Test
  @DisplayName("opens when recency is asked about a health topic the corpus does cover")
  void opensOnRecency() {
    when(recencyDetector.asksAboutRecent(any())).thenReturn(true);

    assertThat(gate.shouldConsultAuthorities("κρούσματα;", List.of(scored(0.9)), true)).isTrue();
  }

  @Test
  @DisplayName("opens when the corpus covers the question poorly, even without recency wording")
  void opensOnWeakCoverage() {
    // The signal that is independent of phrasing: a question the library cannot answer is worth
    // taking outside whether or not the student used the word "recent".
    when(recencyDetector.asksAboutRecent(any())).thenReturn(false);

    assertThat(gate.shouldConsultAuthorities("something obscure", List.of(scored(0.2)), true))
        .isTrue();
  }

  @Test
  @DisplayName("stays shut on an ordinary question the corpus answers well")
  void closedOnOrdinaryQuestion() {
    // The common path, and the one that must never spend a network request.
    when(recencyDetector.asksAboutRecent(any())).thenReturn(false);

    assertThat(gate.shouldConsultAuthorities("τι κάνω για τον πονόλαιμο;", List.of(scored(0.8)),
        true)).isFalse();
  }

  @Test
  @DisplayName("stays shut when the question is not about health at all")
  void closedWhenNotHealthTopical() {
    // Checked before recency on purpose: "what is happening right now" is only our business when
    // it is a health question. Without this, «τι νέα;» would reach WHO.
    when(recencyDetector.asksAboutRecent(any())).thenReturn(true);

    assertThat(gate.shouldConsultAuthorities("τι καιρό κάνει σήμερα;", List.of(), false)).isFalse();
  }

  @Test
  @DisplayName("stays shut when the feature is switched off")
  void closedWhenDisabled() {
    aiProperties.getExternal().setEnabled(false);
    when(recencyDetector.asksAboutRecent(any())).thenReturn(true);

    assertThat(gate.shouldConsultAuthorities("κρούσματα;", List.of(scored(0.9)), true)).isFalse();
  }

  @Test
  @DisplayName("stays shut once the rate limit is spent")
  void closedWhenRateLimited() {
    when(recencyDetector.asksAboutRecent(any())).thenReturn(true);
    when(valueOps.increment(anyString())).thenReturn(11L);

    assertThat(gate.shouldConsultAuthorities("κρούσματα;", List.of(scored(0.9)), true)).isFalse();
  }

  @Test
  @DisplayName("stays shut when Redis cannot be reached, rather than losing the rate limit")
  void closedWhenRedisUnavailable() {
    // Fails closed. Failing open would remove the only cap on how often a multi-user application
    // points itself at three public services.
    when(recencyDetector.asksAboutRecent(any())).thenReturn(true);
    when(redis.opsForValue()).thenThrow(new IllegalStateException("redis down"));

    assertThat(gate.shouldConsultAuthorities("κρούσματα;", List.of(scored(0.9)), true)).isFalse();
  }

  @Test
  @DisplayName("treats no local passages as weak coverage")
  void noPassagesIsWeakCoverage() {
    when(recencyDetector.asksAboutRecent(any())).thenReturn(false);

    assertThat(gate.shouldConsultAuthorities("obscure", List.of(), true)).isTrue();
  }
}
