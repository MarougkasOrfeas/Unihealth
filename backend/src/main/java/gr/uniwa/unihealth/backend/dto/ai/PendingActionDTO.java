package gr.uniwa.unihealth.backend.dto.ai;

import java.util.List;

/**
 * Represents a proposed change awaiting user confirmation.
 *
 * @param actionId identifier used to confirm the action
 * @param kind type of setting being changed
 * @param labelKey key used to display the setting label
 * @param changes proposed setting changes
 * @param irreversible whether the action causes irreversible changes
 *
 * @author omaro */
public record PendingActionDTO(String actionId, String kind, String labelKey,
                               List<ActionFieldChangeDTO> changes, boolean irreversible) {
}
