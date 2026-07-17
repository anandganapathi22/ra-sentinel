package com.rasentinel.agent.ai;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AnthropicAiReasoningClient implements AiReasoningClient {
    private static final Logger log = LoggerFactory.getLogger(AnthropicAiReasoningClient.class);

    private static final String SYSTEM_PROMPT = """
            You are the reasoning component of RA Sentinel, a read-only rental agreement \
            operations assistant. Deterministic tools have already gathered the evidence \
            below; you do not gather evidence yourself and you never execute any action.

            Rules:
            - Ground every claim in the evidence and context you are given. Never invent facts.
            - Choose allowedActions only from the allowed action vocabulary given in the \
            user turn. Never propose an action outside that list.
            - Never imply that you can modify legal text, change charges, sign for the \
            customer, or submit a legal agreement autonomously. Those actions are always \
            blocked regardless of what you recommend.
            - severity must be one of: Low, Medium, High, Warning, Normal.
            - confidence must be one of: low, medium, high.
            """;

    private final RaSentinelAiProperties properties;
    private final ObjectMapper objectMapper;
    private final AnthropicClient client;

    public AnthropicAiReasoningClient(RaSentinelAiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.client = properties.configured()
                ? AnthropicOkHttpClient.builder()
                        .apiKey(properties.apiKey())
                        .timeout(Duration.ofMillis(properties.timeoutMs()))
                        .build()
                : null;
    }

    @Override
    public Optional<AiAssessment> assess(AiAssessmentRequest request) {
        if (client == null) {
            return Optional.empty();
        }
        try {
            var params = MessageCreateParams.builder()
                    .model(Model.of(properties.model()))
                    .maxTokens(1024L)
                    .system(SYSTEM_PROMPT)
                    .outputConfig(AiAssessment.class)
                    .addUserMessage(userTurn(request))
                    .build();

            var response = client.messages().create(params);
            return response.content().stream()
                    .flatMap(block -> block.text().stream())
                    .map(text -> text.text())
                    .findFirst();
        } catch (AnthropicServiceException e) {
            log.warn("AI reasoning failed for {} ({}); falling back to deterministic classification",
                    request.subject(), request.agentName(), e);
            return Optional.empty();
        } catch (RuntimeException e) {
            log.warn("AI reasoning unavailable for {} ({}); falling back to deterministic classification",
                    request.subject(), request.agentName(), e);
            return Optional.empty();
        }
    }

    private String userTurn(AiAssessmentRequest request) {
        var evidenceBlock = request.evidence().stream()
                .map(line -> "- " + line)
                .collect(Collectors.joining("\n"));
        return """
                Agent: %s
                Case: %s
                Task: %s

                Evidence:
                %s

                Raw system context (JSON):
                %s

                Allowed action vocabulary (choose zero or more, verbatim, nothing outside this list): %s
                """.formatted(
                request.agentName(),
                request.subject(),
                request.taskInstruction(),
                evidenceBlock,
                writeContextJson(request.context()),
                String.join(", ", request.allowedActionVocabulary()));
    }

    private String writeContextJson(Map<String, Object> context) {
        try {
            return objectMapper.writeValueAsString(context);
        } catch (Exception e) {
            return "{}";
        }
    }
}
