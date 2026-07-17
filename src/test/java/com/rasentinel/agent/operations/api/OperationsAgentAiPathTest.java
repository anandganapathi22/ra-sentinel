package com.rasentinel.agent.operations.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rasentinel.agent.ai.AiAssessment;
import com.rasentinel.agent.ai.AiAssessmentRequest;
import com.rasentinel.agent.ai.AiReasoningClient;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the AI-first / deterministic-fallback wiring described in the AI reasoning layer plan:
 * a present assessment is surfaced, a hallucinated action is clamped out before it reaches the
 * report, and an absent assessment (AI disabled/failed) reproduces today's deterministic output.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OperationsAgentAiPathTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AiReasoningClient aiReasoningClient;

    @Test
    void surfacesAiAssessmentWhenPresent() throws Exception {
        when(aiReasoningClient.assess(any(AiAssessmentRequest.class))).thenReturn(Optional.of(new AiAssessment(
                "High",
                "AI-synthesized root cause for RA 489965957.",
                "AI-recommended next step.",
                List.of("RESUBMIT_TO_TAS_AFTER_APPROVAL"),
                "high"
        )));

        postCase("/api/ops-agents/ra-troubleshooting", new AgentCaseRequest("489965957", "ORD", "q"))
                .andExpect(jsonPath("$.severity").value("High"))
                .andExpect(jsonPath("$.rootCause").value("AI-synthesized root cause for RA 489965957."))
                .andExpect(jsonPath("$.recommendedAction").value("AI-recommended next step."))
                .andExpect(jsonPath("$.allowedActions", hasItem("RESUBMIT_TO_TAS_AFTER_APPROVAL")));
    }

    @Test
    void clampsOutOfVocabularyActionsAndKeepsBlockedActionsIntact() throws Exception {
        when(aiReasoningClient.assess(any(AiAssessmentRequest.class))).thenReturn(Optional.of(new AiAssessment(
                "High",
                "AI claims signing is possible.",
                "Sign the agreement for the customer immediately.",
                List.of("SIGN_FOR_CUSTOMER", "RESUBMIT_TO_TAS_AFTER_APPROVAL"),
                "high"
        )));

        postCase("/api/ops-agents/ra-troubleshooting", new AgentCaseRequest("489965957", "ORD", "q"))
                .andExpect(jsonPath("$.allowedActions", not(hasItem("SIGN_FOR_CUSTOMER"))))
                .andExpect(jsonPath("$.allowedActions", hasItem("RESUBMIT_TO_TAS_AFTER_APPROVAL")))
                .andExpect(jsonPath("$.blockedActions", hasItem("SIGN_FOR_CUSTOMER")))
                .andExpect(jsonPath("$.requiresHumanApproval").value(true));
    }

    @Test
    void fallsBackToDeterministicClassificationWhenAiIsUnavailable() throws Exception {
        when(aiReasoningClient.assess(any(AiAssessmentRequest.class))).thenReturn(Optional.empty());

        postCase("/api/ops-agents/ra-troubleshooting", new AgentCaseRequest("489965957", "ORD", "q"))
                .andExpect(jsonPath("$.severity").value("High"))
                .andExpect(jsonPath("$.rootCause").value(
                        "Customer signed successfully, PDF exists in S3, STL submit succeeded, and TAS API timeout occurred."))
                .andExpect(jsonPath("$.recommendedAction").value(
                        "Resubmit the transaction to TAS after human approval and attach the correlation timeline."));
    }

    private org.springframework.test.web.servlet.ResultActions postCase(String uri, AgentCaseRequest request) throws Exception {
        return mockMvc.perform(post(uri)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
