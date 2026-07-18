package com.rasentinel.agent.operations.core;

import com.rasentinel.agent.ai.AiAssessmentRequest;
import com.rasentinel.agent.ai.AiReasoningClient;
import com.rasentinel.agent.operations.api.AgentCaseRequest;
import com.rasentinel.agent.operations.api.OperationsAgentReport;
import com.rasentinel.agent.tools.ContractVaultTool;
import com.rasentinel.agent.tools.KeyspaceTool;
import com.rasentinel.agent.tools.S3DocumentTool;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

@Service
public class ComplianceAuditAgent {
    private static final String AGENT_NAME = "Compliance & Audit Agent";
    private static final String TASK_INSTRUCTION = "Check whether a signed agreement is missing its archived PDF "
            + "or its audit metadata, and if so, what compliance gap exists.";
    private static final List<String> VOCABULARY = List.of("OPEN_AUDIT_TICKET", "RETRY_SYNC_AFTER_APPROVAL", "NO_ACTION");

    private final ContractVaultTool contractVaultTool;
    private final S3DocumentTool s3DocumentTool;
    private final KeyspaceTool keyspaceTool;
    private final AgentReportFactory reports;
    private final AiReasoningClient aiReasoningClient;

    public ComplianceAuditAgent(
            ContractVaultTool contractVaultTool,
            S3DocumentTool s3DocumentTool,
            KeyspaceTool keyspaceTool,
            AgentReportFactory reports,
            AiReasoningClient aiReasoningClient
    ) {
        this.contractVaultTool = contractVaultTool;
        this.s3DocumentTool = s3DocumentTool;
        this.keyspaceTool = keyspaceTool;
        this.reports = reports;
        this.aiReasoningClient = aiReasoningClient;
    }

    public OperationsAgentReport audit(AgentCaseRequest request) {
        var vault = contractVaultTool.getAgreementStatus(request.raId());
        var s3 = s3DocumentTool.getSignedPdfStatus(request.raId());
        var keyspace = keyspaceTool.getSubmissionRecord(request.raId());
        var evidence = new ArrayList<String>();
        evidence.add("Contract vault state is " + vault.state());
        evidence.add("S3 signed PDF present: " + s3.present());
        evidence.add("S3 object key is " + s3.objectKey());
        evidence.add("Keyspace audit record present: " + keyspace.present());

        Supplier<OperationsAgentReport> deterministic = () -> {
            if ("SIGNED".equalsIgnoreCase(vault.state()) && !s3.present()) {
                return reports.report(
                        AGENT_NAME,
                        "RA " + request.raId(),
                        "High",
                        "Signature is present but archived PDF is missing.",
                        evidence,
                        List.of(),
                        "Create an audit ticket with contract vault agreement ID and S3 lookup evidence.",
                        true,
                        List.of("OPEN_AUDIT_TICKET")
                );
            }

            if (s3.present() && !keyspace.present()) {
                return reports.report(
                        AGENT_NAME,
                        "RA " + request.raId(),
                        "High",
                        "Signed PDF exists but audit metadata is missing.",
                        evidence,
                        List.of(),
                        "Create an audit ticket and retry metadata sync after approval.",
                        true,
                        List.of("OPEN_AUDIT_TICKET", "RETRY_SYNC_AFTER_APPROVAL")
                );
            }

            return reports.report(
                    AGENT_NAME,
                    "RA " + request.raId(),
                    "Low",
                    "No compliance gap detected for this RA.",
                    evidence,
                    List.of(),
                    "No audit action recommended.",
                    false,
                    List.of("NO_ACTION")
            );
        };

        var context = Map.<String, Object>of("vault", vault, "s3", s3, "keyspace", keyspace);
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
