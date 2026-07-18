package com.rasentinel.agent.operations.core;

import com.rasentinel.agent.ai.AiAssessmentRequest;
import com.rasentinel.agent.ai.AiReasoningClient;
import com.rasentinel.agent.operations.api.AgentCaseRequest;
import com.rasentinel.agent.operations.api.OperationsAgentReport;
import com.rasentinel.agent.tools.SigningPortalTool;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

@Service
public class CustomerCompletionRecoveryAgent {
    private static final String AGENT_NAME = "Customer Completion Recovery Agent";
    private static final String TASK_INSTRUCTION = "Determine whether the customer abandoned the signing portal "
            + "flow and, if so, recommend how to recover completion.";
    private static final List<String> VOCABULARY = List.of(
            "RESEND_LINK_AFTER_APPROVAL", "REGENERATE_LINK_AFTER_APPROVAL", "SEND_REMINDER_AFTER_APPROVAL", "NO_ACTION");

    private final SigningPortalTool signingPortalTool;
    private final AgentReportFactory reports;
    private final AiReasoningClient aiReasoningClient;

    public CustomerCompletionRecoveryAgent(SigningPortalTool signingPortalTool, AgentReportFactory reports, AiReasoningClient aiReasoningClient) {
        this.signingPortalTool = signingPortalTool;
        this.reports = reports;
        this.aiReasoningClient = aiReasoningClient;
    }

    public OperationsAgentReport investigate(AgentCaseRequest request) {
        var portal = signingPortalTool.getSessionStatus(request.raId());
        var evidence = new ArrayList<String>();
        evidence.add("Started at " + portal.startedAt());
        evidence.add("Last activity at " + portal.lastActivityAt());
        evidence.add("Status is " + portal.status());
        evidence.add("Last step is " + portal.lastStep());

        Supplier<OperationsAgentReport> deterministic = () -> {
            if ("ABANDONED".equalsIgnoreCase(portal.status())) {
                return reports.report(
                        AGENT_NAME,
                        "RA " + request.raId(),
                        "Medium",
                        "Customer abandoned the signing portal at " + portal.lastStep() + " step.",
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

        var context = Map.<String, Object>of("portal", portal);
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
