package gr.uniwa.unihealth.backend.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A student's answer to a confirmation card.
 *
 * @param actionId the proposal being answered. Opaque, single-use, and only meaningful when
 *                 combined with the caller's own tenant and username on the server, so it is not a
 *                 capability anyone else can use.
 * @param approved what they clicked.
 */
public record ConfirmActionRequest(
    @NotBlank @Size(max = 64) String actionId,
    boolean approved) {
}
