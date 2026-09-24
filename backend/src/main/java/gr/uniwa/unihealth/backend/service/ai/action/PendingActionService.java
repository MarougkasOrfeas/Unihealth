package gr.uniwa.unihealth.backend.service.ai.action;

import gr.uniwa.unihealth.backend.dto.ai.PendingActionDTO;

import java.util.Optional;

/**
 * Holds proposed changes between the turn that suggests one and the click that approves it.
 *
 * @author omaro
 */
public interface PendingActionService {

  /**
   * Records a proposal for the logged-in student and returns the card to show them.
   *
   * <p>Reads the current preferences first and stores the whole intended end state, because
   * {@code updateMyPreferences} replaces all three settings at once.
   *
   * @param kind    which setting to change.
   * @param enabled the value being proposed.
   * @return the card, or empty when the setting already has that value and there is nothing to
   *         confirm.
   */
  Optional<PendingActionDTO> propose(AiActionKind kind, boolean enabled);

  /**
   * Takes the action for this id, if it belongs to the caller and has not been used.
   *
   * <p>Single-use and atomic: the entry is removed as it is read, so a replayed confirmation finds
   * nothing. The key is rebuilt from the caller's own tenant and username rather than from anything
   * in the request, so a stolen id is useless to a different user.
   */
  Optional<PendingAction> consume(String actionId);

  /** Drops a proposal the student declined, so it cannot be confirmed later by a stale tab. */
  void discard(String actionId);
}
