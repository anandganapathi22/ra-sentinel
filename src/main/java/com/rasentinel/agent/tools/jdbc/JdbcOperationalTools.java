package com.rasentinel.agent.tools.jdbc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rasentinel.agent.tools.CorrelationTool;
import com.rasentinel.agent.tools.DashTool;
import com.rasentinel.agent.tools.EraTool;
import com.rasentinel.agent.tools.KeyspaceTool;
import com.rasentinel.agent.tools.OperationalHealthTool;
import com.rasentinel.agent.tools.RmsTool;
import com.rasentinel.agent.tools.S3DocumentTool;
import com.rasentinel.agent.tools.StlTool;
import com.rasentinel.agent.tools.TasTool;
import com.rasentinel.agent.tools.records.CorrelationEvent;
import com.rasentinel.agent.tools.records.DashCounterState;
import com.rasentinel.agent.tools.records.EraSessionStatus;
import com.rasentinel.agent.tools.records.KeyspaceSubmissionRecord;
import com.rasentinel.agent.tools.records.RmsRentalAgreement;
import com.rasentinel.agent.tools.records.S3SignedPdfStatus;
import com.rasentinel.agent.tools.records.StlSubmissionMetadata;
import com.rasentinel.agent.tools.records.SystemHealthSnapshot;
import com.rasentinel.agent.tools.records.TasAgreementStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("docker")
public class JdbcOperationalTools implements RmsTool, DashTool, StlTool, TasTool, S3DocumentTool, KeyspaceTool, CorrelationTool, EraTool, OperationalHealthTool {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcOperationalTools(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public RmsRentalAgreement getRentalAgreement(String raId) {
        return jdbcTemplate.query("""
                select *
                from rms_rental_agreements
                where ra_id = ?
                """, this::mapRms, raId).stream().findFirst()
                .orElseGet(() -> new RmsRentalAgreement(raId, false, "NOT_FOUND", "", ""));
    }

    @Override
    public DashCounterState getCounterState(String raId) {
        return jdbcTemplate.query("""
                select *
                from dash_counter_states
                where ra_id = ?
                """, this::mapDash, raId).stream().findFirst()
                .orElseGet(() -> new DashCounterState(raId, "UNKNOWN", "UNKNOWN"));
    }

    @Override
    public StlSubmissionMetadata getSubmissionMetadata(String raId) {
        return jdbcTemplate.query("""
                select *
                from stl_submission_metadata
                where ra_id = ?
                """, this::mapStl, raId).stream().findFirst()
                .orElseGet(() -> new StlSubmissionMetadata(raId, "UNKNOWN", "", "", "corr-unknown"));
    }

    @Override
    public TasAgreementStatus getAgreementStatus(String raId) {
        return jdbcTemplate.query("""
                select *
                from tas_agreement_status
                where ra_id = ?
                """, this::mapTas, raId).stream().findFirst()
                .orElseGet(() -> new TasAgreementStatus(raId, "UNKNOWN", "", null));
    }

    @Override
    public S3SignedPdfStatus getSignedPdfStatus(String raId) {
        return jdbcTemplate.query("""
                select *
                from s3_signed_pdf_status
                where ra_id = ?
                """, this::mapS3, raId).stream().findFirst()
                .orElseGet(() -> new S3SignedPdfStatus(raId, false, "ra-signed-pdfs", ""));
    }

    @Override
    public KeyspaceSubmissionRecord getSubmissionRecord(String raId) {
        return jdbcTemplate.query("""
                select *
                from keyspace_submission_records
                where ra_id = ?
                """, this::mapKeyspace, raId).stream().findFirst()
                .orElseGet(() -> new KeyspaceSubmissionRecord(raId, false, "MISSING", null));
    }

    @Override
    public List<CorrelationEvent> getEvents(String raId) {
        return jdbcTemplate.query("""
                select *
                from correlation_events
                where ra_id = ?
                order by occurred_at asc, id asc
                """, this::mapCorrelationEvent, raId);
    }

    @Override
    public EraSessionStatus getSessionStatus(String raId) {
        return jdbcTemplate.query("""
                select *
                from era_session_status
                where ra_id = ?
                """, this::mapEra, raId).stream().findFirst()
                .orElseGet(() -> new EraSessionStatus(raId, "UNKNOWN", null, null, "UNKNOWN", false, false));
    }

    @Override
    public SystemHealthSnapshot getHealth(String location) {
        String normalized = location == null || location.isBlank() ? "GLOBAL" : location.toUpperCase();
        return jdbcTemplate.query("""
                select *
                from system_health_snapshots
                where location = ?
                """, this::mapHealth, normalized).stream().findFirst()
                .orElseGet(() -> jdbcTemplate.query("""
                        select *
                        from system_health_snapshots
                        where location = 'GLOBAL'
                        """, this::mapHealth).stream().findFirst()
                        .orElseGet(() -> new SystemHealthSnapshot(normalized, "OK", "OK", "OK", 800, "OK", 0, Map.of(), List.of())));
    }

    private RmsRentalAgreement mapRms(ResultSet rs, int rowNum) throws SQLException {
        return new RmsRentalAgreement(
                rs.getString("ra_id"),
                rs.getBoolean("exists_flag"),
                rs.getString("status"),
                rs.getString("customer_email"),
                rs.getString("language")
        );
    }

    private DashCounterState mapDash(ResultSet rs, int rowNum) throws SQLException {
        return new DashCounterState(
                rs.getString("ra_id"),
                rs.getString("state"),
                rs.getString("counter_location")
        );
    }

    private StlSubmissionMetadata mapStl(ResultSet rs, int rowNum) throws SQLException {
        return new StlSubmissionMetadata(
                rs.getString("ra_id"),
                rs.getString("submit_status"),
                rs.getString("email"),
                rs.getString("language"),
                rs.getString("correlation_id")
        );
    }

    private TasAgreementStatus mapTas(ResultSet rs, int rowNum) throws SQLException {
        return new TasAgreementStatus(
                rs.getString("ra_id"),
                rs.getString("state"),
                rs.getString("agreement_id"),
                instantOrNull(rs.getTimestamp("hash_expires_at"))
        );
    }

    private S3SignedPdfStatus mapS3(ResultSet rs, int rowNum) throws SQLException {
        return new S3SignedPdfStatus(
                rs.getString("ra_id"),
                rs.getBoolean("present"),
                rs.getString("bucket"),
                rs.getString("object_key")
        );
    }

    private KeyspaceSubmissionRecord mapKeyspace(ResultSet rs, int rowNum) throws SQLException {
        return new KeyspaceSubmissionRecord(
                rs.getString("ra_id"),
                rs.getBoolean("present"),
                rs.getString("status"),
                instantOrNull(rs.getTimestamp("updated_at"))
        );
    }

    private CorrelationEvent mapCorrelationEvent(ResultSet rs, int rowNum) throws SQLException {
        return new CorrelationEvent(
                rs.getString("source"),
                rs.getString("correlation_id"),
                rs.getString("message"),
                rs.getTimestamp("occurred_at").toInstant()
        );
    }

    private EraSessionStatus mapEra(ResultSet rs, int rowNum) throws SQLException {
        return new EraSessionStatus(
                rs.getString("ra_id"),
                rs.getString("status"),
                instantOrNull(rs.getTimestamp("started_at")),
                instantOrNull(rs.getTimestamp("last_activity_at")),
                rs.getString("last_step"),
                rs.getBoolean("license_scan_present"),
                rs.getBoolean("customer_eligible")
        );
    }

    private SystemHealthSnapshot mapHealth(ResultSet rs, int rowNum) throws SQLException {
        return new SystemHealthSnapshot(
                rs.getString("location"),
                rs.getString("rms_status"),
                rs.getString("dash_status"),
                rs.getString("tas_status"),
                rs.getInt("tas_latency_ms"),
                rs.getString("database_status"),
                rs.getInt("queue_backlog"),
                readJson(rs.getString("recent_http_failures_json"), new TypeReference<>() {}),
                readJson(rs.getString("affected_locations_json"), new TypeReference<>() {})
        );
    }

    private Instant instantOrNull(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private <T> T readJson(String json, TypeReference<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to read operational JSON data", ex);
        }
    }
}
