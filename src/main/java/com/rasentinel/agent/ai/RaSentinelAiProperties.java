package com.rasentinel.agent.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rasentinel.ai")
public record RaSentinelAiProperties(
        boolean enabled,
        String apiKey,
        String model,
        String effort,
        long timeoutMs
) {
    public RaSentinelAiProperties {
        if (model == null || model.isBlank()) {
            model = "claude-opus-4-8";
        }
        if (effort == null || effort.isBlank()) {
            effort = "low";
        }
        if (timeoutMs <= 0) {
            timeoutMs = 8000;
        }
    }

    public boolean configured() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }
}
