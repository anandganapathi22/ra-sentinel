package com.rasentinel.agent.tools.records;

public record FleetLedgerAgreement(
        String raId,
        boolean exists,
        String status,
        String customerEmail,
        String language
) {
}
