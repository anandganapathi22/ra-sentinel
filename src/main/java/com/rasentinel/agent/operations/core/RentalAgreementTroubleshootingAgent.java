package com.rasentinel.agent.operations.core;

import com.rasentinel.agent.ai.AiAssessmentRequest;
import com.rasentinel.agent.ai.AiReasoningClient;
import com.rasentinel.agent.operations.api.AgentCaseRequest;
import com.rasentinel.agent.operations.api.OperationsAgentReport;
import com.rasentinel.agent.tools.CorrelationTool;
import com.rasentinel.agent.tools.CounterConsoleTool;
import com.rasentinel.agent.tools.ContractVaultTool;
import com.rasentinel.agent.tools.FleetLedgerTool;
import com.rasentinel.agent.tools.KeyspaceTool;
import com.rasentinel.agent.tools.S3DocumentTool;
import com.rasentinel.agent.tools.SigningPortalTool;
import com.rasentinel.agent.tools.SubmissionGatewayTool;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

@Service
public class RentalAgreementTroubleshootingAgent {
    private static final String AGENT_NAME = "Rental Agreement Troubleshooting Agent";
    private static final String TASK_INSTRUCTION = "Determine whether the customer's rental agreement completed "
            + "successfully end to end across the signing portal, fleet ledger, counter console, submission "
            + "gateway, contract vault, S3, and Keyspace, and if not, identify the single system where the "
            + "transaction broke down.";

    private final SigningPortalTool signingPortalTool;
    private final FleetLedgerTool fleetLedgerTool;
    private final CounterConsoleTool counterConsoleTool;
    private final SubmissionGatewayTool submissionGatewayTool;
    private final ContractVaultTool contractVaultTool;
    private final S3DocumentTool s3DocumentTool;
    private final KeyspaceTool keyspaceTool;
    private final CorrelationTool correlationTool;
    private final AgentReportFactory reports;
    private final AiReasoningClient aiReasoningClient;

    public RentalAgreementTroubleshootingAgent(
            SigningPortalTool signingPortalTool,
            FleetLedgerTool fleetLedgerTool,
            CounterConsoleTool counterConsoleTool,
            SubmissionGatewayTool submissionGatewayTool,
            ContractVaultTool contractVaultTool,
            S3DocumentTool s3DocumentTool,
            KeyspaceTool keyspaceTool,
            CorrelationTool correlationTool,
            AgentReportFactory reports,
            AiReasoningClient aiReasoningClient
    ) {
        this.signingPortalTool = signingPortalTool;
        this.fleetLedgerTool = fleetLedgerTool;
        this.counterConsoleTool = counterConsoleTool;
        this.submissionGatewayTool = submissionGatewayTool;
        this.contractVaultTool = contractVaultTool;
        this.s3DocumentTool = s3DocumentTool;
        this.keyspaceTool = keyspaceTool;
        this.correlationTool = correlationTool;
        this.reports = reports;
        this.aiReasoningClient = aiReasoningClient;
    }

    public OperationsAgentReport investigate(AgentCaseRequest request) {
        var raId = request.raId().trim();
        var portal = signingPortalTool.getSessionStatus(raId);
        var fleet = fleetLedgerTool.getRentalAgreement(raId);
        var console = counterConsoleTool.getCounterState(raId);
        var gateway = submissionGatewayTool.getSubmissionMetadata(raId);
        var vault = contractVaultTool.getAgreementStatus(raId);
        var s3 = s3DocumentTool.getSignedPdfStatus(raId);
        var keyspace = keyspaceTool.getSubmissionRecord(raId);
        var events = correlationTool.getEvents(raId);
        var timeline = ReportSupport.timeline(events);

        var evidence = new ArrayList<String>();
        evidence.add("Signing portal status is " + portal.status() + " at step " + portal.lastStep());
        evidence.add("Fleet ledger status is " + fleet.status());
        evidence.add("Counter console state is " + console.state());
        evidence.add("Submission gateway status is " + gateway.submitStatus() + " with correlation ID " + gateway.correlationId());
        evidence.add("Contract vault state is " + vault.state());
        evidence.add("S3 PDF present: " + s3.present());
        evidence.add("Keyspace record present: " + keyspace.present());

        Supplier<OperationsAgentReport> deterministic = () -> {
            if ("SIGNED".equalsIgnoreCase(portal.status())
                    && s3.present()
                    && "SUBMITTED".equalsIgnoreCase(gateway.submitStatus())
                    && "API_TIMEOUT".equalsIgnoreCase(vault.state())) {
                return reports.report(
                        AGENT_NAME,
                        "RA " + raId,
                        "High",
                        "Customer signed successfully, PDF exists in S3, submission gateway succeeded, and contract vault API timeout occurred.",
                        evidence,
                        timeline,
                        "Resubmit the transaction to the contract vault after human approval and attach the correlation timeline.",
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
        var context = Map.<String, Object>of(
                "portal", portal, "fleet", fleet, "console", console, "gateway", gateway, "vault", vault, "s3", s3, "keyspace", keyspace);
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
