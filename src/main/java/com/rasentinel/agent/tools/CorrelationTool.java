package com.rasentinel.agent.tools;

import com.rasentinel.agent.tools.records.CorrelationEvent;
import java.util.List;

public interface CorrelationTool {
    List<CorrelationEvent> getEvents(String raId);
}
