package gr.uniwa.unihealth.backend.service.ai.action;

import gr.uniwa.unihealth.backend.dto.UserPreferencesDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A change the assistant has proposed and a human has not yet approved.
 *
 * <p>Held in Redis for a few minutes between the turn that proposes and the click that confirms.
 * The language model never sees the {@code actionId}: it is returned to the browser in the chat
 * response and comes back on a separate endpoint, so the only thing that can trigger a write is a
 * person pressing a button.
 *
 * <p>A plain bean rather than a record because it round-trips through Jackson, and this project
 * runs Jackson 3 for HTTP while this payload is written with Jackson 2. Getters and a no-arg
 * constructor bind under either without needing a module registered.
 *
 * @author omaro
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PendingAction {

  private String actionId;

  private AiActionKind kind;

  /** True when the proposal turns the setting on. */
  private boolean enabled;

  /**
   * The <em>entire</em> preferences payload to save, not just the field being changed.
   *
   * <p>{@code updateMyPreferences} replaces all three settings at once. Storing a delta and
   * rebuilding the payload at confirmation time would mean reading the other two fields twice and
   * hoping they agreed; storing the whole intended end state means the write is exactly what the
   * student was shown.
   */
  private UserPreferencesDTO target;

  /**
   * A digest of the three settings as they were when this was proposed.
   *
   * <p>Re-checked before the write. If the student changed a setting in another tab between the
   * proposal and the click, the stored payload would silently revert it - so the confirmation is
   * refused instead and they are asked again. Ordinary optimistic concurrency, in the one place
   * where the two windows are minutes apart by design.
   */
  private String preStateHash;

  /** True when going through with this destroys data. Surfaced on the confirmation card. */
  private boolean irreversible;
}
