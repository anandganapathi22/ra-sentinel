package com.rasentinel.agent.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AnthropicAiReasoningClientTest {
    private static final AiAssessmentRequest REQUEST = new AiAssessmentRequest(
            "Test Agent",
            "RA 123",
            "Decide whether anything is wrong.",
            List.of("evidence line"),
            Map.of("k", "v"),
            List.of("NO_ACTION")
    );

    @Test
    void returnsEmptyWithoutConstructingAClientWhenApiKeyIsBlank() {
        var properties = new RaSentinelAiProperties(true, "", "claude-opus-4-8", "low", 8000);
        var client = new AnthropicAiReasoningClient(properties, new ObjectMapper());

        assertThat(client.assess(REQUEST)).isEmpty();
    }

    @Test
    void returnsEmptyWhenDisabledEvenIfAnApiKeyIsPresent() {
        var properties = new RaSentinelAiProperties(false, "sk-ant-fake-key", "claude-opus-4-8", "low", 8000);
        var client = new AnthropicAiReasoningClient(properties, new ObjectMapper());

        assertThat(client.assess(REQUEST)).isEmpty();
    }
}
