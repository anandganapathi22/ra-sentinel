package com.rasentinel.agent.ai;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;

public record AiAssessment(
        @JsonPropertyDescription("One of: Low, Medium, High, Warning, Normal.")
        String severity,
        @JsonPropertyDescription("One or two sentences describing the likely root cause, grounded only in the evidence and context provided.")
        String rootCause,
        @JsonPropertyDescription("A single recommended next action, phrased for a human approver to read.")
        String recommendedAction,
        @JsonPropertyDescription("Zero or more actions chosen only from the allowed action vocabulary given in the prompt. Never include an action outside that list.")
        List<String> allowedActions,
        @JsonPropertyDescription("One of: low, medium, high.")
        String confidence
) {
}
