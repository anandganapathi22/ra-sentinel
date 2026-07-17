package com.rasentinel.agent.operations.core;

import com.rasentinel.agent.ai.AiAssessmentRequest;
import com.rasentinel.agent.ai.AiReasoningClient;
import com.rasentinel.agent.operations.api.AgentCaseRequest;
import com.rasentinel.agent.operations.api.OperationsAgentReport;
import com.rasentinel.agent.tools.EraTool;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

@Service
public class CustomerCompletionRecoveryAgent {
    private static final String AGENT_NAME = "Customer Completion Recovery Agent";
    private static final String TASK_INSTRUCTION = "Determine whether the customer abandoned the eRA signing "
            + "flow and, if so, recommend how to recover completion.";
    private static final List<String> VOCABULARY = List.of(
            "RESEND_LINK_AFTER_APPROVAL", "REGENERATE_LINK_AFTER_APPROVAL", "SEND_REMINDER_AFTER_APPROVAL", "NO_ACTION");

    private final EraTool eraTool;
    private final AgentReportFactory reports;
    private final AiReasoningClient aiReasoningClient;

    public CustomerCompletionRecoveryAgent(EraTool eraTool, AgentReportFactory reports, AiReasoningClient aiReasoningClient) {
        this.eraTool = eraTool;
        this.reports = reports;
        this.aiReasoningClient = aiReasoningClient;
    }

    public OperationsAgentReport investigate(AgentCaseRequest request) {
        var era = eraTool.getSessionStatus(request.raId());
        var evidence = new ArrayList<String>();
        evidence.add("Started at " + era.startedAt());
        evidence.add("Last activity at " + era.lastActivityAt());
        evidence.add("Status is " + era.status());
        evidence.add("Last step is " + era.lastStep());

        Supplier<OperationsAgentReport> deterministic = () -> {
            if ("ABANDONED".equalsIgnoreCase(era.status())) {
                return reports.report(
                        AGENT_NAME,
                        "RA " + request.raId(),
                        "Medium",
                        "Customer abandoned eRA at " + era.lastStep() + " step.",
                        evidence,
                        List.of(),
                        "Regenerate or resend the existing signing link and send a reminder after approval.",
                        true,
                        List.of("RESEND_LINK_AFTER_APPROVAL", "REGENERATE_LINK_AFTER_APPROVAL", "SEND_REMINDER_AFTER_APPROVAL")
                );
            }

            return reports.report(
                    AGENT_NAME,
                    "RA " + request.raId(),
                    "Low",
                    "Customer completion recovery is not required.",
                    evidence,
                    List.of(),
                    "No recovery action recommended.",
                    false,
                    List.of("NO_ACTION")
            );
        };

        var context = Map.<String, Object>of("era", era);
        var aiRequest = new AiAssessmentRequest(
                AGENT_NAME, "RA " + request.raId(), TASK_INSTRUCTION, evidence, context, VOCABULARY);

        return aiReasoningClient.assess(aiRequest)
                .map(assessment -> {
                    var allowedActions = ReportSupport.clampActions(assessment.allowedActions(), VOCABULARY);
                    return reports.report(
                            AGENT_NAME,
                            "RA " + request.raId(),
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
