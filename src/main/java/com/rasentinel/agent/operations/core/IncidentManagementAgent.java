package com.rasentinel.agent.operations.core;

import com.rasentinel.agent.ai.AiAssessmentRequest;
import com.rasentinel.agent.ai.AiReasoningClient;
import com.rasentinel.agent.operations.api.AgentCaseRequest;
import com.rasentinel.agent.operations.api.OperationsAgentReport;
import com.rasentinel.agent.tools.OperationalHealthTool;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

@Service
public class IncidentManagementAgent {
    private static final String AGENT_NAME = "Incident Management Agent";
    private static final String TASK_INSTRUCTION = "Determine whether there is an active platform incident "
            + "affecting this location based on fleet ledger/counter console/contract vault/database/queue "
            + "health, and if so, its severity and recommended mitigation.";
    private static final List<String> VOCABULARY = concat(ReportSupport.APPROVAL_GATED_OPERATIONS, "CONTINUE_MONITORING");

    private final OperationalHealthTool healthTool;
    private final AgentReportFactory reports;
    private final AiReasoningClient aiReasoningClient;

    public IncidentManagementAgent(OperationalHealthTool healthTool, AgentReportFactory reports, AiReasoningClient aiReasoningClient) {
        this.healthTool = healthTool;
        this.reports = reports;
        this.aiReasoningClient = aiReasoningClient;
    }

    public OperationsAgentReport investigate(AgentCaseRequest request) {
        var location = request.location() == null || request.location().isBlank() ? "GLOBAL" : request.location();
        var health = healthTool.getHealth(location);
        var evidence = new ArrayList<String>();
        evidence.add("Fleet ledger status is " + health.fleetStatus());
        evidence.add("Counter console status is " + health.consoleStatus());
        evidence.add("Contract vault status is " + health.vaultStatus());
        evidence.add("Contract vault latency is " + health.vaultLatencyMs() + "ms");
        evidence.add("Database status is " + health.databaseStatus());
        evidence.add("Queue backlog is " + health.queueBacklog());
        evidence.add("Recent HTTP failures: " + health.recentHttpFailures());
        evidence.add("Affected locations: " + health.affectedLocations());

        Supplier<OperationsAgentReport> deterministic = () -> {
            if ("UNAVAILABLE".equalsIgnoreCase(health.fleetStatus())) {
                return reports.report(
                        AGENT_NAME,
                        location,
                        "High",
                        "Fleet ledger endpoint unavailable.",
                        evidence,
                        List.of(),
                        "Fail over to the secondary fleet ledger endpoint after incident commander approval.",
                        true,
                        ReportSupport.APPROVAL_GATED_OPERATIONS
                );
            }

            return reports.report(
                    AGENT_NAME,
                    location,
                    "Low",
                    "No active platform incident detected.",
                    evidence,
                    List.of(),
                    "Continue monitoring.",
                    false,
                    List.of("CONTINUE_MONITORING")
            );
        };

        var context = Map.<String, Object>of("health", health);
        var aiRequest = new AiAssessmentRequest(AGENT_NAME, location, TASK_INSTRUCTION, evidence, context, VOCABULARY);

        return aiReasoningClient.assess(aiRequest)
                .map(assessment -> {
                    var allowedActions = ReportSupport.clampActions(assessment.allowedActions(), VOCABULARY);
                    return reports.report(
                            AGENT_NAME,
                            location,
                            assessment.severity(),
                            assessment.rootCause(),
                            evidence,
                            List.of(),
                            assessment.recommendedAction(),
                            ReportSupport.requiresApproval(allowedActions),
                            allowedActions
                    );
                })
                .orElseGet(deterministic);
    }

    private static List<String> concat(List<String> base, String extra) {
        var combined = new ArrayList<>(base);
        combined.add(extra);
        return List.copyOf(combined);
    }
}
