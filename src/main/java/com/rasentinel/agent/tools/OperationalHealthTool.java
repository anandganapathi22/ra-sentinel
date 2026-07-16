package com.rasentinel.agent.tools;

import com.rasentinel.agent.tools.records.SystemHealthSnapshot;

public interface OperationalHealthTool {
    SystemHealthSnapshot getHealth(String location);
}
