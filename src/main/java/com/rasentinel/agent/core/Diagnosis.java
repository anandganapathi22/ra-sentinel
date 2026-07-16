package com.rasentinel.agent.core;

import java.util.List;

public record Diagnosis(
        String status,
        String likelyCause,
        List<String> evidence,
        String recommendedAction,
        String confidence
) {
}
