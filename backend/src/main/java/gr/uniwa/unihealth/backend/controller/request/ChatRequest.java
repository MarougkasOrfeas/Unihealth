package gr.uniwa.unihealth.backend.controller.request;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(@NotBlank String message) {
}
