package com.rasentinel;

import com.rasentinel.agent.ai.RaSentinelAiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(RaSentinelAiProperties.class)
public class RaSentinelApplication {
    public static void main(String[] args) {
        SpringApplication.run(RaSentinelApplication.class, args);
    }
}
