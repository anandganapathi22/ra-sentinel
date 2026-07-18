package com.rasentinel.agent.tools.records;

import java.util.List;
import java.util.Map;

public record SystemHealthSnapshot(
        String location,
        String fleetStatus,
        String consoleStatus,
        String vaultStatus,
        int vaultLatencyMs,
        String databaseStatus,
        int queueBacklog,
        Map<String, Integer> recentHttpFailures,
        List<String> affectedLocations
) {
}
