package com.rasentinel.agent.tools.mock;

import com.rasentinel.agent.tools.CorrelationTool;
import com.rasentinel.agent.tools.DashTool;
import com.rasentinel.agent.tools.EraTool;
import com.rasentinel.agent.tools.KeyspaceTool;
import com.rasentinel.agent.tools.OperationalHealthTool;
import com.rasentinel.agent.tools.RmsTool;
import com.rasentinel.agent.tools.S3DocumentTool;
import com.rasentinel.agent.tools.StlTool;
import com.rasentinel.agent.tools.TasTool;
import com.rasentinel.agent.tools.records.CorrelationEvent;
import com.rasentinel.agent.tools.records.DashCounterState;
import com.rasentinel.agent.tools.records.EraSessionStatus;
import com.rasentinel.agent.tools.records.KeyspaceSubmissionRecord;
import com.rasentinel.agent.tools.records.RmsRentalAgreement;
import com.rasentinel.agent.tools.records.S3SignedPdfStatus;
import com.rasentinel.agent.tools.records.StlSubmissionMetadata;
import com.rasentinel.agent.tools.records.SystemHealthSnapshot;
import com.rasentinel.agent.tools.records.TasAgreementStatus;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class InMemoryTools implements RmsTool, DashTool, StlTool, TasTool, S3DocumentTool, KeyspaceTool, CorrelationTool, EraTool, OperationalHealthTool {
    private final InMemoryRaToolData data;

    public InMemoryTools(InMemoryRaToolData data) {
        this.data = data;
    }

    @Override
    public RmsRentalAgreement getRentalAgreement(String raId) {
        return data.rms(raId);
    }

    @Override
    public DashCounterState getCounterState(String raId) {
        return data.dash(raId);
    }

    @Override
    public StlSubmissionMetadata getSubmissionMetadata(String raId) {
        return data.stl(raId);
    }

    @Override
    public TasAgreementStatus getAgreementStatus(String raId) {
        return data.tas(raId);
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
    public EraSessionStatus getSessionStatus(String raId) {
        return data.era(raId);
    }

    @Override
    public SystemHealthSnapshot getHealth(String location) {
        return data.health(location);
    }
}
