package com.rasentinel.agent.operations.api;

import java.time.Instant;

public record TimelineEvent(
        Instant occurredAt,
        String system,
        String event,
        String correlationId
) {
}
