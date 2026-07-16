package com.rasentinel.agent.operations.api;

import jakarta.validation.constraints.NotBlank;

public record AgentCaseRequest(
        @NotBlank String raId,
        String location,
        String question
) {
}
