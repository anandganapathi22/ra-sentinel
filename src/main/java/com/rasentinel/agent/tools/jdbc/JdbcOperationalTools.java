package com.rasentinel.agent.tools.jdbc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rasentinel.agent.tools.CorrelationTool;
import com.rasentinel.agent.tools.CounterConsoleTool;
import com.rasentinel.agent.tools.ContractVaultTool;
import com.rasentinel.agent.tools.FleetLedgerTool;
import com.rasentinel.agent.tools.KeyspaceTool;
import com.rasentinel.agent.tools.OperationalHealthTool;
import com.rasentinel.agent.tools.S3DocumentTool;
import com.rasentinel.agent.tools.SigningPortalTool;
import com.rasentinel.agent.tools.SubmissionGatewayTool;
import com.rasentinel.agent.tools.records.CorrelationEvent;
import com.rasentinel.agent.tools.records.CounterConsoleState;
import com.rasentinel.agent.tools.records.ContractVaultStatus;
import com.rasentinel.agent.tools.records.FleetLedgerAgreement;
import com.rasentinel.agent.tools.records.KeyspaceSubmissionRecord;
import com.rasentinel.agent.tools.records.S3SignedPdfStatus;
import com.rasentinel.agent.tools.records.SigningPortalSession;
import com.rasentinel.agent.tools.records.SubmissionGatewayMetadata;
import com.rasentinel.agent.tools.records.SystemHealthSnapshot;
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
public class JdbcOperationalTools implements FleetLedgerTool, CounterConsoleTool, SubmissionGatewayTool, ContractVaultTool, S3DocumentTool, KeyspaceTool, CorrelationTool, SigningPortalTool, OperationalHealthTool {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcOperationalTools(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public FleetLedgerAgreement getRentalAgreement(String raId) {
        return jdbcTemplate.query("""
                select *
                from fleet_ledger_agreements
                where ra_id = ?
                """, this::mapFleetLedger, raId).stream().findFirst()
                .orElseGet(() -> new FleetLedgerAgreement(raId, false, "NOT_FOUND", "", ""));
    }

    @Override
    public CounterConsoleState getCounterState(String raId) {
        return jdbcTemplate.query("""
                select *
                from counter_console_states
                where ra_id = ?
                """, this::mapCounterConsole, raId).stream().findFirst()
                .orElseGet(() -> new CounterConsoleState(raId, "UNKNOWN", "UNKNOWN"));
    }

    @Override
    public SubmissionGatewayMetadata getSubmissionMetadata(String raId) {
        return jdbcTemplate.query("""
                select *
                from submission_gateway_metadata
                where ra_id = ?
                """, this::mapSubmissionGateway, raId).stream().findFirst()
                .orElseGet(() -> new SubmissionGatewayMetadata(raId, "UNKNOWN", "", "", "corr-unknown"));
    }

    @Override
    public ContractVaultStatus getAgreementStatus(String raId) {
        return jdbcTemplate.query("""
                select *
                from contract_vault_status
                where ra_id = ?
                """, this::mapContractVault, raId).stream().findFirst()
                .orElseGet(() -> new ContractVaultStatus(raId, "UNKNOWN", "", null));
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
    public SigningPortalSession getSessionStatus(String raId) {
        return jdbcTemplate.query("""
                select *
                from signing_portal_sessions
                where ra_id = ?
                """, this::mapSigningPortal, raId).stream().findFirst()
                .orElseGet(() -> new SigningPortalSession(raId, "UNKNOWN", null, null, "UNKNOWN", false, false));
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

    private FleetLedgerAgreement mapFleetLedger(ResultSet rs, int rowNum) throws SQLException {
        return new FleetLedgerAgreement(
                rs.getString("ra_id"),
                rs.getBoolean("exists_flag"),
                rs.getString("status"),
                rs.getString("customer_email"),
                rs.getString("language")
        );
    }

    private CounterConsoleState mapCounterConsole(ResultSet rs, int rowNum) throws SQLException {
        return new CounterConsoleState(
                rs.getString("ra_id"),
                rs.getString("state"),
                rs.getString("counter_location")
        );
    }

    private SubmissionGatewayMetadata mapSubmissionGateway(ResultSet rs, int rowNum) throws SQLException {
        return new SubmissionGatewayMetadata(
                rs.getString("ra_id"),
                rs.getString("submit_status"),
                rs.getString("email"),
                rs.getString("language"),
                rs.getString("correlation_id")
        );
    }

    private ContractVaultStatus mapContractVault(ResultSet rs, int rowNum) throws SQLException {
        return new ContractVaultStatus(
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

    private SigningPortalSession mapSigningPortal(ResultSet rs, int rowNum) throws SQLException {
        return new SigningPortalSession(
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
                rs.getString("fleet_status"),
                rs.getString("console_status"),
                rs.getString("vault_status"),
                rs.getInt("vault_latency_ms"),
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
