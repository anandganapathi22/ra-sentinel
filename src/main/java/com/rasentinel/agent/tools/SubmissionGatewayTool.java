package com.rasentinel.agent.tools;

import com.rasentinel.agent.tools.records.SubmissionGatewayMetadata;

public interface SubmissionGatewayTool {
    SubmissionGatewayMetadata getSubmissionMetadata(String raId);
}
