package com.rasentinel.agent.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RaCompletionControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void diagnosesSeedRaAndPersistsAuditRun() throws Exception {
        var body = objectMapper.writeValueAsString(new RaCompletionRequest("123", "Why can't this customer finish signing?"));

        mockMvc.perform(post("/api/agent/ra-completion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.raId").value("123"))
                .andExpect(jsonPath("$.status").value("blocked"))
                .andExpect(jsonPath("$.likelyCause").value("Contract vault signing hash expired"))
                .andExpect(jsonPath("$.requiresHumanApproval").value(true))
                .andExpect(jsonPath("$.blockedActions", hasItem("MODIFY_LEGAL_TEXT")))
                .andExpect(jsonPath("$.blockedActions", hasItem("SIGN_FOR_CUSTOMER")));

        mockMvc.perform(get("/api/agent/audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].raId").value("123"));
    }
}
