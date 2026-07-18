package com.rasentinel.agent.operations.core;

import com.rasentinel.agent.operations.api.TimelineEvent;
import com.rasentinel.agent.tools.records.CorrelationEvent;
import java.util.Comparator;
import java.util.List;

final class ReportSupport {
    static final List<String> APPROVAL_GATED_OPERATIONS = List.of(
            "OPEN_INCIDENT",
            "RESUBMIT_TO_CONTRACT_VAULT_AFTER_APPROVAL",
            "RESEND_LINK_AFTER_APPROVAL",
            "REGENERATE_LINK_AFTER_APPROVAL",
            "RETRY_SYNC_AFTER_APPROVAL",
            "FAIL_OVER_ENDPOINT_AFTER_APPROVAL"
    );

    private static final List<String> NO_OP_ACTIONS = List.of(
            "NO_ACTION",
            "CONTINUE_MONITORING",
            "RUN_RA_TROUBLESHOOTING"
    );

    private ReportSupport() {
    }

    static List<TimelineEvent> timeline(List<CorrelationEvent> events) {
        return events.stream()
                .sorted(Comparator.comparing(CorrelationEvent::occurredAt))
                .map(event -> new TimelineEvent(
                        event.occurredAt(),
                        event.source(),
                        event.message(),
                        event.correlationId()
                ))
                .toList();
    }

    /**
     * Never trust an AI-proposed action verbatim: only actions present in the agent's own
     * vocabulary can be surfaced, regardless of what the model returns.
     */
    static List<String> clampActions(List<String> proposed, List<String> vocabulary) {
        if (proposed == null || proposed.isEmpty()) {
            return List.of("NO_ACTION");
        }
        var clamped = proposed.stream()
                .filter(vocabulary::contains)
                .distinct()
                .toList();
        return clamped.isEmpty() ? List.of("NO_ACTION") : clamped;
    }

    static boolean requiresApproval(List<String> clampedActions) {
        return !NO_OP_ACTIONS.containsAll(clampedActions);
    }
}
