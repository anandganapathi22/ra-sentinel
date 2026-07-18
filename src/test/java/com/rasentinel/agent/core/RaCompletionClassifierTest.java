package com.rasentinel.agent.core;

import com.rasentinel.agent.tools.records.CorrelationEvent;
import com.rasentinel.agent.tools.records.CounterConsoleState;
import com.rasentinel.agent.tools.records.ContractVaultStatus;
import com.rasentinel.agent.tools.records.FleetLedgerAgreement;
import com.rasentinel.agent.tools.records.KeyspaceSubmissionRecord;
import com.rasentinel.agent.tools.records.S3SignedPdfStatus;
import com.rasentinel.agent.tools.records.SubmissionGatewayMetadata;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RaCompletionClassifierTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-16T20:00:00Z"), ZoneOffset.UTC);
    private final RaCompletionClassifier classifier = new RaCompletionClassifier(CLOCK);

    @Test
    void classifiesExpiredVaultHashAsBlockedHumanApprovedAction() {
        var diagnosis = classifier.classify(snapshot(
                fleet("PENDING_SIGNATURE", "customer@example.com", "en-US"),
                gateway("SUBMITTED", "customer@example.com", "en-US"),
                vault("WAITING_FOR_SIGNATURE", Instant.parse("2026-07-16T19:00:00Z")),
                s3(false),
                keyspace(false)
        ));

        assertThat(diagnosis.status()).isEqualTo("blocked");
        assertThat(diagnosis.likelyCause()).isEqualTo("Contract vault signing hash expired");
        assertThat(diagnosis.recommendedAction()).contains("after human approval");
        assertThat(diagnosis.evidence()).contains("Contract vault signing hash expired at 2026-07-16T19:00:00Z");
    }

    @Test
    void classifiesMissingSignedPdfAsIncidentWithoutLegalRecreation() {
        var diagnosis = classifier.classify(snapshot(
                fleet("SIGNED", "customer@example.com", "en-US"),
                gateway("SUBMITTED", "customer@example.com", "en-US"),
                vault("SIGNED", Instant.parse("2026-07-17T20:00:00Z")),
                s3(false),
                keyspace(false)
        ));

        assertThat(diagnosis.status()).isEqualTo("incident");
        assertThat(diagnosis.likelyCause()).isEqualTo("Signed PDF is missing from S3");
        assertThat(diagnosis.recommendedAction()).contains("do not recreate the legal agreement manually");
    }

    @Test
    void classifiesEmailMismatchBeforePendingCustomerAdvice() {
        var diagnosis = classifier.classify(snapshot(
                fleet("PENDING_SIGNATURE", "right@example.com", "en-US"),
                gateway("SUBMITTED", "wrong@example.com", "en-US"),
                vault("WAITING_FOR_SIGNATURE", Instant.parse("2026-07-17T20:00:00Z")),
                s3(false),
                keyspace(false)
        ));

        assertThat(diagnosis.status()).isEqualTo("blocked");
        assertThat(diagnosis.likelyCause()).isEqualTo("Customer email mismatch between the fleet ledger and submission gateway");
    }

    private RaSnapshot snapshot(
            FleetLedgerAgreement fleet,
            SubmissionGatewayMetadata gateway,
            ContractVaultStatus vault,
            S3SignedPdfStatus s3,
            KeyspaceSubmissionRecord keyspace
    ) {
        return new RaSnapshot(
                fleet,
                new CounterConsoleState("123", "WAITING_ON_CUSTOMER", "DFW"),
                gateway,
                vault,
                s3,
                keyspace,
                List.of(new CorrelationEvent("TEST", "corr-123", "test event", CLOCK.instant()))
        );
    }

    private FleetLedgerAgreement fleet(String status, String email, String language) {
        return new FleetLedgerAgreement("123", true, status, email, language);
    }

    private SubmissionGatewayMetadata gateway(String status, String email, String language) {
        return new SubmissionGatewayMetadata("123", status, email, language, "corr-123");
    }

    private ContractVaultStatus vault(String state, Instant hashExpiresAt) {
        return new ContractVaultStatus("123", state, "vault-123", hashExpiresAt);
    }

    private S3SignedPdfStatus s3(boolean present) {
        return new S3SignedPdfStatus("123", present, "ra-signed-pdfs", present ? "agreements/123/signed.pdf" : "");
    }

    private KeyspaceSubmissionRecord keyspace(boolean present) {
        return new KeyspaceSubmissionRecord("123", present, present ? "SYNCED" : "MISSING", present ? CLOCK.instant() : null);
    }
}
