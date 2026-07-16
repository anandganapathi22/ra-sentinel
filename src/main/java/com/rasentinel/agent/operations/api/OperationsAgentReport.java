package com.rasentinel.agent.operations.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OperationsAgentReport(
        UUID runId,
        String agentName,
        String subject,
        String severity,
        String rootCause,
        List<String> evidence,
        List<TimelineEvent> timeline,
        String recommendedAction,
        boolean requiresHumanApproval,
        List<String> allowedActions,
        List<String> blockedActions,
        Instant createdAt
) {
}
