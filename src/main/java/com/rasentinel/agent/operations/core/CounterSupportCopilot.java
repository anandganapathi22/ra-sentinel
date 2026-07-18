package com.rasentinel.agent.operations.core;

import com.rasentinel.agent.ai.AiAssessmentRequest;
import com.rasentinel.agent.ai.AiReasoningClient;
import com.rasentinel.agent.operations.api.AgentCaseRequest;
import com.rasentinel.agent.operations.api.OperationsAgentReport;
import com.rasentinel.agent.tools.CounterConsoleTool;
import com.rasentinel.agent.tools.ContractVaultTool;
import com.rasentinel.agent.tools.FleetLedgerTool;
import com.rasentinel.agent.tools.SigningPortalTool;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

@Service
public class CounterSupportCopilot {
    private static final String AGENT_NAME = "Counter Support Copilot";
    private static final String TASK_INSTRUCTION = "Answer the counter agent's question about why this RA is not "
            + "ready, based on signing portal/fleet ledger/counter console/contract vault state, and point them "
            + "at the missing prerequisite if any.";
    private static final List<String> VOCABULARY = List.of("RESCAN_LICENSE", "RUN_RA_TROUBLESHOOTING");

    private final SigningPortalTool signingPortalTool;
    private final FleetLedgerTool fleetLedgerTool;
    private final CounterConsoleTool counterConsoleTool;
    private final ContractVaultTool contractVaultTool;
    private final AgentReportFactory reports;
    private final AiReasoningClient aiReasoningClient;

    public CounterSupportCopilot(
            SigningPortalTool signingPortalTool,
            FleetLedgerTool fleetLedgerTool,
            CounterConsoleTool counterConsoleTool,
            ContractVaultTool contractVaultTool,
            AgentReportFactory reports,
            AiReasoningClient aiReasoningClient
    ) {
        this.signingPortalTool = signingPortalTool;
        this.fleetLedgerTool = fleetLedgerTool;
        this.counterConsoleTool = counterConsoleTool;
        this.contractVaultTool = contractVaultTool;
        this.reports = reports;
        this.aiReasoningClient = aiReasoningClient;
    }

    public OperationsAgentReport answer(AgentCaseRequest request) {
        var portal = signingPortalTool.getSessionStatus(request.raId());
        var fleet = fleetLedgerTool.getRentalAgreement(request.raId());
        var console = counterConsoleTool.getCounterState(request.raId());
        var vault = contractVaultTool.getAgreementStatus(request.raId());
        var evidence = new ArrayList<String>();
        evidence.add("Signing portal last step is " + portal.lastStep());
        evidence.add("License scan present: " + portal.licenseScanPresent());
        evidence.add("Customer eligible: " + portal.customerEligible());
        evidence.add("Fleet ledger status is " + fleet.status());
        evidence.add("Counter console state is " + console.state());
        evidence.add("Contract vault state is " + vault.state());

        Supplier<OperationsAgentReport> deterministic = () -> {
            if (!portal.licenseScanPresent()) {
                return reports.report(
                        AGENT_NAME,
                        "RA " + request.raId(),
                        "Medium",
                        "Customer license scan is missing.",
                        evidence,
                        List.of(),
                        "Rescan the driver's license before continuing the deterministic signing flow.",
                        false,
                        List.of("RESCAN_LICENSE")
                );
            }

            return reports.report(
                    AGENT_NAME,
                    "RA " + request.raId(),
                    "Low",
                    "No counter-side missing prerequisite detected.",
                    evidence,
                    List.of(),
                    "Use the troubleshooting agent if the agreement is still not visible downstream.",
                    false,
                    List.of("RUN_RA_TROUBLESHOOTING")
            );
        };

        var context = Map.<String, Object>of("portal", portal, "fleet", fleet, "console", console, "vault", vault);
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
