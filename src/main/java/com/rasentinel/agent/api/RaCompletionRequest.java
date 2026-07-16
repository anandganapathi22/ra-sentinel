package com.rasentinel.agent.api;

import jakarta.validation.constraints.NotBlank;

public record RaCompletionRequest(
        @NotBlank String raId,
        String question
) {
}
