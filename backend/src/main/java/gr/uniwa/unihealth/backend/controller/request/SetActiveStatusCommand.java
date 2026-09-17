package gr.uniwa.unihealth.backend.controller.request;

/**
 * Request body for activating or deactivating a resource. Shared by the school and department
 * endpoints, which both take only an id and the desired state.
 */
public record SetActiveStatusCommand(String id, boolean active) {
}
