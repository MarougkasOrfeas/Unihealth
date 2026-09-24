package gr.uniwa.unihealth.backend.service.ai.action;

/**
 * Applies a change a student has explicitly approved.
 *
 * <p>The only code path in this feature that writes anything. It is reached from a controller
 * endpoint triggered by a button, never from a tool and never from a generated sentence - the model
 * does not hold the {@code actionId} and has no way to call this.
 *
 * @author omaro
 */
public interface ActionExecutionService {

  /**
   * The outcome, as a lexicon key the client renders.
   *
   * @param messageKey what to tell the student.
   * @param applied    whether anything was actually written.
   */
  record Outcome(String messageKey, boolean applied) {

    public static Outcome applied(String messageKey) {
      return new Outcome(messageKey, true);
    }

    public static Outcome refused(String messageKey) {
      return new Outcome(messageKey, false);
    }
  }

  /**
   * @param actionId the proposal being answered.
   * @param approved what the student clicked.
   */
  Outcome resolve(String actionId, boolean approved);
}
