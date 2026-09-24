package gr.uniwa.unihealth.backend.dto.ai;

/**
 * Represents a setting change shown on a confirmation card.
 *
 * @param labelKey key used to display the setting label
 * @param from current value
 * @param to proposed value
 *
 * @author omaro
 */
public record ActionFieldChangeDTO(String labelKey, boolean from, boolean to) {
}

