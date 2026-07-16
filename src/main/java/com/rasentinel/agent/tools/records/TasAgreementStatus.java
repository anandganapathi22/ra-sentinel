package com.rasentinel.agent.tools.records;

import java.time.Instant;

public record TasAgreementStatus(
        String raId,
        String state,
        String agreementId,
        Instant hashExpiresAt
) {
}
