package com.rasentinel.agent.operations.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OperationsAgentControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void troubleshootingAgentFindsTasTimeoutForSignedCustomer() throws Exception {
        postCase("/api/ops-agents/ra-troubleshooting", new AgentCaseRequest("489965957", "ORD", "Customer completed eRA but agreement is not visible in TAS."))
                .andExpect(jsonPath("$.agentName").value("Rental Agreement Troubleshooting Agent"))
                .andExpect(jsonPath("$.severity").value("High"))
                .andExpect(jsonPath("$.rootCause").value("Customer signed successfully, PDF exists in S3, STL submit succeeded, and TAS API timeout occurred."))
                .andExpect(jsonPath("$.recommendedAction").value("Resubmit the transaction to TAS after human approval and attach the correlation timeline."))
                .andExpect(jsonPath("$.blockedActions", hasItem("MODIFY_LEGAL_TEXT")))
                .andExpect(jsonPath("$.timeline[0].system").value("eRA"));
    }

    @Test
    void incidentAgentFindsChicagoRmsOutage() throws Exception {
        postCase("/api/ops-agents/incident-management", new AgentCaseRequest("489965957", "Chicago", "Chicago location is unable to retrieve agreements."))
                .andExpect(jsonPath("$.severity").value("High"))
                .andExpect(jsonPath("$.rootCause").value("RMS endpoint unavailable."))
                .andExpect(jsonPath("$.evidence", hasItem("Affected locations: [ORD, MDW]")));
    }

    @Test
    void recoveryAgentFindsAbandonedSignatureStep() throws Exception {
        postCase("/api/ops-agents/completion-recovery", new AgentCaseRequest("777", "MDW", "Customer started eRA but never completed signing."))
                .andExpect(jsonPath("$.rootCause").value("Customer abandoned eRA at SIGNATURE step."))
                .andExpect(jsonPath("$.allowedActions", hasItem("SEND_REMINDER_AFTER_APPROVAL")));
    }

    @Test
    void counterCopilotFindsMissingLicenseScan() throws Exception {
        postCase("/api/ops-agents/counter-copilot", new AgentCaseRequest("123456", "ORD", "Why is RA not ready?"))
                .andExpect(jsonPath("$.rootCause").value("Customer license scan is missing."))
                .andExpect(jsonPath("$.recommendedAction").value("Rescan the driver's license before continuing the deterministic signing flow."));
    }

    @Test
    void complianceAgentFindsMissingPdfArchive() throws Exception {
        postCase("/api/ops-agents/compliance-audit", new AgentCaseRequest("200", "DFW", "Check audit state."))
                .andExpect(jsonPath("$.rootCause").value("Signature is present but archived PDF is missing."))
                .andExpect(jsonPath("$.allowedActions", hasItem("OPEN_AUDIT_TICKET")))
                .andExpect(jsonPath("$.blockedActions", hasItem("SUBMIT_LEGAL_AGREEMENT_AUTONOMOUSLY")));
    }

    @Test
    void complianceAgentFindsMissingKeyspaceMetadata() throws Exception {
        postCase("/api/ops-agents/compliance-audit", new AgentCaseRequest("300", "DFW", "Check audit metadata."))
                .andExpect(jsonPath("$.severity").value("High"))
                .andExpect(jsonPath("$.rootCause").value("Signed PDF exists but audit metadata is missing."))
                .andExpect(jsonPath("$.allowedActions", hasItem("RETRY_SYNC_AFTER_APPROVAL")));
    }

    @Test
    void complianceAgentPassesCompleteAgreement() throws Exception {
        postCase("/api/ops-agents/compliance-audit", new AgentCaseRequest("500", "DFW", "Check audit state."))
                .andExpect(jsonPath("$.severity").value("Low"))
                .andExpect(jsonPath("$.rootCause").value("No compliance gap detected for this RA."))
                .andExpect(jsonPath("$.requiresHumanApproval").value(false))
                .andExpect(jsonPath("$.allowedActions", hasItem("NO_ACTION")))
                .andExpect(jsonPath("$.allowedActions", not(hasItem("SIGN_FOR_CUSTOMER"))));
    }

    @Test
    void healthAgentWarnsOnTasLatencyAndBacklog() throws Exception {
        postCase("/api/ops-agents/health-monitoring", new AgentCaseRequest("489965957", "Chicago", "Check health."))
                .andExpect(jsonPath("$.severity").value("Warning"))
                .andExpect(jsonPath("$.rootCause").value("Operational latency and backlog indicate counter delays are likely."));
    }

    @Test
    void transactionAgentBuildsTimeline() throws Exception {
        postCase("/api/ops-agents/transaction-investigation", new AgentCaseRequest("489965957", "ORD", "Trace transaction."))
                .andExpect(jsonPath("$.rootCause").value("Transaction reached TAS but timed out during downstream processing."))
                .andExpect(jsonPath("$.timeline[3].system").value("RMS"))
                .andExpect(jsonPath("$.timeline[4].event").value("TAS API timeout"));
    }

    @Test
    void rejectsMissingRaId() throws Exception {
        mockMvc.perform(post("/api/ops-agents/ra-troubleshooting")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AgentCaseRequest("", "ORD", "Missing RA"))))
                .andExpect(status().isBadRequest());
    }

    private org.springframework.test.web.servlet.ResultActions postCase(String uri, AgentCaseRequest request) throws Exception {
        return mockMvc.perform(post(uri)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
