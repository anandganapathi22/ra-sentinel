package com.rasentinel.agent.ai;

import java.util.List;
import java.util.Map;

public record AiAssessmentRequest(
        String agentName,
        String subject,
        String taskInstruction,
        List<String> evidence,
        Map<String, Object> context,
        List<String> allowedActionVocabulary
) {
}
