package gr.uniwa.unihealth.backend.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * One question from a student.
 *
 * @param message        the question. Capped because the controller hands it straight to a local
 *                       model whose read timeout is two minutes: without a bound, a single large
 *                       paste occupies the GPU long enough to deny the endpoint to everyone else.
 *                       A thousand characters is far more than any real question asked here.
 * @param conversationId the chat to continue, or {@code null} to start a new one. Never trusted as
 *                       an identity: the server loads it by (id, owner) and treats a conversation
 *                       belonging to somebody else as absent.
 */
public record ChatRequest(
    @NotBlank @Size(max = 1000) String message,
    @Size(max = 36) String conversationId) {
}
