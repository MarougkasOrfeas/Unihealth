package gr.uniwa.unihealth.backend.controller.request;

public record SetGroupStatusCommand(String id, boolean active) {
}
