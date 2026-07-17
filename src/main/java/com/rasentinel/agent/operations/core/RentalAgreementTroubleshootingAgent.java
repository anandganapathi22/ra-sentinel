package com.rasentinel.agent.operations.core;

import com.rasentinel.agent.ai.AiAssessmentRequest;
import com.rasentinel.agent.ai.AiReasoningClient;
import com.rasentinel.agent.operations.api.AgentCaseRequest;
import com.rasentinel.agent.operations.api.OperationsAgentReport;
import com.rasentinel.agent.tools.CorrelationTool;
import com.rasentinel.agent.tools.DashTool;
import com.rasentinel.agent.tools.EraTool;
import com.rasentinel.agent.tools.KeyspaceTool;
import com.rasentinel.agent.tools.RmsTool;
import com.rasentinel.agent.tools.S3DocumentTool;
import com.rasentinel.agent.tools.StlTool;
import com.rasentinel.agent.tools.TasTool;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

@Service
public class RentalAgreementTroubleshootingAgent {
    private static final String AGENT_NAME = "Rental Agreement Troubleshooting Agent";
    private static final String TASK_INSTRUCTION = "Determine whether the customer's rental agreement completed "
            + "successfully end to end across eRA, RMS, Dash, STL, TAS, S3, and Keyspace, and if not, identify the "
            + "single system where the transaction broke down.";

    private final EraTool eraTool;
    private final RmsTool rmsTool;
    private final DashTool dashTool;
    private final StlTool stlTool;
    private final TasTool tasTool;
    private final S3DocumentTool s3DocumentTool;
    private final KeyspaceTool keyspaceTool;
    private final CorrelationTool correlationTool;
    private final AgentReportFactory reports;
    private final AiReasoningClient aiReasoningClient;

    public RentalAgreementTroubleshootingAgent(
            EraTool eraTool,
            RmsTool rmsTool,
            DashTool dashTool,
            StlTool stlTool,
            TasTool tasTool,
            S3DocumentTool s3DocumentTool,
            KeyspaceTool keyspaceTool,
            CorrelationTool correlationTool,
            AgentReportFactory reports,
            AiReasoningClient aiReasoningClient
    ) {
        this.eraTool = eraTool;
        this.rmsTool = rmsTool;
        this.dashTool = dashTool;
        this.stlTool = stlTool;
        this.tasTool = tasTool;
        this.s3DocumentTool = s3DocumentTool;
        this.keyspaceTool = keyspaceTool;
        this.correlationTool = correlationTool;
        this.reports = reports;
        this.aiReasoningClient = aiReasoningClient;
    }

    public OperationsAgentReport investigate(AgentCaseRequest request) {
        var raId = request.raId().trim();
        var era = eraTool.getSessionStatus(raId);
        var rms = rmsTool.getRentalAgreement(raId);
        var dash = dashTool.getCounterState(raId);
        var stl = stlTool.getSubmissionMetadata(raId);
        var tas = tasTool.getAgreementStatus(raId);
        var s3 = s3DocumentTool.getSignedPdfStatus(raId);
        var keyspace = keyspaceTool.getSubmissionRecord(raId);
        var events = correlationTool.getEvents(raId);
        var timeline = ReportSupport.timeline(events);

        var evidence = new ArrayList<String>();
        evidence.add("eRA status is " + era.status() + " at step " + era.lastStep());
        evidence.add("RMS status is " + rms.status());
        evidence.add("Dash state is " + dash.state());
        evidence.add("STL submit status is " + stl.submitStatus() + " with correlation ID " + stl.correlationId());
        evidence.add("TAS state is " + tas.state());
        evidence.add("S3 PDF present: " + s3.present());
        evidence.add("Keyspace record present: " + keyspace.present());

        Supplier<OperationsAgentReport> deterministic = () -> {
            if ("SIGNED".equalsIgnoreCase(era.status())
                    && s3.present()
                    && "SUBMITTED".equalsIgnoreCase(stl.submitStatus())
                    && "API_TIMEOUT".equalsIgnoreCase(tas.state())) {
                return reports.report(
                        AGENT_NAME,
                        "RA " + raId,
                        "High",
                        "Customer signed successfully, PDF exists in S3, STL submit succeeded, and TAS API timeout occurred.",
                        evidence,
                        timeline,
                        "Resubmit the transaction to TAS after human approval and attach the correlation timeline.",
                        true,
                        ReportSupport.APPROVAL_GATED_OPERATIONS
                );
            }

            return reports.report(
                    AGENT_NAME,
                    "RA " + raId,
                    "Medium",
                    "No single cross-system failure was confirmed.",
                    evidence,
                    timeline,
                    "Open a support case with the attached cross-system evidence.",
                    true,
                    ReportSupport.APPROVAL_GATED_OPERATIONS
            );
        };

        var vocabulary = ReportSupport.APPROVAL_GATED_OPERATIONS;
        var context = Map.<String, Object>of("era", era, "rms", rms, "dash", dash, "stl", stl, "tas", tas, "s3", s3, "keyspace", keyspace);
        var aiRequest = new AiAssessmentRequest(AGENT_NAME, "RA " + raId, TASK_INSTRUCTION, evidence, context, vocabulary);

        return aiReasoningClient.assess(aiRequest)
                .map(assessment -> {
                    var allowedActions = ReportSupport.clampActions(assessment.allowedActions(), vocabulary);
                    return reports.report(
                            AGENT_NAME,
                            "RA " + raId,
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
