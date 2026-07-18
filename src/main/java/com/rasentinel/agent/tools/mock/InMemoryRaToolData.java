package com.rasentinel.agent.tools.mock;

import com.rasentinel.agent.tools.records.CorrelationEvent;
import com.rasentinel.agent.tools.records.CounterConsoleState;
import com.rasentinel.agent.tools.records.ContractVaultStatus;
import com.rasentinel.agent.tools.records.FleetLedgerAgreement;
import com.rasentinel.agent.tools.records.KeyspaceSubmissionRecord;
import com.rasentinel.agent.tools.records.S3SignedPdfStatus;
import com.rasentinel.agent.tools.records.SigningPortalSession;
import com.rasentinel.agent.tools.records.SubmissionGatewayMetadata;
import com.rasentinel.agent.tools.records.SystemHealthSnapshot;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class InMemoryRaToolData {
    private final Clock clock;

    public InMemoryRaToolData(Clock clock) {
        this.clock = clock;
    }

    public FleetLedgerAgreement fleetLedger(String raId) {
        return switch (raId) {
            case "123" -> new FleetLedgerAgreement(raId, true, "PENDING_SIGNATURE", "customer@example.com", "en-US");
            case "200" -> new FleetLedgerAgreement(raId, true, "SIGNED", "signed@example.com", "en-US");
            case "300" -> new FleetLedgerAgreement(raId, true, "SIGNED", "metadata@example.com", "en-US");
            case "400" -> new FleetLedgerAgreement(raId, true, "PENDING_SIGNATURE", "right@example.com", "en-US");
            case "489965957" -> new FleetLedgerAgreement(raId, true, "SIGNED", "hertz.customer@example.com", "en-US");
            case "123456" -> new FleetLedgerAgreement(raId, true, "PENDING_COUNTER_REVIEW", "license@example.com", "en-US");
            case "777" -> new FleetLedgerAgreement(raId, true, "PENDING_SIGNATURE", "abandoned@example.com", "en-US");
            case "500" -> new FleetLedgerAgreement(raId, true, "SIGNED", "complete@example.com", "en-US");
            default -> new FleetLedgerAgreement(raId, false, "NOT_FOUND", "", "");
        };
    }

    public CounterConsoleState counterConsole(String raId) {
        return switch (raId) {
            case "123", "400" -> new CounterConsoleState(raId, "WAITING_ON_CUSTOMER", "DFW");
            case "200", "300" -> new CounterConsoleState(raId, "PENDING_BACKEND_SYNC", "DFW");
            case "489965957" -> new CounterConsoleState(raId, "SIGNED_NOT_VISIBLE_IN_VAULT", "ORD");
            case "123456" -> new CounterConsoleState(raId, "NOT_READY", "ORD");
            case "777" -> new CounterConsoleState(raId, "CUSTOMER_ABANDONED", "MDW");
            case "500" -> new CounterConsoleState(raId, "COMPLETE", "DFW");
            default -> new CounterConsoleState(raId, "UNKNOWN", "UNKNOWN");
        };
    }

    public SubmissionGatewayMetadata submissionGateway(String raId) {
        return switch (raId) {
            case "123" -> new SubmissionGatewayMetadata(raId, "SUBMITTED", "customer@example.com", "en-US", "corr-123");
            case "200" -> new SubmissionGatewayMetadata(raId, "SUBMITTED", "signed@example.com", "en-US", "corr-200");
            case "300" -> new SubmissionGatewayMetadata(raId, "SUBMITTED", "metadata@example.com", "en-US", "corr-300");
            case "400" -> new SubmissionGatewayMetadata(raId, "SUBMITTED", "wrong@example.com", "en-US", "corr-400");
            case "489965957" -> new SubmissionGatewayMetadata(raId, "SUBMITTED", "hertz.customer@example.com", "en-US", "corr-489965957");
            case "123456" -> new SubmissionGatewayMetadata(raId, "NOT_READY", "license@example.com", "en-US", "corr-123456");
            case "777" -> new SubmissionGatewayMetadata(raId, "STARTED", "abandoned@example.com", "en-US", "corr-777");
            case "500" -> new SubmissionGatewayMetadata(raId, "SUBMITTED", "complete@example.com", "en-US", "corr-500");
            default -> new SubmissionGatewayMetadata(raId, "UNKNOWN", "", "", "corr-unknown");
        };
    }

    public ContractVaultStatus contractVault(String raId) {
        return switch (raId) {
            case "123" -> new ContractVaultStatus(raId, "WAITING_FOR_SIGNATURE", "vault-123", clock.instant().minus(Duration.ofHours(2)));
            case "200", "300" -> new ContractVaultStatus(raId, "SIGNED", "vault-" + raId, clock.instant().plus(Duration.ofHours(12)));
            case "400" -> new ContractVaultStatus(raId, "WAITING_FOR_SIGNATURE", "vault-400", clock.instant().plus(Duration.ofHours(12)));
            case "489965957" -> new ContractVaultStatus(raId, "API_TIMEOUT", "vault-489965957", clock.instant().plus(Duration.ofHours(12)));
            case "123456" -> new ContractVaultStatus(raId, "NOT_CREATED", "", null);
            case "777" -> new ContractVaultStatus(raId, "WAITING_FOR_SIGNATURE", "vault-777", clock.instant().plus(Duration.ofHours(4)));
            case "500" -> new ContractVaultStatus(raId, "SIGNED", "vault-500", clock.instant().plus(Duration.ofHours(12)));
            default -> new ContractVaultStatus(raId, "UNKNOWN", "", null);
        };
    }

    public S3SignedPdfStatus s3(String raId) {
        return switch (raId) {
            case "200" -> new S3SignedPdfStatus(raId, false, "ra-signed-pdfs", "");
            case "300", "489965957", "500" -> new S3SignedPdfStatus(raId, true, "ra-signed-pdfs", "agreements/" + raId + "/signed.pdf");
            default -> new S3SignedPdfStatus(raId, false, "ra-signed-pdfs", "");
        };
    }

    public KeyspaceSubmissionRecord keyspace(String raId) {
        return switch (raId) {
            case "300" -> new KeyspaceSubmissionRecord(raId, false, "MISSING", null);
            case "200" -> new KeyspaceSubmissionRecord(raId, false, "MISSING", null);
            case "500" -> new KeyspaceSubmissionRecord(raId, true, "SYNCED", clock.instant().minus(Duration.ofMinutes(2)));
            default -> new KeyspaceSubmissionRecord(raId, false, "MISSING", null);
        };
    }

    public List<CorrelationEvent> events(String raId) {
        if ("489965957".equals(raId)) {
            return List.of(
                    new CorrelationEvent("SigningPortal", "corr-489965957", "Customer opened agreement", clock.instant().minus(Duration.ofMinutes(9))),
                    new CorrelationEvent("SigningPortal", "corr-489965957", "Customer signed agreement", clock.instant().minus(Duration.ofMinutes(7))),
                    new CorrelationEvent("SubmissionGateway", "corr-489965957", "Submission gateway submit success", clock.instant().minus(Duration.ofMinutes(7))),
                    new CorrelationEvent("FleetLedger", "corr-489965957", "Fleet ledger signed status persisted", clock.instant().minus(Duration.ofMinutes(6))),
                    new CorrelationEvent("ContractVault", "corr-489965957", "Contract vault API timeout", clock.instant().minus(Duration.ofMinutes(5))),
                    new CorrelationEvent("ContractVault", "corr-489965957", "Retry failed with timeout", clock.instant().minus(Duration.ofMinutes(3)))
            );
        }
        return List.of(
                new CorrelationEvent("SubmissionGateway", "corr-" + raId, "Submission metadata retrieved", clock.instant().minus(Duration.ofMinutes(30))),
                new CorrelationEvent("ContractVault", "corr-" + raId, "Agreement status retrieved", clock.instant().minus(Duration.ofMinutes(20)))
        );
    }

    public SigningPortalSession signingPortal(String raId) {
        return switch (raId) {
            case "489965957" -> new SigningPortalSession(raId, "SIGNED", clock.instant().minus(Duration.ofMinutes(9)), clock.instant().minus(Duration.ofMinutes(7)), "SIGNATURE_COMPLETE", true, true);
            case "123456" -> new SigningPortalSession(raId, "BLOCKED", clock.instant().minus(Duration.ofMinutes(20)), clock.instant().minus(Duration.ofMinutes(15)), "LICENSE_SCAN", false, true);
            case "777" -> new SigningPortalSession(raId, "ABANDONED", clock.instant().minus(Duration.ofMinutes(34)), clock.instant().minus(Duration.ofMinutes(31)), "SIGNATURE", true, true);
            case "500" -> new SigningPortalSession(raId, "SIGNED", clock.instant().minus(Duration.ofMinutes(10)), clock.instant().minus(Duration.ofMinutes(4)), "SIGNATURE_COMPLETE", true, true);
            default -> new SigningPortalSession(raId, "UNKNOWN", null, null, "UNKNOWN", false, false);
        };
    }

    public SystemHealthSnapshot health(String location) {
        if ("CHICAGO".equalsIgnoreCase(location) || "ORD".equalsIgnoreCase(location)) {
            return new SystemHealthSnapshot(
                    location,
                    "UNAVAILABLE",
                    "DEGRADED",
                    "SLOW",
                    6200,
                    "OK",
                    284,
                    Map.of("FLEET_503", 147, "VAULT_TIMEOUT", 21),
                    List.of("ORD", "MDW")
            );
        }
        return new SystemHealthSnapshot(
                location == null || location.isBlank() ? "GLOBAL" : location,
                "OK",
                "OK",
                "OK",
                800,
                "OK",
                12,
                Map.of(),
                List.of()
        );
    }
}
