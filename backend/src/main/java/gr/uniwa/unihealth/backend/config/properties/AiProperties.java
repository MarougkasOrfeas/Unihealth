package gr.uniwa.unihealth.backend.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Settings for the grounded assistant, under {@code unihealth.ai.*}.
 *
 * @author omaro
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "unihealth.ai")
public class AiProperties {

  /** Directory holding the persisted per-tenant embedding files. Created if absent. */
  private String vectorStorePath = "./data/ai";

  /**
   * How many passages are put in front of the model.
   *
   * <p>Six rather than the usual three because the chunks here are single page sections and short.
   * It is also bounded by the 8192-token window: more passages would start crowding out the
   * conversation history rather than adding evidence.
   */
  private int topK = 6;

  /** Rejects a chat message longer than this before it reaches the model. */
  private int maxMessageChars = 1000;

  /**
   * Cosine similarity below which a passage is not worth showing the model.
   *
   * <p>Deliberately low. A wrong-but-included passage is visible - the model cites it and the
   * answer reads oddly - whereas a threshold set too high silently returns nothing and the
   * assistant falls back to its training weights, which is the failure this whole feature exists to
   * remove. Tune it upward from observed scores, never downward from a guess.
   */
  private double similarityThreshold = 0.3;

  /** Live lookups at trusted health authorities. */
  private External external = new External();

  /**
   * Settings for consulting EODY, ECDC and WHO at request time.
   *
   * <p>A nested object rather than a flat {@code external-*} prefix so the whole feature can be
   * switched off with one property, and so it is obvious at a glance which settings belong to the
   * one part of this application that makes outbound requests while a student waits.
   */
  @Getter
  @Setter
  public static class External {

    /**
     * Master switch. Off by default: an installation should have to opt in to outbound traffic
     * rather than discover it, and a deployment with no network egress must still work.
     */
    private boolean enabled = false;

    /**
     * Ceiling for the whole external step - discovery, ranking and page fetches together, across
     * every source in parallel.
     *
     * <p>Three seconds, on hardware where generation already costs several. Past that the answer
     * is late enough that the student is better served by the local corpus with an honest note
     * that nothing recent was found.
     */
    private Duration budget = Duration.ofSeconds(3);

    /**
     * Nothing older than this is offered as "recent". Ninety days covers a seasonal outbreak
     * without dredging up last year's.
     */
    private int maxAgeDays = 90;

    /** How many external passages reach the prompt. Kept small so they cannot crowd out the NHS. */
    private int topK = 3;

    /** Characters kept from any one fetched page, for the same reason. */
    private int maxPassageChars = 1200;

    /**
     * Below this best local similarity score the corpus is treated as not covering the question,
     * which is the second thing that can open the gate. Set from observed scores, not guessed.
     */
    private double weakCoverageScore = 0.45;

    /** Per student, per hour. A student asking questions is fine; a script is not. */
    private int maxLookupsPerHour = 10;

    /** How long a source's index listing is reused before being fetched again. */
    private Duration indexCacheTtl = Duration.ofMinutes(30);

    /** How long a fetched page body is reused. */
    private Duration pageCacheTtl = Duration.ofHours(24);

  }
}
