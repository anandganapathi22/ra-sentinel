package com.rasentinel.agent.ai;

import java.util.Optional;

public interface AiReasoningClient {
    Optional<AiAssessment> assess(AiAssessmentRequest request);
}
