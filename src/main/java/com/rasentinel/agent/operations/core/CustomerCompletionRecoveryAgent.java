package com.rasentinel.agent.operations.core;

import com.rasentinel.agent.operations.api.AgentCaseRequest;
import com.rasentinel.agent.operations.api.OperationsAgentReport;
import com.rasentinel.agent.tools.EraTool;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CustomerCompletionRecoveryAgent {
    private final EraTool eraTool;
    private final AgentReportFactory reports;

    public CustomerCompletionRecoveryAgent(EraTool eraTool, AgentReportFactory reports) {
        this.eraTool = eraTool;
        this.reports = reports;
    }

    public OperationsAgentReport investigate(AgentCaseRequest request) {
        var era = eraTool.getSessionStatus(request.raId());
        var evidence = new ArrayList<String>();
        evidence.add("Started at " + era.startedAt());
        evidence.add("Last activity at " + era.lastActivityAt());
        evidence.add("Status is " + era.status());
        evidence.add("Last step is " + era.lastStep());

        if ("ABANDONED".equalsIgnoreCase(era.status())) {
            return reports.report(
                    "Customer Completion Recovery Agent",
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
                "Customer Completion Recovery Agent",
                "RA " + request.raId(),
                "Low",
                "Customer completion recovery is not required.",
                evidence,
                List.of(),
                "No recovery action recommended.",
                false,
                List.of("NO_ACTION")
        );
    }
}
