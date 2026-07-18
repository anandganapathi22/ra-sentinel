package com.rasentinel.agent.core;

import com.rasentinel.agent.tools.records.ContractVaultStatus;
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
        evidence.add("Fleet ledger status is " + snapshot.fleet().status());
        evidence.add("Counter console state is " + snapshot.console().state());
        evidence.add("Submission gateway status is " + snapshot.gateway().submitStatus());
        evidence.add("Contract vault agreement state is " + snapshot.vault().state());
        evidence.add("S3 signed PDF present: " + snapshot.s3().present());
        evidence.add("Keyspace metadata present: " + snapshot.keyspace().present());

        if (!snapshot.fleet().exists()) {
            return new Diagnosis(
                    "not_found",
                    "RA was not found in the fleet ledger",
                    evidence,
                    "Verify the RA number and open a counter support ticket if the rental exists outside the fleet ledger.",
                    "high"
            );
        }

        if (isExpiredVaultHash(snapshot.vault())) {
            evidence.add("Contract vault signing hash expired at " + snapshot.vault().hashExpiresAt());
            return new Diagnosis(
                    "blocked",
                    "Contract vault signing hash expired",
                    evidence,
                    "Regenerate the contract vault hash and resend the signing link after human approval.",
                    "high"
            );
        }

        if (!snapshot.gateway().email().equalsIgnoreCase(snapshot.fleet().customerEmail())) {
            evidence.add("Fleet ledger customer email is " + snapshot.fleet().customerEmail());
            evidence.add("Submission gateway signing email is " + snapshot.gateway().email());
            return new Diagnosis(
                    "blocked",
                    "Customer email mismatch between the fleet ledger and submission gateway",
                    evidence,
                    "Correct the email source of truth and resend the signing link after approval.",
                    "high"
            );
        }

        if (!snapshot.fleet().language().equalsIgnoreCase(snapshot.gateway().language())) {
            evidence.add("Fleet ledger language is " + snapshot.fleet().language());
            evidence.add("Submission gateway language is " + snapshot.gateway().language());
            return new Diagnosis(
                    "blocked",
                    "Agreement language mismatch",
                    evidence,
                    "Regenerate the signing package from the deterministic submission gateway template after approval.",
                    "medium"
            );
        }

        if ("SIGNED".equalsIgnoreCase(snapshot.vault().state()) && !snapshot.s3().present()) {
            return new Diagnosis(
                    "incident",
                    "Signed PDF is missing from S3",
                    evidence,
                    "Open an ops incident with contract vault and submission gateway correlation evidence; do not recreate the legal agreement manually.",
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

        if ("SUBMIT_FAILED".equalsIgnoreCase(snapshot.gateway().submitStatus())) {
            return new Diagnosis(
                    "incident",
                    "Submission gateway submit failed",
                    evidence,
                    "Retry submission gateway submit after approval and include correlation IDs in the incident trail.",
                    "high"
            );
        }

        if ("SIGNED".equalsIgnoreCase(snapshot.vault().state()) && snapshot.s3().present() && snapshot.keyspace().present()) {
            return new Diagnosis(
                    "complete",
                    "Agreement appears complete across the contract vault, S3, and Keyspace",
                    evidence,
                    "No signing action is recommended. Ask counter console/fleet ledger owners to investigate stale counter display if the UI still shows pending.",
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

    private boolean isExpiredVaultHash(ContractVaultStatus vault) {
        return vault.hashExpiresAt() != null && vault.hashExpiresAt().isBefore(clock.instant());
    }
}
