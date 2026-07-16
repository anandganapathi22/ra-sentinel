package com.rasentinel.agent.audit;

import com.rasentinel.agent.api.RaCompletionResponse;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgentAuditStore {
    void save(RaCompletionResponse response);

    List<RaCompletionResponse> findAll();

    Optional<RaCompletionResponse> findById(UUID runId);
}
