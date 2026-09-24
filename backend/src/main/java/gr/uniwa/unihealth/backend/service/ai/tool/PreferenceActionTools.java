package gr.uniwa.unihealth.backend.service.ai.tool;

import gr.uniwa.unihealth.backend.service.ai.action.AiActionKind;
import gr.uniwa.unihealth.backend.service.ai.action.PendingActionHolder;
import gr.uniwa.unihealth.backend.service.ai.action.PendingActionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * The one thing the assistant may do that eventually changes something - and even this one does
 * not change it.
 *
 * <p>The tool <em>proposes</em>. It writes a pending action to Redis and hands the model back a
 * sentence; the student then sees a card with the literal before-and-after and clicks. Only that
 * click reaches {@code updateMyPreferences}, through a separate controller endpoint.
 *
 * <p>The property this buys is worth stating plainly: <b>no code path from the language model
 * writes to the database.</b> The model never learns the {@code actionId}, so it cannot confirm its
 * own proposal even if it decides to try, and a prompt injection in a retrieved passage has nothing
 * to reach for. What the model influences is which card appears - never what the click does.
 *
 * <p>The setting is an enum parameter rather than free text, so the schema the model is shown
 * enumerates exactly what is possible. {@link AiActionKind} is the entire capability list, and it
 * contains no deletion of anything.
 *
 * @author omaro
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PreferenceActionTools {

  private final PendingActionService pendingActionService;
  private final PendingActionHolder pendingActionHolder;

  @Tool(description = "Propose changing one of the student's own settings. This does NOT apply the "
      + "change: it shows them a confirmation card that they must approve. Tell them you have "
      + "asked them to confirm. Never say the change has been made.")
  public String proposePreferenceChange(
      @ToolParam(description = "Which setting to change") AiActionKind setting,
      @ToolParam(description = "true to turn it on, false to turn it off") boolean enabled) {

    // No identity parameter, and none possible: PendingActionService resolves the student from the
    // security context. There is nothing here the model could set to act on somebody else.
    return pendingActionService.propose(setting, enabled)
        .map(proposal -> {
          pendingActionHolder.hold(proposal);
          return "A confirmation card has been shown to the student. Ask them to confirm it. Do "
              + "not claim the change has been made.";
        })
        .orElseGet(() -> {
          log.debug("Model proposed {} -> {}, which is already the current value.",
              setting, enabled);
          return "That setting is already set to the requested value. Tell the student, and do not "
              + "propose it again.";
        });
  }
}
