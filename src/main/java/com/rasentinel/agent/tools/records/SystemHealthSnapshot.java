package com.rasentinel.agent.tools.records;

import java.util.List;
import java.util.Map;

public record SystemHealthSnapshot(
        String location,
        String rmsStatus,
        String dashStatus,
        String tasStatus,
        int tasLatencyMs,
        String databaseStatus,
        int queueBacklog,
        Map<String, Integer> recentHttpFailures,
        List<String> affectedLocations
) {
}
