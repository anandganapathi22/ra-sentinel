package com.rasentinel.agent.operations.core;

import com.rasentinel.agent.operations.api.AgentCaseRequest;
import com.rasentinel.agent.operations.api.OperationsAgentReport;
import com.rasentinel.agent.tools.KeyspaceTool;
import com.rasentinel.agent.tools.S3DocumentTool;
import com.rasentinel.agent.tools.TasTool;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ComplianceAuditAgent {
    private final TasTool tasTool;
    private final S3DocumentTool s3DocumentTool;
    private final KeyspaceTool keyspaceTool;
    private final AgentReportFactory reports;

    public ComplianceAuditAgent(TasTool tasTool, S3DocumentTool s3DocumentTool, KeyspaceTool keyspaceTool, AgentReportFactory reports) {
        this.tasTool = tasTool;
        this.s3DocumentTool = s3DocumentTool;
        this.keyspaceTool = keyspaceTool;
        this.reports = reports;
    }

    public OperationsAgentReport audit(AgentCaseRequest request) {
        var tas = tasTool.getAgreementStatus(request.raId());
        var s3 = s3DocumentTool.getSignedPdfStatus(request.raId());
        var keyspace = keyspaceTool.getSubmissionRecord(request.raId());
        var evidence = new ArrayList<String>();
        evidence.add("TAS state is " + tas.state());
        evidence.add("S3 signed PDF present: " + s3.present());
        evidence.add("S3 object key is " + s3.objectKey());
        evidence.add("Keyspace audit record present: " + keyspace.present());

        if ("SIGNED".equalsIgnoreCase(tas.state()) && !s3.present()) {
            return reports.report(
                    "Compliance & Audit Agent",
                    "RA " + request.raId(),
                    "High",
                    "Signature is present but archived PDF is missing.",
                    evidence,
                    List.of(),
                    "Create an audit ticket with TAS agreement ID and S3 lookup evidence.",
                    true,
                    List.of("OPEN_AUDIT_TICKET")
            );
        }

        if (s3.present() && !keyspace.present()) {
            return reports.report(
                    "Compliance & Audit Agent",
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
                "Compliance & Audit Agent",
                "RA " + request.raId(),
                "Low",
                "No compliance gap detected for this RA.",
                evidence,
                List.of(),
                "No audit action recommended.",
                false,
                List.of("NO_ACTION")
        );
    }
}
