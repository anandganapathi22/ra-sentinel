package com.rasentinel.agent.tools.records;

public record StlSubmissionMetadata(
        String raId,
        String submitStatus,
        String email,
        String language,
        String correlationId
) {
}
