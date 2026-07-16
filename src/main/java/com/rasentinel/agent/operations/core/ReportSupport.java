package com.rasentinel.agent.operations.core;

import com.rasentinel.agent.operations.api.TimelineEvent;
import com.rasentinel.agent.tools.records.CorrelationEvent;
import java.util.Comparator;
import java.util.List;

final class ReportSupport {
    static final List<String> APPROVAL_GATED_OPERATIONS = List.of(
            "OPEN_INCIDENT",
            "RESUBMIT_TO_TAS_AFTER_APPROVAL",
            "RESEND_LINK_AFTER_APPROVAL",
            "REGENERATE_LINK_AFTER_APPROVAL",
            "RETRY_SYNC_AFTER_APPROVAL",
            "FAIL_OVER_ENDPOINT_AFTER_APPROVAL"
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
}
