package com.rasentinel.agent.tools.records;

import java.time.Instant;

public record CorrelationEvent(
        String source,
        String correlationId,
        String message,
        Instant occurredAt
) {
}
