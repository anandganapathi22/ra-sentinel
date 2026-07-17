package com.rasentinel.agent.operations.core;

import com.rasentinel.agent.ai.AiAssessmentRequest;
import com.rasentinel.agent.ai.AiReasoningClient;
import com.rasentinel.agent.operations.api.AgentCaseRequest;
import com.rasentinel.agent.operations.api.OperationsAgentReport;
import com.rasentinel.agent.tools.DashTool;
import com.rasentinel.agent.tools.EraTool;
import com.rasentinel.agent.tools.RmsTool;
import com.rasentinel.agent.tools.TasTool;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

@Service
public class CounterSupportCopilot {
    private static final String AGENT_NAME = "Counter Support Copilot";
    private static final String TASK_INSTRUCTION = "Answer the counter agent's question about why this RA is not "
            + "ready, based on eRA/RMS/Dash/TAS state, and point them at the missing prerequisite if any.";
    private static final List<String> VOCABULARY = List.of("RESCAN_LICENSE", "RUN_RA_TROUBLESHOOTING");

    private final EraTool eraTool;
    private final RmsTool rmsTool;
    private final DashTool dashTool;
    private final TasTool tasTool;
    private final AgentReportFactory reports;
    private final AiReasoningClient aiReasoningClient;

    public CounterSupportCopilot(
            EraTool eraTool,
            RmsTool rmsTool,
            DashTool dashTool,
            TasTool tasTool,
            AgentReportFactory reports,
            AiReasoningClient aiReasoningClient
    ) {
        this.eraTool = eraTool;
        this.rmsTool = rmsTool;
        this.dashTool = dashTool;
        this.tasTool = tasTool;
        this.reports = reports;
        this.aiReasoningClient = aiReasoningClient;
    }

    public OperationsAgentReport answer(AgentCaseRequest request) {
        var era = eraTool.getSessionStatus(request.raId());
        var rms = rmsTool.getRentalAgreement(request.raId());
        var dash = dashTool.getCounterState(request.raId());
        var tas = tasTool.getAgreementStatus(request.raId());
        var evidence = new ArrayList<String>();
        evidence.add("eRA last step is " + era.lastStep());
        evidence.add("License scan present: " + era.licenseScanPresent());
        evidence.add("Customer eligible: " + era.customerEligible());
        evidence.add("RMS status is " + rms.status());
        evidence.add("Dash state is " + dash.state());
        evidence.add("TAS state is " + tas.state());

        Supplier<OperationsAgentReport> deterministic = () -> {
            if (!era.licenseScanPresent()) {
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

        var context = Map.<String, Object>of("era", era, "rms", rms, "dash", dash, "tas", tas);
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
