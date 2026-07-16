package com.rasentinel.agent.tools;

import com.rasentinel.agent.tools.records.KeyspaceSubmissionRecord;

public interface KeyspaceTool {
    KeyspaceSubmissionRecord getSubmissionRecord(String raId);
}
