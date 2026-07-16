package com.rasentinel.agent.operations.core;

import com.rasentinel.agent.operations.api.OperationsAgentReport;
import com.rasentinel.agent.operations.api.TimelineEvent;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AgentReportFactory {
    private static final List<String> BLOCKED_LEGAL_ACTIONS = List.of(
            "MODIFY_LEGAL_TEXT",
            "CHANGE_CHARGES",
            "SIGN_FOR_CUSTOMER",
            "SUBMIT_LEGAL_AGREEMENT_AUTONOMOUSLY"
    );

    private final Clock clock;

    public AgentReportFactory(Clock clock) {
        this.clock = clock;
    }

    public OperationsAgentReport report(
            String agentName,
            String subject,
            String severity,
            String rootCause,
            List<String> evidence,
            List<TimelineEvent> timeline,
            String recommendedAction,
            boolean requiresHumanApproval,
            List<String> allowedActions
    ) {
        return new OperationsAgentReport(
                UUID.randomUUID(),
                agentName,
                subject,
                severity,
                rootCause,
                evidence,
                timeline,
                recommendedAction,
                requiresHumanApproval,
                allowedActions,
                BLOCKED_LEGAL_ACTIONS,
                clock.instant()
        );
    }
}
