package com.rasentinel.agent.tools.records;

public record S3SignedPdfStatus(
        String raId,
        boolean present,
        String bucket,
        String objectKey
) {
}
