package com.rasentinel.agent.core;

import com.rasentinel.agent.tools.records.CorrelationEvent;
import com.rasentinel.agent.tools.records.CounterConsoleState;
import com.rasentinel.agent.tools.records.ContractVaultStatus;
import com.rasentinel.agent.tools.records.FleetLedgerAgreement;
import com.rasentinel.agent.tools.records.KeyspaceSubmissionRecord;
import com.rasentinel.agent.tools.records.S3SignedPdfStatus;
import com.rasentinel.agent.tools.records.SubmissionGatewayMetadata;
import java.util.List;

public record RaSnapshot(
        FleetLedgerAgreement fleet,
        CounterConsoleState console,
        SubmissionGatewayMetadata gateway,
        ContractVaultStatus vault,
        S3SignedPdfStatus s3,
        KeyspaceSubmissionRecord keyspace,
        List<CorrelationEvent> correlationEvents
) {
}
