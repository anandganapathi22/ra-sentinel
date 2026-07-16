package com.rasentinel.agent.tools.mock;

import com.rasentinel.agent.tools.records.CorrelationEvent;
import com.rasentinel.agent.tools.records.DashCounterState;
import com.rasentinel.agent.tools.records.EraSessionStatus;
import com.rasentinel.agent.tools.records.KeyspaceSubmissionRecord;
import com.rasentinel.agent.tools.records.RmsRentalAgreement;
import com.rasentinel.agent.tools.records.S3SignedPdfStatus;
import com.rasentinel.agent.tools.records.StlSubmissionMetadata;
import com.rasentinel.agent.tools.records.SystemHealthSnapshot;
import com.rasentinel.agent.tools.records.TasAgreementStatus;
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

    public RmsRentalAgreement rms(String raId) {
        return switch (raId) {
            case "123" -> new RmsRentalAgreement(raId, true, "PENDING_SIGNATURE", "customer@example.com", "en-US");
            case "200" -> new RmsRentalAgreement(raId, true, "SIGNED", "signed@example.com", "en-US");
            case "300" -> new RmsRentalAgreement(raId, true, "SIGNED", "metadata@example.com", "en-US");
            case "400" -> new RmsRentalAgreement(raId, true, "PENDING_SIGNATURE", "right@example.com", "en-US");
            case "489965957" -> new RmsRentalAgreement(raId, true, "SIGNED", "hertz.customer@example.com", "en-US");
            case "123456" -> new RmsRentalAgreement(raId, true, "PENDING_COUNTER_REVIEW", "license@example.com", "en-US");
            case "777" -> new RmsRentalAgreement(raId, true, "PENDING_SIGNATURE", "abandoned@example.com", "en-US");
            case "500" -> new RmsRentalAgreement(raId, true, "SIGNED", "complete@example.com", "en-US");
            default -> new RmsRentalAgreement(raId, false, "NOT_FOUND", "", "");
        };
    }

    public DashCounterState dash(String raId) {
        return switch (raId) {
            case "123", "400" -> new DashCounterState(raId, "WAITING_ON_CUSTOMER", "DFW");
            case "200", "300" -> new DashCounterState(raId, "PENDING_BACKEND_SYNC", "DFW");
            case "489965957" -> new DashCounterState(raId, "SIGNED_NOT_VISIBLE_IN_TAS", "ORD");
            case "123456" -> new DashCounterState(raId, "NOT_READY", "ORD");
            case "777" -> new DashCounterState(raId, "CUSTOMER_ABANDONED", "MDW");
            case "500" -> new DashCounterState(raId, "COMPLETE", "DFW");
            default -> new DashCounterState(raId, "UNKNOWN", "UNKNOWN");
        };
    }

    public StlSubmissionMetadata stl(String raId) {
        return switch (raId) {
            case "123" -> new StlSubmissionMetadata(raId, "SUBMITTED", "customer@example.com", "en-US", "corr-123");
            case "200" -> new StlSubmissionMetadata(raId, "SUBMITTED", "signed@example.com", "en-US", "corr-200");
            case "300" -> new StlSubmissionMetadata(raId, "SUBMITTED", "metadata@example.com", "en-US", "corr-300");
            case "400" -> new StlSubmissionMetadata(raId, "SUBMITTED", "wrong@example.com", "en-US", "corr-400");
            case "489965957" -> new StlSubmissionMetadata(raId, "SUBMITTED", "hertz.customer@example.com", "en-US", "corr-489965957");
            case "123456" -> new StlSubmissionMetadata(raId, "NOT_READY", "license@example.com", "en-US", "corr-123456");
            case "777" -> new StlSubmissionMetadata(raId, "STARTED", "abandoned@example.com", "en-US", "corr-777");
            case "500" -> new StlSubmissionMetadata(raId, "SUBMITTED", "complete@example.com", "en-US", "corr-500");
            default -> new StlSubmissionMetadata(raId, "UNKNOWN", "", "", "corr-unknown");
        };
    }

    public TasAgreementStatus tas(String raId) {
        return switch (raId) {
            case "123" -> new TasAgreementStatus(raId, "WAITING_FOR_SIGNATURE", "tas-123", clock.instant().minus(Duration.ofHours(2)));
            case "200", "300" -> new TasAgreementStatus(raId, "SIGNED", "tas-" + raId, clock.instant().plus(Duration.ofHours(12)));
            case "400" -> new TasAgreementStatus(raId, "WAITING_FOR_SIGNATURE", "tas-400", clock.instant().plus(Duration.ofHours(12)));
            case "489965957" -> new TasAgreementStatus(raId, "API_TIMEOUT", "tas-489965957", clock.instant().plus(Duration.ofHours(12)));
            case "123456" -> new TasAgreementStatus(raId, "NOT_CREATED", "", null);
            case "777" -> new TasAgreementStatus(raId, "WAITING_FOR_SIGNATURE", "tas-777", clock.instant().plus(Duration.ofHours(4)));
            case "500" -> new TasAgreementStatus(raId, "SIGNED", "tas-500", clock.instant().plus(Duration.ofHours(12)));
            default -> new TasAgreementStatus(raId, "UNKNOWN", "", null);
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
                    new CorrelationEvent("eRA", "corr-489965957", "Customer opened agreement", clock.instant().minus(Duration.ofMinutes(9))),
                    new CorrelationEvent("eRA", "corr-489965957", "Customer signed agreement", clock.instant().minus(Duration.ofMinutes(7))),
                    new CorrelationEvent("STL", "corr-489965957", "STL submit success", clock.instant().minus(Duration.ofMinutes(7))),
                    new CorrelationEvent("RMS", "corr-489965957", "RMS signed status persisted", clock.instant().minus(Duration.ofMinutes(6))),
                    new CorrelationEvent("TAS", "corr-489965957", "TAS API timeout", clock.instant().minus(Duration.ofMinutes(5))),
                    new CorrelationEvent("TAS", "corr-489965957", "Retry failed with timeout", clock.instant().minus(Duration.ofMinutes(3)))
            );
        }
        return List.of(
                new CorrelationEvent("STL", "corr-" + raId, "Submission metadata retrieved", clock.instant().minus(Duration.ofMinutes(30))),
                new CorrelationEvent("TAS", "corr-" + raId, "Agreement status retrieved", clock.instant().minus(Duration.ofMinutes(20)))
        );
    }

    public EraSessionStatus era(String raId) {
        return switch (raId) {
            case "489965957" -> new EraSessionStatus(raId, "SIGNED", clock.instant().minus(Duration.ofMinutes(9)), clock.instant().minus(Duration.ofMinutes(7)), "SIGNATURE_COMPLETE", true, true);
            case "123456" -> new EraSessionStatus(raId, "BLOCKED", clock.instant().minus(Duration.ofMinutes(20)), clock.instant().minus(Duration.ofMinutes(15)), "LICENSE_SCAN", false, true);
            case "777" -> new EraSessionStatus(raId, "ABANDONED", clock.instant().minus(Duration.ofMinutes(34)), clock.instant().minus(Duration.ofMinutes(31)), "SIGNATURE", true, true);
            case "500" -> new EraSessionStatus(raId, "SIGNED", clock.instant().minus(Duration.ofMinutes(10)), clock.instant().minus(Duration.ofMinutes(4)), "SIGNATURE_COMPLETE", true, true);
            default -> new EraSessionStatus(raId, "UNKNOWN", null, null, "UNKNOWN", false, false);
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
                    Map.of("RMS_503", 147, "TAS_TIMEOUT", 21),
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
