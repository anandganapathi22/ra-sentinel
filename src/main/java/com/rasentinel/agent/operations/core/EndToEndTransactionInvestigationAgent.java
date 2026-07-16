package com.rasentinel.agent.operations.core;

import com.rasentinel.agent.operations.api.AgentCaseRequest;
import com.rasentinel.agent.operations.api.OperationsAgentReport;
import com.rasentinel.agent.tools.CorrelationTool;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class EndToEndTransactionInvestigationAgent {
    private final CorrelationTool correlationTool;
    private final AgentReportFactory reports;

    public EndToEndTransactionInvestigationAgent(CorrelationTool correlationTool, AgentReportFactory reports) {
        this.correlationTool = correlationTool;
        this.reports = reports;
    }

    public OperationsAgentReport trace(AgentCaseRequest request) {
        var events = correlationTool.getEvents(request.raId());
        var timeline = ReportSupport.timeline(events);
        var tasTimeout = events.stream().anyMatch(event -> event.message().toLowerCase().contains("timeout"));
        var evidence = timeline.stream()
                .map(event -> event.system() + ": " + event.event())
                .toList();

        return reports.report(
                "End-to-End Transaction Investigation Agent",
                "RA " + request.raId(),
                tasTimeout ? "High" : "Medium",
                tasTimeout ? "Transaction reached TAS but timed out during downstream processing." : "Transaction timeline collected.",
                evidence,
                timeline,
                tasTimeout ? "Resubmit or retry TAS processing after approval." : "Review the timeline and route to the owning system.",
                true,
                tasTimeout ? ReportSupport.APPROVAL_GATED_OPERATIONS : List.of("OPEN_INCIDENT")
        );
    }
}
