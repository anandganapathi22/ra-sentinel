package com.rasentinel.agent.core;

import com.rasentinel.agent.tools.records.CorrelationEvent;
import com.rasentinel.agent.tools.records.DashCounterState;
import com.rasentinel.agent.tools.records.KeyspaceSubmissionRecord;
import com.rasentinel.agent.tools.records.RmsRentalAgreement;
import com.rasentinel.agent.tools.records.S3SignedPdfStatus;
import com.rasentinel.agent.tools.records.StlSubmissionMetadata;
import com.rasentinel.agent.tools.records.TasAgreementStatus;
import java.util.List;

public record RaSnapshot(
        RmsRentalAgreement rms,
        DashCounterState dash,
        StlSubmissionMetadata stl,
        TasAgreementStatus tas,
        S3SignedPdfStatus s3,
        KeyspaceSubmissionRecord keyspace,
        List<CorrelationEvent> correlationEvents
) {
}
