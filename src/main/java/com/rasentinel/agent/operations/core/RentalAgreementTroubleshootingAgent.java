package com.rasentinel.agent.operations.core;

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
import org.springframework.stereotype.Service;

@Service
public class RentalAgreementTroubleshootingAgent {
    private final EraTool eraTool;
    private final RmsTool rmsTool;
    private final DashTool dashTool;
    private final StlTool stlTool;
    private final TasTool tasTool;
    private final S3DocumentTool s3DocumentTool;
    private final KeyspaceTool keyspaceTool;
    private final CorrelationTool correlationTool;
    private final AgentReportFactory reports;

    public RentalAgreementTroubleshootingAgent(
            EraTool eraTool,
            RmsTool rmsTool,
            DashTool dashTool,
            StlTool stlTool,
            TasTool tasTool,
            S3DocumentTool s3DocumentTool,
            KeyspaceTool keyspaceTool,
            CorrelationTool correlationTool,
            AgentReportFactory reports
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

        var evidence = new ArrayList<String>();
        evidence.add("eRA status is " + era.status() + " at step " + era.lastStep());
        evidence.add("RMS status is " + rms.status());
        evidence.add("Dash state is " + dash.state());
        evidence.add("STL submit status is " + stl.submitStatus() + " with correlation ID " + stl.correlationId());
        evidence.add("TAS state is " + tas.state());
        evidence.add("S3 PDF present: " + s3.present());
        evidence.add("Keyspace record present: " + keyspace.present());

        if ("SIGNED".equalsIgnoreCase(era.status())
                && s3.present()
                && "SUBMITTED".equalsIgnoreCase(stl.submitStatus())
                && "API_TIMEOUT".equalsIgnoreCase(tas.state())) {
            return reports.report(
                    "Rental Agreement Troubleshooting Agent",
                    "RA " + raId,
                    "High",
                    "Customer signed successfully, PDF exists in S3, STL submit succeeded, and TAS API timeout occurred.",
                    evidence,
                    ReportSupport.timeline(events),
                    "Resubmit the transaction to TAS after human approval and attach the correlation timeline.",
                    true,
                    ReportSupport.APPROVAL_GATED_OPERATIONS
            );
        }

        return reports.report(
                "Rental Agreement Troubleshooting Agent",
                "RA " + raId,
                "Medium",
                "No single cross-system failure was confirmed.",
                evidence,
                ReportSupport.timeline(events),
                "Open a support case with the attached cross-system evidence.",
                true,
                ReportSupport.APPROVAL_GATED_OPERATIONS
        );
    }
}
