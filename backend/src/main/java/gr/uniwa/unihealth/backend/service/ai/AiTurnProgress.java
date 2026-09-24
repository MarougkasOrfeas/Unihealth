package gr.uniwa.unihealth.backend.service.ai;

/**
 * Where the chat pipeline reports what it is doing.
 *
 * <p>A one-method interface rather than a bare {@code Consumer} so the intent is legible at the
 * call site and so the no-op has an obvious home. The streaming endpoint supplies an implementation
 * that writes a line to the response; the plain JSON endpoint supplies {@link #NONE}.
 *
 * <p>Implementations must never throw. A browser that has closed the connection mid-turn is normal
 * - the student navigated away - and it must not become an error inside the pipeline, which by then
 * may be halfway through work worth finishing.
 *
 * @author omaro
 */
@FunctionalInterface
public interface AiTurnProgress {

  /** For callers that do not stream. */
  AiTurnProgress NONE = stage -> {
    // Intentionally empty.
  };

  void report(AiTurnStage stage);
}
