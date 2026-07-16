package com.rasentinel.agent.tools;

import com.rasentinel.agent.tools.records.EraSessionStatus;

public interface EraTool {
    EraSessionStatus getSessionStatus(String raId);
}
