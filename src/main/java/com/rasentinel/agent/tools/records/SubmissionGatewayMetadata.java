package com.rasentinel.agent.tools.records;

public record SubmissionGatewayMetadata(
        String raId,
        String submitStatus,
        String email,
        String language,
        String correlationId
) {
}
