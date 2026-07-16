package com.rasentinel.agent.core;

import com.rasentinel.agent.tools.records.CorrelationEvent;
import com.rasentinel.agent.tools.records.DashCounterState;
import com.rasentinel.agent.tools.records.KeyspaceSubmissionRecord;
import com.rasentinel.agent.tools.records.RmsRentalAgreement;
import com.rasentinel.agent.tools.records.S3SignedPdfStatus;
import com.rasentinel.agent.tools.records.StlSubmissionMetadata;
import com.rasentinel.agent.tools.records.TasAgreementStatus;
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
    void classifiesExpiredTasHashAsBlockedHumanApprovedAction() {
        var diagnosis = classifier.classify(snapshot(
                rms("PENDING_SIGNATURE", "customer@example.com", "en-US"),
                stl("SUBMITTED", "customer@example.com", "en-US"),
                tas("WAITING_FOR_SIGNATURE", Instant.parse("2026-07-16T19:00:00Z")),
                s3(false),
                keyspace(false)
        ));

        assertThat(diagnosis.status()).isEqualTo("blocked");
        assertThat(diagnosis.likelyCause()).isEqualTo("TAS signing hash expired");
        assertThat(diagnosis.recommendedAction()).contains("after human approval");
        assertThat(diagnosis.evidence()).contains("TAS signing hash expired at 2026-07-16T19:00:00Z");
    }

    @Test
    void classifiesMissingSignedPdfAsIncidentWithoutLegalRecreation() {
        var diagnosis = classifier.classify(snapshot(
                rms("SIGNED", "customer@example.com", "en-US"),
                stl("SUBMITTED", "customer@example.com", "en-US"),
                tas("SIGNED", Instant.parse("2026-07-17T20:00:00Z")),
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
                rms("PENDING_SIGNATURE", "right@example.com", "en-US"),
                stl("SUBMITTED", "wrong@example.com", "en-US"),
                tas("WAITING_FOR_SIGNATURE", Instant.parse("2026-07-17T20:00:00Z")),
                s3(false),
                keyspace(false)
        ));

        assertThat(diagnosis.status()).isEqualTo("blocked");
        assertThat(diagnosis.likelyCause()).isEqualTo("Customer email mismatch between RMS and STL");
    }

    private RaSnapshot snapshot(
            RmsRentalAgreement rms,
            StlSubmissionMetadata stl,
            TasAgreementStatus tas,
            S3SignedPdfStatus s3,
            KeyspaceSubmissionRecord keyspace
    ) {
        return new RaSnapshot(
                rms,
                new DashCounterState("123", "WAITING_ON_CUSTOMER", "DFW"),
                stl,
                tas,
                s3,
                keyspace,
                List.of(new CorrelationEvent("TEST", "corr-123", "test event", CLOCK.instant()))
        );
    }

    private RmsRentalAgreement rms(String status, String email, String language) {
        return new RmsRentalAgreement("123", true, status, email, language);
    }

    private StlSubmissionMetadata stl(String status, String email, String language) {
        return new StlSubmissionMetadata("123", status, email, language, "corr-123");
    }

    private TasAgreementStatus tas(String state, Instant hashExpiresAt) {
        return new TasAgreementStatus("123", state, "tas-123", hashExpiresAt);
    }

    private S3SignedPdfStatus s3(boolean present) {
        return new S3SignedPdfStatus("123", present, "ra-signed-pdfs", present ? "agreements/123/signed.pdf" : "");
    }

    private KeyspaceSubmissionRecord keyspace(boolean present) {
        return new KeyspaceSubmissionRecord("123", present, present ? "SYNCED" : "MISSING", present ? CLOCK.instant() : null);
    }
}
