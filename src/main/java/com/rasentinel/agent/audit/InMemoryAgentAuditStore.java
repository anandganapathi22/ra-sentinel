package com.rasentinel.agent.audit;

import com.rasentinel.agent.api.RaCompletionResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!docker")
public class InMemoryAgentAuditStore implements AgentAuditStore {
    private final ConcurrentHashMap<UUID, RaCompletionResponse> runs = new ConcurrentHashMap<>();

    @Override
    public void save(RaCompletionResponse response) {
        runs.put(response.runId(), response);
    }

    @Override
    public List<RaCompletionResponse> findAll() {
        var ordered = new ArrayList<>(runs.values());
        ordered.sort(Comparator.comparing(RaCompletionResponse::createdAt).reversed());
        return ordered;
    }

    @Override
    public Optional<RaCompletionResponse> findById(UUID runId) {
        return Optional.ofNullable(runs.get(runId));
    }
}
