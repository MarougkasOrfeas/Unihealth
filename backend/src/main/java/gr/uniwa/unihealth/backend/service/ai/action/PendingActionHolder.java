package gr.uniwa.unihealth.backend.service.ai.action;

import gr.uniwa.unihealth.backend.dto.ai.PendingActionDTO;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Carries a proposal from the tool that created it out to the response for the turn.
 *
 * <p>A tool runs in the middle of {@code chatClient.prompt()...call()} and can only hand the model
 * back a string. That string is not good enough to drive the UI: it goes through the model, which
 * may paraphrase it, ignore it, or announce that the change has already been made. So the tool also
 * parks the structured proposal here, and the chat service picks it up after generation and puts it
 * on the response itself.
 *
 * <p>A {@link ThreadLocal} rather than a request-scoped bean, because the value has to survive
 * exactly the span of one {@code answer(...)} call and be visible to code running inside the chat
 * client on the same thread. The chat service clears it in a {@code finally}, which is the part
 * that matters: a leaked value would attach one student's proposal to the next request handled by a
 * reused thread.
 *
 * @author omaro
 */
@Component
public class PendingActionHolder {

  private static final ThreadLocal<PendingActionDTO> PROPOSED = new ThreadLocal<>();

  /** Called by a tool. Last proposal in a turn wins; a turn should only ever make one. */
  public void hold(PendingActionDTO action) {
    PROPOSED.set(action);
  }

  public Optional<PendingActionDTO> proposed() {
    return Optional.ofNullable(PROPOSED.get());
  }

  /** Must be called in a {@code finally} at the end of every turn. */
  public void clear() {
    PROPOSED.remove();
  }
}
