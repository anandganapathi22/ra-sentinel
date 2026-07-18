package com.rasentinel.agent.operations.core;

import com.rasentinel.agent.ai.AiAssessmentRequest;
import com.rasentinel.agent.ai.AiReasoningClient;
import com.rasentinel.agent.operations.api.AgentCaseRequest;
import com.rasentinel.agent.operations.api.OperationsAgentReport;
import com.rasentinel.agent.tools.CorrelationTool;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

@Service
public class EndToEndTransactionInvestigationAgent {
    private static final String AGENT_NAME = "End-to-End Transaction Investigation Agent";
    private static final String TASK_INSTRUCTION = "Build a root-cause assessment from the cross-system "
            + "correlation timeline for this RA, calling out where in the signing portal/submission "
            + "gateway/fleet ledger/counter console/contract vault/S3 chain the transaction broke down, if "
            + "anywhere.";

    private final CorrelationTool correlationTool;
    private final AgentReportFactory reports;
    private final AiReasoningClient aiReasoningClient;

    public EndToEndTransactionInvestigationAgent(
            CorrelationTool correlationTool, AgentReportFactory reports, AiReasoningClient aiReasoningClient) {
        this.correlationTool = correlationTool;
        this.reports = reports;
        this.aiReasoningClient = aiReasoningClient;
    }

    public OperationsAgentReport trace(AgentCaseRequest request) {
        var events = correlationTool.getEvents(request.raId());
        var timeline = ReportSupport.timeline(events);
        var vaultTimeout = events.stream().anyMatch(event -> event.message().toLowerCase().contains("timeout"));
        var evidence = timeline.stream()
                .map(event -> event.system() + ": " + event.event())
                .toList();

        Supplier<OperationsAgentReport> deterministic = () -> reports.report(
                AGENT_NAME,
                "RA " + request.raId(),
                vaultTimeout ? "High" : "Medium",
                vaultTimeout ? "Transaction reached the contract vault but timed out during downstream processing." : "Transaction timeline collected.",
                evidence,
                timeline,
                vaultTimeout ? "Resubmit or retry contract vault processing after approval." : "Review the timeline and route to the owning system.",
                true,
                vaultTimeout ? ReportSupport.APPROVAL_GATED_OPERATIONS : List.of("OPEN_INCIDENT")
        );

        var context = Map.<String, Object>of("events", events);
        var aiRequest = new AiAssessmentRequest(
                AGENT_NAME, "RA " + request.raId(), TASK_INSTRUCTION, evidence, context, ReportSupport.APPROVAL_GATED_OPERATIONS);

        return aiReasoningClient.assess(aiRequest)
                .map(assessment -> {
                    var allowedActions = ReportSupport.clampActions(assessment.allowedActions(), ReportSupport.APPROVAL_GATED_OPERATIONS);
                    return reports.report(
                            AGENT_NAME,
                            "RA " + request.raId(),
                            assessment.severity(),
                            assessment.rootCause(),
                            evidence,
                            timeline,
                            assessment.recommendedAction(),
                            ReportSupport.requiresApproval(allowedActions),
                            allowedActions
                    );
                })
                .orElseGet(deterministic);
    }
}
