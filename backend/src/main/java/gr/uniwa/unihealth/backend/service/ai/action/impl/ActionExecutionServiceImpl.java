package gr.uniwa.unihealth.backend.service.ai.action.impl;

import gr.uniwa.unihealth.backend.dto.UserPreferencesDTO;
import gr.uniwa.unihealth.backend.service.UserPreferencesService;
import gr.uniwa.unihealth.backend.service.ai.action.ActionExecutionService;
import gr.uniwa.unihealth.backend.service.ai.action.PendingAction;
import gr.uniwa.unihealth.backend.service.ai.action.PendingActionService;
import gr.uniwa.unihealth.backend.service.ai.action.PreferencesFingerprint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Executes confirmed AI actions after validating that they are valid,
 * unused, and based on the user's current preferences.
 *
 *  @author omaro
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActionExecutionServiceImpl implements ActionExecutionService {

  private final PendingActionService pendingActionService;
  private final UserPreferencesService userPreferencesService;

  @Override
  public Outcome resolve(String actionId, boolean approved) {
    if (!approved) {
      pendingActionService.discard(actionId);
      log.info("Student declined a proposed change.");
      return Outcome.refused("ai.action.cancelled");
    }

    Optional<PendingAction> pending = pendingActionService.consume(actionId);

    if (pending.isEmpty()) {
      // The action is invalid, expired, or already used.
      log.info("A confirmation arrived for an unknown or spent action.");
      return Outcome.refused("ai.action.expired");
    }

    PendingAction action = pending.get();
    UserPreferencesDTO current = userPreferencesService.findMyPreferences();
    // Reject the action if preferences changed after it was proposed.
    if (!PreferencesFingerprint.of(current).equals(action.getPreStateHash())) {
      log.info("Refusing a confirmation for {}: the settings changed after it was proposed.",
          action.getKind());
      return Outcome.refused("ai.action.stale");
    }

    userPreferencesService.updateMyPreferences(action.getTarget());

    log.info("Applied {} -> {} after explicit confirmation.",
        action.getKind(), action.isEnabled());

    return Outcome.applied("ai.action.applied");
  }
}
