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
public class SystemHealthMonitoringAgent {
    private static final String AGENT_NAME = "TAS/RMS Health Monitoring Agent";
    private static final String TASK_INSTRUCTION = "Assess whether current TAS latency and queue backlog indicate "
            + "counter delays are likely for this location, and recommend a mitigation if so.";
    private static final List<String> VOCABULARY = List.of(
            "ALERT_OPERATIONS", "FAIL_OVER_ENDPOINT_AFTER_APPROVAL", "CONTINUE_MONITORING");

    private final OperationalHealthTool healthTool;
    private final AgentReportFactory reports;
    private final AiReasoningClient aiReasoningClient;

    public SystemHealthMonitoringAgent(OperationalHealthTool healthTool, AgentReportFactory reports, AiReasoningClient aiReasoningClient) {
        this.healthTool = healthTool;
        this.reports = reports;
        this.aiReasoningClient = aiReasoningClient;
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

        Supplier<OperationsAgentReport> deterministic = () -> {
            if (health.tasLatencyMs() > 5000 || health.queueBacklog() > 200) {
                return reports.report(
                        AGENT_NAME,
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
                    AGENT_NAME,
                    location,
                    "Normal",
                    "RMS, Dash, TAS, database, and queue health are within expected thresholds.",
                    evidence,
                    List.of(),
                    "Continue scheduled monitoring.",
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
}
