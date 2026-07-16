package com.rasentinel.agent.tools.records;

public record RmsRentalAgreement(
        String raId,
        boolean exists,
        String status,
        String customerEmail,
        String language
) {
}
