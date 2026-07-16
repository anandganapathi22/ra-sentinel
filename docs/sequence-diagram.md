# RA Sentinel Sequence Diagram

This sequence shows how RA Sentinel works around the deterministic legal signing
flow. The agent investigates exceptions, builds evidence, and recommends actions.
It does not alter legal text, charges, signatures, or final agreement state.

```mermaid
sequenceDiagram
    autonumber
    actor Counter as Counter/Dash User
    participant API as RA Sentinel API
    participant Router as Agent Router
    participant Agent as Selected Ops Agent
    participant Tools as Tool Interfaces
    participant ERA as eRA
    participant STL as STL
    participant RMS as RMS
    participant Dash as Dash
    participant TAS as TAS
    participant S3 as S3/PDF Archive
    participant Keyspace as Keyspace/Audit DB
    participant Logs as Logs/Correlation Events
    participant Policy as Guardrail Policy
    participant Audit as Audit Store
    participant PG as PostgreSQL<br/>source tables + agent_audit_runs
    participant Human as Human Approver
    participant Ticket as Incident/Audit Ticket

    Counter->>API: POST /api/ops-agents/{agent}<br/>raId, location, question
    API->>Router: Validate request and route by endpoint
    Router->>Agent: Start investigation

    Agent->>Tools: Read-only evidence collection
    par Query eRA
        Tools->>PG: select era_session_status
        Tools->>ERA: getSessionStatus(raId)
        ERA-->>Tools: eRA status, last step, activity
    and Query STL
        Tools->>PG: select stl_submission_metadata
        Tools->>STL: getSubmissionMetadata(raId)
        STL-->>Tools: submit status, email, language, correlation ID
    and Query RMS
        Tools->>PG: select rms_rental_agreements
        Tools->>RMS: getRentalAgreement(raId)
        RMS-->>Tools: RA status, customer data
    and Query Dash
        Tools->>PG: select dash_counter_states
        Tools->>Dash: getCounterState(raId)
        Dash-->>Tools: counter workflow state
    and Query TAS
        Tools->>PG: select tas_agreement_status
        Tools->>TAS: getAgreementStatus(raId)
        TAS-->>Tools: agreement state, hash, agreement ID
    and Query S3
        Tools->>PG: select s3_signed_pdf_status
        Tools->>S3: getSignedPdfStatus(raId)
        S3-->>Tools: PDF present/missing, bucket, object key
    and Query Keyspace
        Tools->>PG: select keyspace_submission_records
        Tools->>Keyspace: getSubmissionRecord(raId)
        Keyspace-->>Tools: metadata present/missing, sync status
    and Query Logs
        Tools->>PG: select correlation_events
        Tools->>Logs: getEvents(raId)
        Logs-->>Tools: ordered correlation events
    end

    Tools-->>Agent: Evidence snapshot
    Agent->>Agent: Correlate evidence and build timeline
    Agent->>Agent: Classify root cause
    Agent->>Policy: Apply legal and operational guardrails
    Policy-->>Agent: Allowed actions and blocked actions
    Agent->>Audit: Persist run, evidence, tool results, recommendation

    alt Docker profile
        Audit->>PG: insert agent_audit_runs
        PG-->>Audit: saved
    else Local/default profile
        Audit->>Audit: save in memory
    end

    alt Happy path complete
        Agent-->>API: Report complete/no action
        API-->>Counter: Agreement appears complete across systems
    else TAS timeout after successful signing
        Agent-->>API: Root cause: TAS API timeout
        API-->>Counter: Recommend TAS resubmit after approval
        Counter->>Human: Request approval
        Human-->>Counter: Approve retry
        Counter->>Ticket: Open incident with evidence/timeline
    else PDF upload failed or PDF missing from S3
        Agent-->>API: Root cause: signed PDF missing from S3
        API-->>Counter: Recommend audit ticket and PDF archive retry after approval
        Counter->>Human: Request approval
        Human-->>Counter: Approve archive retry
        Counter->>Ticket: Open audit/ops ticket with TAS/STL/S3 evidence
    else Customer abandoned signing
        Agent-->>API: Root cause: customer abandoned at signing step
        API-->>Counter: Recommend resend/regenerate link after approval
    else Counter prerequisite missing
        Agent-->>API: Root cause: missing prerequisite, e.g. license scan
        API-->>Counter: Recommend counter action, e.g. rescan license
    else Platform incident
        Agent-->>API: Root cause: RMS/TAS/Dash health issue
        API-->>Counter: Severity, affected locations, suggested runbook action
        Counter->>Ticket: Open incident
    end
```

## Legal Boundary

The agent is blocked from:

- modifying legal text
- changing charges
- signing for the customer
- submitting a legal agreement autonomously

Any write operation, such as resending a signing link, retrying TAS submission,
retrying PDF archival, or failing over an endpoint, requires human approval.

## Local Production-Like Docker Sequence

This sequence shows the local Docker runtime with PostgreSQL-backed audit
persistence.

```mermaid
sequenceDiagram
    autonumber
    actor Dev as Developer
    participant Compose as Docker Compose
    participant PG as ra-sentinel-postgres
    participant App as ra-sentinel App
    participant Health as Actuator Health
    participant API as Agent API
    participant Tools as JDBC Tool Adapters
    participant Audit as JDBC Audit Store

    Dev->>Compose: docker compose up --build -d
    Compose->>PG: Start PostgreSQL
    PG-->>Compose: Healthy
    Compose->>App: Start Spring Boot with docker profile
    App->>PG: Run schema.sql<br/>create and seed source/audit tables
    App->>Health: Register DB health contributor
    App-->>Compose: Healthy

    Dev->>API: POST /api/agent/ra-completion
    API->>Tools: Load RA source state
    Tools->>PG: select source tables
    PG-->>Tools: eRA/RMS/Dash/STL/TAS/S3/Keyspace/log data
    API->>Audit: Save diagnostic run
    Audit->>PG: insert into agent_audit_runs
    PG-->>Audit: saved
    API-->>Dev: Diagnosis response

    Dev->>API: GET /api/agent/audit
    API->>Audit: Load persisted runs
    Audit->>PG: select from agent_audit_runs
    PG-->>Audit: rows
    API-->>Dev: Audit history
```
