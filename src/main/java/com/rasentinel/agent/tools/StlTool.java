package com.rasentinel.agent.tools;

import com.rasentinel.agent.tools.records.StlSubmissionMetadata;

public interface StlTool {
    StlSubmissionMetadata getSubmissionMetadata(String raId);
}
