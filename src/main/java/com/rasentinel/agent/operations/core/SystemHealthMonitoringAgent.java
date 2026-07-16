package com.rasentinel.agent.operations.core;

import com.rasentinel.agent.operations.api.AgentCaseRequest;
import com.rasentinel.agent.operations.api.OperationsAgentReport;
import com.rasentinel.agent.tools.OperationalHealthTool;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SystemHealthMonitoringAgent {
    private final OperationalHealthTool healthTool;
    private final AgentReportFactory reports;

    public SystemHealthMonitoringAgent(OperationalHealthTool healthTool, AgentReportFactory reports) {
        this.healthTool = healthTool;
        this.reports = reports;
    }

    public OperationsAgentReport check(AgentCaseRequest request) {
        var location = request.location() == null || request.location().isBlank() ? "GLOBAL" : request.location();
        var health = healthTool.getHealth(location);
        var evidence = new ArrayList<String>();
        evidence.add("RMS status is " + health.rmsStatus());
        evidence.add("Dash status is " + health.dashStatus());
        evidence.add("TAS status is " + health.tasStatus());
        evidence.add("TAS latency is " + health.tasLatencyMs() + "ms");
        evidence.add("Queue backlog is " + health.queueBacklog());

        if (health.tasLatencyMs() > 5000 || health.queueBacklog() > 200) {
            return reports.report(
                    "TAS/RMS Health Monitoring Agent",
                    location,
                    "Warning",
                    "Operational latency and backlog indicate counter delays are likely.",
                    evidence,
                    List.of(),
                    "Alert operations and prepare failover/runbook actions.",
                    true,
                    List.of("ALERT_OPERATIONS", "FAIL_OVER_ENDPOINT_AFTER_APPROVAL")
            );
        }

        return reports.report(
                "TAS/RMS Health Monitoring Agent",
                location,
                "Normal",
                "RMS, Dash, TAS, database, and queue health are within expected thresholds.",
                evidence,
                List.of(),
                "Continue scheduled monitoring.",
                false,
                List.of("CONTINUE_MONITORING")
        );
    }
}
