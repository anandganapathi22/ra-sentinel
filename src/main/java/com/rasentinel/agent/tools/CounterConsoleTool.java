package com.rasentinel.agent.tools;

import com.rasentinel.agent.tools.records.CounterConsoleState;

public interface CounterConsoleTool {
    CounterConsoleState getCounterState(String raId);
}
