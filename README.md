# RA Sentinel

RA Sentinel is a read-only agentic operations assistant for rental agreement
systems. It sits beside the legal signing flow and investigates exceptions
without changing contract text, charges, signatures, or final submission state.

## Process Boundary

The signing path remains deterministic:

```text
eRA -> STL -> RMS/Dash -> TAS -> S3/Keyspace
```

Agents operate around that path:

```text
Counter/Dash user
  -> RA Sentinel Agent API
      -> RMS lookup
      -> Dash state lookup
      -> STL metadata lookup
      -> TAS agreement/hash lookup
      -> S3 signed PDF check
      -> Keyspace metadata check
      -> Correlation event lookup
  -> Evidence-backed recommendation
  -> Human approval for any write action
```

Local production-like runtime:

```text
Docker Compose
  -> ra-sentinel Spring Boot service
      -> docker Spring profile
      -> PostgreSQL-backed operational data and audit store
  -> ra-sentinel-postgres
      -> source-state tables for eRA/RMS/Dash/STL/TAS/S3/Keyspace/logs/health
      -> agent_audit_runs table
      -> persistent Docker volume
```

## How This Project Works

RA Sentinel is a Spring Boot service that exposes focused agent endpoints for
rental operations support. Each agent follows the same pattern:

```text
1. Accept an RA/location/question
2. Query read-only tools for eRA, STL, RMS, Dash, TAS, S3, Keyspace, logs, and health
3. Correlate the system evidence
4. Classify the likely root cause
5. Return a structured report with evidence, timeline, recommended action, and guardrails
6. Require human approval for any action that changes system state
```

The default Maven profile uses in-memory tool adapters so unit/integration tests
can run without infrastructure. The Docker profile uses PostgreSQL-backed tool
adapters, so all source data for eRA, STL, RMS, Dash, TAS, S3, Keyspace, logs,
health, and audit history flows from the database.

Production integrations can replace the PostgreSQL seed/source tables with real
clients for Hertz systems, or keep the same table-backed adapter pattern if an
ops data mart is available.

Data storage depends on the runtime profile:

| Runtime | Agent source data | Audit Store |
| --- | --- |
| Local Maven/default profile | In-memory seed tools | In-memory audit store |
| Docker `docker` profile | PostgreSQL operational tables | PostgreSQL `agent_audit_runs` table |

The agent does not own the legal signing transaction. It only investigates
exceptions and recommends operational actions.

Full project sequence diagram:

[docs/sequence-diagram.md](docs/sequence-diagram.md)

Manual and automated test cases:

[docs/test-cases.md](docs/test-cases.md)

Endpoint regression script:

```powershell
.\scripts\run-test-cases.ps1 -BaseUrl http://localhost:8081
```

## Agent Endpoints

All endpoints accept:

```json
{
  "raId": "489965957",
  "location": "ORD",
  "question": "Customer completed eRA but agreement is not visible in TAS."
}
```

| Agent | Endpoint |
| --- | --- |
| Rental Agreement Troubleshooting Agent | `POST /api/ops-agents/ra-troubleshooting` |
| Incident Management Agent | `POST /api/ops-agents/incident-management` |
| Customer Completion Recovery Agent | `POST /api/ops-agents/completion-recovery` |
| Counter Support Copilot | `POST /api/ops-agents/counter-copilot` |
| Compliance & Audit Agent | `POST /api/ops-agents/compliance-audit` |
| TAS/RMS Health Monitoring Agent | `POST /api/ops-agents/health-monitoring` |
| End-to-End Transaction Investigation Agent | `POST /api/ops-agents/transaction-investigation` |

## Run

```powershell
mvn spring-boot:run
```

## Run With Docker

The Docker setup is production-shaped for local testing:

```text
ra-sentinel app container
  -> PostgreSQL container
      -> persistent agent audit table
```

Build and start:

```powershell
docker compose up --build
```

Run in the background:

```powershell
docker compose up --build -d
```

Check health:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

When running with `RA_SENTINEL_PORT=8081`:

```powershell
Invoke-RestMethod http://localhost:8081/actuator/health
```

Stop:

```powershell
docker compose down
```

Use a different host port if `8080` is already taken:

```powershell
$env:RA_SENTINEL_PORT=8081
docker compose up --build
```

PostgreSQL is exposed on host port `5433` by default:

```text
host: localhost
port: 5433
database: ra_sentinel
username: ra_sentinel
password: ra_sentinel
```

Adminer is available as a free browser UI for PostgreSQL:

```text
http://localhost:8082
```

Adminer login:

```text
System: PostgreSQL
Server: postgres
Username: ra_sentinel
Password: ra_sentinel
Database: ra_sentinel
```

The app uses the `docker` Spring profile inside Compose. In that profile, agent
source data and audit runs are stored in PostgreSQL.

Important tables:

```text
era_session_status
rms_rental_agreements
dash_counter_states
stl_submission_metadata
tas_agreement_status
s3_signed_pdf_status
keyspace_submission_records
correlation_events
system_health_snapshots
agent_audit_runs
```

Verify audit persistence:

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri http://localhost:8081/api/agent/ra-completion `
  -ContentType "application/json" `
  -Body '{"raId":"123","question":"Why cannot this customer finish eRA?"}'

Invoke-RestMethod http://localhost:8081/api/agent/audit

docker --context default exec ra-sentinel-postgres `
  psql -U ra_sentinel -d ra_sentinel `
  -c "select run_id, ra_id, status, likely_cause, created_at from agent_audit_runs order by created_at desc limit 5;"
```

Stop and keep PostgreSQL data:

```powershell
docker compose down
```

Stop and remove PostgreSQL data:

```powershell
docker compose down -v
```

## Example: RA Troubleshooting

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri http://localhost:8080/api/ops-agents/ra-troubleshooting `
  -ContentType "application/json" `
  -Body '{"raId":"489965957","location":"ORD","question":"Customer completed eRA but agreement is not visible in TAS."}'
```

## Example: PDF Upload Failure

Scenario:

```text
Customer signs successfully in TAS.
STL/RMS submit succeeds.
The signed PDF does not upload to S3, or the S3 PUT fails.
```

The agent handles this as an operational/compliance exception, not as a new
signing event.

The agent checks:

```text
TAS agreement state
STL submit status
RMS/Dash status
S3 PDF presence
Keyspace metadata
Correlation events/logs
```

Expected diagnosis:

```text
Severity: High

Root Cause:
Customer signed successfully, but the signed PDF is missing from S3.

Evidence:
- TAS state is SIGNED
- STL submit status is SUBMITTED
- S3 signed PDF is not present
- Keyspace metadata is missing or stale

Recommended Action:
Create an audit/ops ticket and retry PDF archival from the original signed TAS
artifact after human approval.
```

Safe actions:

```text
OPEN_AUDIT_TICKET
RETRY_SYNC_AFTER_APPROVAL
RETRY_PDF_ARCHIVE_AFTER_APPROVAL
```

Blocked actions:

```text
MODIFY_LEGAL_TEXT
CHANGE_CHARGES
SIGN_FOR_CUSTOMER
SUBMIT_LEGAL_AGREEMENT_AUTONOMOUSLY
```

Run the seeded example with RA `200`:

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri http://localhost:8080/api/ops-agents/compliance-audit `
  -ContentType "application/json" `
  -Body '{"raId":"200","location":"DFW","question":"Check audit state for missing signed PDF."}'
```

If running through Docker on host port `8081`, use:

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri http://localhost:8081/api/ops-agents/compliance-audit `
  -ContentType "application/json" `
  -Body '{"raId":"200","location":"DFW","question":"Check audit state for missing signed PDF."}'
```

Seed RA examples:

- `123`: TAS signing hash expired
- `200`: TAS signed but PDF missing from S3
- `300`: signed PDF exists but Keyspace metadata is missing
- `400`: customer email mismatch
- `489965957`: signed successfully, S3 PDF exists, STL succeeded, TAS timeout
- `123456`: license scan missing
- `777`: customer abandoned at signature step
- `500`: complete agreement happy path

Seed location examples:

- `Chicago` / `ORD`: RMS unavailable, TAS latency high, ORD and MDW affected

## Guardrails

The agent is intentionally blocked from:

- modifying legal text
- changing charges
- signing for the customer
- submitting a legal agreement autonomously

Allowed actions are recommendations or approval-gated operations such as opening
an incident, resending a link, regenerating a hash, or retrying sync.
