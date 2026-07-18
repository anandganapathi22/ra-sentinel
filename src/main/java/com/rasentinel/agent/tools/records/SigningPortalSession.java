package com.rasentinel.agent.tools.records;

import java.time.Instant;

public record SigningPortalSession(
        String raId,
        String status,
        Instant startedAt,
        Instant lastActivityAt,
        String lastStep,
        boolean licenseScanPresent,
        boolean customerEligible
) {
}
