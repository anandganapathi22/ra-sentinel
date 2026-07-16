package com.rasentinel.agent.tools;

import com.rasentinel.agent.tools.records.S3SignedPdfStatus;

public interface S3DocumentTool {
    S3SignedPdfStatus getSignedPdfStatus(String raId);
}
