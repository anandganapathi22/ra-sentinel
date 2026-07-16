package com.rasentinel.agent.audit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rasentinel.agent.api.RaCompletionResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Profile("docker")
public class JdbcAgentAuditStore implements AgentAuditStore {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcAgentAuditStore(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(RaCompletionResponse response) {
        jdbcTemplate.update("""
                insert into agent_audit_runs (
                    run_id,
                    ra_id,
                    status,
                    likely_cause,
                    evidence_json,
                    recommended_action,
                    confidence,
                    requires_human_approval,
                    allowed_actions_json,
                    blocked_actions_json,
                    tool_results_json,
                    created_at
                ) values (?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb, ?)
                on conflict (run_id) do update set
                    ra_id = excluded.ra_id,
                    status = excluded.status,
                    likely_cause = excluded.likely_cause,
                    evidence_json = excluded.evidence_json,
                    recommended_action = excluded.recommended_action,
                    confidence = excluded.confidence,
                    requires_human_approval = excluded.requires_human_approval,
                    allowed_actions_json = excluded.allowed_actions_json,
                    blocked_actions_json = excluded.blocked_actions_json,
                    tool_results_json = excluded.tool_results_json,
                    created_at = excluded.created_at
                """,
                response.runId(),
                response.raId(),
                response.status(),
                response.likelyCause(),
                writeJson(response.evidence()),
                response.recommendedAction(),
                response.confidence(),
                response.requiresHumanApproval(),
                writeJson(response.allowedActions()),
                writeJson(response.blockedActions()),
                writeJson(response.toolResults()),
                Timestamp.from(response.createdAt())
        );
    }

    @Override
    public List<RaCompletionResponse> findAll() {
        return jdbcTemplate.query("""
                select *
                from agent_audit_runs
                order by created_at desc
                """, this::mapRow);
    }

    @Override
    public Optional<RaCompletionResponse> findById(UUID runId) {
        return jdbcTemplate.query("""
                select *
                from agent_audit_runs
                where run_id = ?
                """, this::mapRow, runId).stream().findFirst();
    }

    private RaCompletionResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new RaCompletionResponse(
                rs.getObject("run_id", UUID.class),
                rs.getString("ra_id"),
                rs.getString("status"),
                rs.getString("likely_cause"),
                readJson(rs.getString("evidence_json"), new TypeReference<>() {}),
                rs.getString("recommended_action"),
                rs.getString("confidence"),
                rs.getBoolean("requires_human_approval"),
                readJson(rs.getString("allowed_actions_json"), new TypeReference<>() {}),
                readJson(rs.getString("blocked_actions_json"), new TypeReference<>() {}),
                readJson(rs.getString("tool_results_json"), new TypeReference<>() {}),
                rs.getTimestamp("created_at").toInstant()
        );
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize audit field", ex);
        }
    }

    private <T> T readJson(String json, TypeReference<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to deserialize audit field", ex);
        }
    }
}
