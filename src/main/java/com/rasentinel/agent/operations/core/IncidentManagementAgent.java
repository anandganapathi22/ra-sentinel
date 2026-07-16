package com.rasentinel.agent.operations.core;

import com.rasentinel.agent.operations.api.AgentCaseRequest;
import com.rasentinel.agent.operations.api.OperationsAgentReport;
import com.rasentinel.agent.tools.OperationalHealthTool;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class IncidentManagementAgent {
    private final OperationalHealthTool healthTool;
    private final AgentReportFactory reports;

    public IncidentManagementAgent(OperationalHealthTool healthTool, AgentReportFactory reports) {
        this.healthTool = healthTool;
        this.reports = reports;
    }

    public OperationsAgentReport investigate(AgentCaseRequest request) {
        var location = request.location() == null || request.location().isBlank() ? "GLOBAL" : request.location();
        var health = healthTool.getHealth(location);
        var evidence = new ArrayList<String>();
        evidence.add("RMS status is " + health.rmsStatus());
        evidence.add("Dash status is " + health.dashStatus());
        evidence.add("TAS status is " + health.tasStatus());
        evidence.add("TAS latency is " + health.tasLatencyMs() + "ms");
        evidence.add("Database status is " + health.databaseStatus());
        evidence.add("Queue backlog is " + health.queueBacklog());
        evidence.add("Recent HTTP failures: " + health.recentHttpFailures());
        evidence.add("Affected locations: " + health.affectedLocations());

        if ("UNAVAILABLE".equalsIgnoreCase(health.rmsStatus())) {
            return reports.report(
                    "Incident Management Agent",
                    location,
                    "High",
                    "RMS endpoint unavailable.",
                    evidence,
                    List.of(),
                    "Fail over to the secondary RMS endpoint after incident commander approval.",
                    true,
                    ReportSupport.APPROVAL_GATED_OPERATIONS
            );
        }

        return reports.report(
                "Incident Management Agent",
                location,
                "Low",
                "No active platform incident detected.",
                evidence,
                List.of(),
                "Continue monitoring.",
                false,
                List.of("CONTINUE_MONITORING")
        );
    }
}
