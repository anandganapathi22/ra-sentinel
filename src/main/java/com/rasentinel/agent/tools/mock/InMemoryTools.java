package com.rasentinel.agent.tools.mock;

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
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!docker")
public class InMemoryTools implements FleetLedgerTool, CounterConsoleTool, SubmissionGatewayTool, ContractVaultTool, S3DocumentTool, KeyspaceTool, CorrelationTool, SigningPortalTool, OperationalHealthTool {
    private final InMemoryRaToolData data;

    public InMemoryTools(InMemoryRaToolData data) {
        this.data = data;
    }

    @Override
    public FleetLedgerAgreement getRentalAgreement(String raId) {
        return data.fleetLedger(raId);
    }

    @Override
    public CounterConsoleState getCounterState(String raId) {
        return data.counterConsole(raId);
    }

    @Override
    public SubmissionGatewayMetadata getSubmissionMetadata(String raId) {
        return data.submissionGateway(raId);
    }

    @Override
    public ContractVaultStatus getAgreementStatus(String raId) {
        return data.contractVault(raId);
    }

    @Override
    public S3SignedPdfStatus getSignedPdfStatus(String raId) {
        return data.s3(raId);
    }

    @Override
    public KeyspaceSubmissionRecord getSubmissionRecord(String raId) {
        return data.keyspace(raId);
    }

    @Override
    public List<CorrelationEvent> getEvents(String raId) {
        return data.events(raId);
    }

    @Override
    public SigningPortalSession getSessionStatus(String raId) {
        return data.signingPortal(raId);
    }

    @Override
    public SystemHealthSnapshot getHealth(String location) {
        return data.health(location);
    }
}
