package com.rasentinel.agent.api;

import com.rasentinel.agent.core.RaCompletionAgent;
import com.rasentinel.agent.audit.AgentAuditStore;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agent")
public class RaCompletionController {
    private final RaCompletionAgent agent;
    private final AgentAuditStore auditStore;

    public RaCompletionController(RaCompletionAgent agent, AgentAuditStore auditStore) {
        this.agent = agent;
        this.auditStore = auditStore;
    }

    @PostMapping("/ra-completion")
    public RaCompletionResponse diagnose(@Valid @RequestBody RaCompletionRequest request) {
        return agent.diagnose(request);
    }

    @GetMapping("/audit")
    public List<RaCompletionResponse> auditRuns() {
        return auditStore.findAll();
    }

    @GetMapping("/audit/{runId}")
    public ResponseEntity<RaCompletionResponse> auditRun(@PathVariable UUID runId) {
        return auditStore.findById(runId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
