package com.rasentinel.agent.core;

import com.rasentinel.agent.api.RaCompletionRequest;
import com.rasentinel.agent.api.RaCompletionResponse;
import com.rasentinel.agent.audit.AgentAuditStore;
import com.rasentinel.agent.tools.CorrelationTool;
import com.rasentinel.agent.tools.DashTool;
import com.rasentinel.agent.tools.KeyspaceTool;
import com.rasentinel.agent.tools.RmsTool;
import com.rasentinel.agent.tools.S3DocumentTool;
import com.rasentinel.agent.tools.StlTool;
import com.rasentinel.agent.tools.TasTool;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RaCompletionAgent {
    private final RmsTool rmsTool;
    private final DashTool dashTool;
    private final StlTool stlTool;
    private final TasTool tasTool;
    private final S3DocumentTool s3DocumentTool;
    private final KeyspaceTool keyspaceTool;
    private final CorrelationTool correlationTool;
    private final RaCompletionClassifier classifier;
    private final AgentAuditStore auditStore;
    private final Clock clock;

    public RaCompletionAgent(
            RmsTool rmsTool,
            DashTool dashTool,
            StlTool stlTool,
            TasTool tasTool,
            S3DocumentTool s3DocumentTool,
            KeyspaceTool keyspaceTool,
            CorrelationTool correlationTool,
            RaCompletionClassifier classifier,
            AgentAuditStore auditStore,
            Clock clock
    ) {
        this.rmsTool = rmsTool;
        this.dashTool = dashTool;
        this.stlTool = stlTool;
        this.tasTool = tasTool;
        this.s3DocumentTool = s3DocumentTool;
        this.keyspaceTool = keyspaceTool;
        this.correlationTool = correlationTool;
        this.classifier = classifier;
        this.auditStore = auditStore;
        this.clock = clock;
    }

    public RaCompletionResponse diagnose(RaCompletionRequest request) {
        String raId = request.raId().trim();
        var snapshot = new RaSnapshot(
                rmsTool.getRentalAgreement(raId),
                dashTool.getCounterState(raId),
                stlTool.getSubmissionMetadata(raId),
                tasTool.getAgreementStatus(raId),
                s3DocumentTool.getSignedPdfStatus(raId),
                keyspaceTool.getSubmissionRecord(raId),
                correlationTool.getEvents(raId)
        );
        var diagnosis = classifier.classify(snapshot);
        var toolResults = new LinkedHashMap<String, Object>();
        toolResults.put("rms", snapshot.rms());
        toolResults.put("dash", snapshot.dash());
        toolResults.put("stl", snapshot.stl());
        toolResults.put("tas", snapshot.tas());
        toolResults.put("s3", snapshot.s3());
        toolResults.put("keyspace", snapshot.keyspace());
        toolResults.put("correlationEvents", snapshot.correlationEvents());

        var response = new RaCompletionResponse(
                UUID.randomUUID(),
                raId,
                diagnosis.status(),
                diagnosis.likelyCause(),
                diagnosis.evidence(),
                diagnosis.recommendedAction(),
                diagnosis.confidence(),
                true,
                List.of("OPEN_INCIDENT", "RESEND_LINK_AFTER_APPROVAL", "REGENERATE_HASH_AFTER_APPROVAL", "RETRY_SYNC_AFTER_APPROVAL"),
                List.of("MODIFY_LEGAL_TEXT", "CHANGE_CHARGES", "SIGN_FOR_CUSTOMER", "SUBMIT_LEGAL_AGREEMENT_AUTONOMOUSLY"),
                toolResults,
                clock.instant()
        );
        auditStore.save(response);
        return response;
    }
}
