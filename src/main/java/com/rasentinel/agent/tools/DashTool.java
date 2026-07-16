package com.rasentinel.agent.tools;

import com.rasentinel.agent.tools.records.DashCounterState;

public interface DashTool {
    DashCounterState getCounterState(String raId);
}
