package gr.uniwa.unihealth.backend.service.ai;

/**
 * What the assistant is doing right now, reported to the browser while it works.
 *
 * <p>A turn on this hardware can take ten seconds or more: a local model, a cold embedding, and
 * sometimes three public websites. A spinner for that long reads as a hang. Naming the stage turns
 * the wait into something the student can follow, and - because the stages are reported as they
 * actually happen rather than animated on a timer - it is also honest. "Let me check online" only
 * appears when the assistant really did go outside, which is roughly one turn in ten.
 *
 * <p>Each constant carries a lexicon key rather than text, like every other user-facing string in
 * this application, so the progress messages follow a language switch and are owned by whoever owns
 * the lexicon.
 *
 * @author omaro
 */
public enum AiTurnStage {

  /** Sent immediately, so something changes on screen before any slow work begins. */
  THINKING("ai.stage.thinking"),

  /** Searching the vetted NHS and university corpus. Usually fast once embeddings are warm. */
  SEARCHING_LIBRARY("ai.stage.searchingLibrary"),

  /**
   * Consulting EODY, ECDC and WHO. Only ever sent when the gate actually opened, which is what
   * makes this worth showing at all.
   */
  SEARCHING_ONLINE("ai.stage.searchingOnline"),

  /** Reading the most relevant pages found outside. */
  READING_SOURCES("ai.stage.readingSources"),

  /** The model is writing. Reliably the longest stage, so it is worth naming separately. */
  WRITING("ai.stage.writing");

  private final String messageKey;

  AiTurnStage(String messageKey) {
    this.messageKey = messageKey;
  }

  public String messageKey() {
    return messageKey;
  }
}
