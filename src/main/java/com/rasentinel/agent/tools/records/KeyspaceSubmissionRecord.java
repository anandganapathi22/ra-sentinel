package com.rasentinel.agent.tools.records;

import java.time.Instant;

public record KeyspaceSubmissionRecord(
        String raId,
        boolean present,
        String status,
        Instant updatedAt
) {
}
