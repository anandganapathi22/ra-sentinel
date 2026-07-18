package com.rasentinel.agent.tools.records;

import java.time.Instant;

public record ContractVaultStatus(
        String raId,
        String state,
        String agreementId,
        Instant hashExpiresAt
) {
}
