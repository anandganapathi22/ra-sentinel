package com.rasentinel.agent.operations.api;

import com.rasentinel.agent.operations.core.ComplianceAuditAgent;
import com.rasentinel.agent.operations.core.CounterSupportCopilot;
import com.rasentinel.agent.operations.core.CustomerCompletionRecoveryAgent;
import com.rasentinel.agent.operations.core.EndToEndTransactionInvestigationAgent;
import com.rasentinel.agent.operations.core.IncidentManagementAgent;
import com.rasentinel.agent.operations.core.RentalAgreementTroubleshootingAgent;
import com.rasentinel.agent.operations.core.SystemHealthMonitoringAgent;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ops-agents")
public class OperationsAgentController {
    private final RentalAgreementTroubleshootingAgent troubleshootingAgent;
    private final IncidentManagementAgent incidentManagementAgent;
    private final CustomerCompletionRecoveryAgent recoveryAgent;
    private final CounterSupportCopilot counterSupportCopilot;
    private final ComplianceAuditAgent complianceAuditAgent;
    private final SystemHealthMonitoringAgent healthMonitoringAgent;
    private final EndToEndTransactionInvestigationAgent transactionInvestigationAgent;

    public OperationsAgentController(
            RentalAgreementTroubleshootingAgent troubleshootingAgent,
            IncidentManagementAgent incidentManagementAgent,
            CustomerCompletionRecoveryAgent recoveryAgent,
            CounterSupportCopilot counterSupportCopilot,
            ComplianceAuditAgent complianceAuditAgent,
            SystemHealthMonitoringAgent healthMonitoringAgent,
            EndToEndTransactionInvestigationAgent transactionInvestigationAgent
    ) {
        this.troubleshootingAgent = troubleshootingAgent;
        this.incidentManagementAgent = incidentManagementAgent;
        this.recoveryAgent = recoveryAgent;
        this.counterSupportCopilot = counterSupportCopilot;
        this.complianceAuditAgent = complianceAuditAgent;
        this.healthMonitoringAgent = healthMonitoringAgent;
        this.transactionInvestigationAgent = transactionInvestigationAgent;
    }

    @PostMapping("/ra-troubleshooting")
    public OperationsAgentReport troubleshootRa(@Valid @RequestBody AgentCaseRequest request) {
        return troubleshootingAgent.investigate(request);
    }

    @PostMapping("/incident-management")
    public OperationsAgentReport investigateIncident(@Valid @RequestBody AgentCaseRequest request) {
        return incidentManagementAgent.investigate(request);
    }

    @PostMapping("/completion-recovery")
    public OperationsAgentReport recoverCompletion(@Valid @RequestBody AgentCaseRequest request) {
        return recoveryAgent.investigate(request);
    }

    @PostMapping("/counter-copilot")
    public OperationsAgentReport supportCounter(@Valid @RequestBody AgentCaseRequest request) {
        return counterSupportCopilot.answer(request);
    }

    @PostMapping("/compliance-audit")
    public OperationsAgentReport auditCompliance(@Valid @RequestBody AgentCaseRequest request) {
        return complianceAuditAgent.audit(request);
    }

    @PostMapping("/health-monitoring")
    public OperationsAgentReport monitorHealth(@Valid @RequestBody AgentCaseRequest request) {
        return healthMonitoringAgent.check(request);
    }

    @PostMapping("/transaction-investigation")
    public OperationsAgentReport investigateTransaction(@Valid @RequestBody AgentCaseRequest request) {
        return transactionInvestigationAgent.trace(request);
    }
}
