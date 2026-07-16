package com.rasentinel.agent.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record RaCompletionResponse(
        UUID runId,
        String raId,
        String status,
        String likelyCause,
        List<String> evidence,
        String recommendedAction,
        String confidence,
        boolean requiresHumanApproval,
        List<String> allowedActions,
        List<String> blockedActions,
        Map<String, Object> toolResults,
        Instant createdAt
) {
}
