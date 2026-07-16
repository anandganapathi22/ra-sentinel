package com.rasentinel.agent.core;

import com.rasentinel.agent.tools.records.TasAgreementStatus;
import java.time.Clock;
import java.util.ArrayList;
import org.springframework.stereotype.Component;

@Component
public class RaCompletionClassifier {
    private final Clock clock;

    public RaCompletionClassifier(Clock clock) {
        this.clock = clock;
    }

    public Diagnosis classify(RaSnapshot snapshot) {
        var evidence = new ArrayList<String>();
        evidence.add("RMS status is " + snapshot.rms().status());
        evidence.add("Dash counter state is " + snapshot.dash().state());
        evidence.add("STL submit status is " + snapshot.stl().submitStatus());
        evidence.add("TAS agreement state is " + snapshot.tas().state());
        evidence.add("S3 signed PDF present: " + snapshot.s3().present());
        evidence.add("Keyspace metadata present: " + snapshot.keyspace().present());

        if (!snapshot.rms().exists()) {
            return new Diagnosis(
                    "not_found",
                    "RA was not found in RMS",
                    evidence,
                    "Verify the RA number and open a counter support ticket if the rental exists outside RMS.",
                    "high"
            );
        }

        if (isExpiredTasHash(snapshot.tas())) {
            evidence.add("TAS signing hash expired at " + snapshot.tas().hashExpiresAt());
            return new Diagnosis(
                    "blocked",
                    "TAS signing hash expired",
                    evidence,
                    "Regenerate the TAS hash and resend the signing link after human approval.",
                    "high"
            );
        }

        if (!snapshot.stl().email().equalsIgnoreCase(snapshot.rms().customerEmail())) {
            evidence.add("RMS customer email is " + snapshot.rms().customerEmail());
            evidence.add("STL signing email is " + snapshot.stl().email());
            return new Diagnosis(
                    "blocked",
                    "Customer email mismatch between RMS and STL",
                    evidence,
                    "Correct the email source of truth and resend the signing link after approval.",
                    "high"
            );
        }

        if (!snapshot.rms().language().equalsIgnoreCase(snapshot.stl().language())) {
            evidence.add("RMS language is " + snapshot.rms().language());
            evidence.add("STL language is " + snapshot.stl().language());
            return new Diagnosis(
                    "blocked",
                    "Agreement language mismatch",
                    evidence,
                    "Regenerate the signing package from the deterministic STL template after approval.",
                    "medium"
            );
        }

        if ("SIGNED".equalsIgnoreCase(snapshot.tas().state()) && !snapshot.s3().present()) {
            return new Diagnosis(
                    "incident",
                    "Signed PDF is missing from S3",
                    evidence,
                    "Open an ops incident with TAS and STL correlation evidence; do not recreate the legal agreement manually.",
                    "high"
            );
        }

        if (snapshot.s3().present() && !snapshot.keyspace().present()) {
            return new Diagnosis(
                    "incident",
                    "Signed PDF exists but Keyspace metadata is missing",
                    evidence,
                    "Retry metadata sync after approval and attach the S3 object key to the incident.",
                    "high"
            );
        }

        if ("SUBMIT_FAILED".equalsIgnoreCase(snapshot.stl().submitStatus())) {
            return new Diagnosis(
                    "incident",
                    "STL submit failed",
                    evidence,
                    "Retry STL submit after approval and include correlation IDs in the incident trail.",
                    "high"
            );
        }

        if ("SIGNED".equalsIgnoreCase(snapshot.tas().state()) && snapshot.s3().present() && snapshot.keyspace().present()) {
            return new Diagnosis(
                    "complete",
                    "Agreement appears complete across TAS, S3, and Keyspace",
                    evidence,
                    "No signing action is recommended. Ask Dash/RMS owners to investigate stale counter display if the UI still shows pending.",
                    "medium"
            );
        }

        return new Diagnosis(
                "pending",
                "Customer has not completed the deterministic signing flow",
                evidence,
                "Wait for customer completion or resend the existing link after human approval.",
                "medium"
        );
    }

    private boolean isExpiredTasHash(TasAgreementStatus tas) {
        return tas.hashExpiresAt() != null && tas.hashExpiresAt().isBefore(clock.instant());
    }
}
