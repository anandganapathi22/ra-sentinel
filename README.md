# RA Sentinel

RA Sentinel is a read-only agentic operations assistant for rental agreement
systems. It sits beside the legal signing flow and investigates exceptions
without changing contract text, charges, signatures, or final submission state.

## Process Boundary

The signing path remains deterministic:

```text
Signing Portal -> Submission Gateway -> Fleet Ledger/Counter Console -> Contract Vault -> S3/Keyspace
```

Agents operate around that path:

```text
Counter/Console user
  -> RA Sentinel Agent API
      -> Fleet ledger lookup
      -> Counter console state lookup
      -> Submission gateway metadata lookup
      -> Contract vault agreement/hash lookup
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
      -> source-state tables for the signing portal/fleet ledger/counter
         console/submission gateway/contract vault/S3/Keyspace/logs/health
      -> agent_audit_runs table
      -> persistent Docker volume
```

## How This Project Works

RA Sentinel is a Spring Boot service that exposes focused agent endpoints for
rental operations support. Each agent follows the same pattern:

```text
1. Accept an RA/location/question
2. Query read-only tools for the signing portal, submission gateway, fleet
   ledger, counter console, contract vault, S3, Keyspace, logs, and health
3. Correlate the system evidence
4. Classify the likely root cause
5. Return a structured report with evidence, timeline, recommended action, and guardrails
6. Require human approval for any action that changes system state
```

The default Maven profile uses in-memory tool adapters so unit/integration tests
can run without infrastructure. The Docker profile uses PostgreSQL-backed tool
adapters, so all source data for the signing portal, submission gateway, fleet
ledger, counter console, contract vault, S3, Keyspace, logs, health, and audit
history flows from the database.

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
  "question": "Customer completed signing but agreement is not visible in the contract vault."
}
```

| Agent | Endpoint |
| --- | --- |
| Rental Agreement Troubleshooting Agent | `POST /api/ops-agents/ra-troubleshooting` |
| Incident Management Agent | `POST /api/ops-agents/incident-management` |
| Customer Completion Recovery Agent | `POST /api/ops-agents/completion-recovery` |
| Counter Support Copilot | `POST /api/ops-agents/counter-copilot` |
| Compliance & Audit Agent | `POST /api/ops-agents/compliance-audit` |
| Contract Vault/Fleet Ledger Health Monitoring Agent | `POST /api/ops-agents/health-monitoring` |
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
signing_portal_sessions
fleet_ledger_agreements
counter_console_states
submission_gateway_metadata
contract_vault_status
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
  -Body '{"raId":"123","question":"Why cannot this customer finish signing?"}'

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
  -Body '{"raId":"489965957","location":"ORD","question":"Customer completed signing but agreement is not visible in the contract vault."}'
```

## Example: PDF Upload Failure

Scenario:

```text
Customer signs successfully in the contract vault.
Submission gateway/fleet ledger submit succeeds.
The signed PDF does not upload to S3, or the S3 PUT fails.
```

The agent handles this as an operational/compliance exception, not as a new
signing event.

The agent checks:

```text
Contract vault agreement state
Submission gateway submit status
Fleet ledger/counter console status
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
- Contract vault state is SIGNED
- Submission gateway status is SUBMITTED
- S3 signed PDF is not present
- Keyspace metadata is missing or stale

Recommended Action:
Create an audit/ops ticket and retry PDF archival from the original signed
contract vault artifact after human approval.
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

- `123`: contract vault signing hash expired
- `200`: contract vault signed but PDF missing from S3
- `300`: signed PDF exists but Keyspace metadata is missing
- `400`: customer email mismatch
- `489965957`: signed successfully, S3 PDF exists, submission gateway succeeded, contract vault timeout
- `123456`: license scan missing
- `777`: customer abandoned at signature step
- `500`: complete agreement happy path

Seed location examples:

- `Chicago` / `ORD`: fleet ledger unavailable, contract vault latency high, ORD and MDW affected

## Guardrails

The agent is intentionally blocked from:

- modifying legal text
- changing charges
- signing for the customer
- submitting a legal agreement autonomously

Allowed actions are recommendations or approval-gated operations such as opening
an incident, resending a link, regenerating a hash, or retrying sync.

## AI Reasoning Layer

Each of the 7 `ops-agents` still gathers evidence exclusively from the deterministic
read-only tools above — the AI never queries a system directly. Once evidence is
gathered, the agent asks Claude (`claude-opus-4-8` by default) to synthesize the
`severity` / `rootCause` / `recommendedAction` from that evidence instead of the
original hand-written `if/else`.

Two guardrails hold regardless of what the model returns:

- `allowedActions` is always intersected with a fixed, per-agent action vocabulary
  before it reaches the report — an out-of-vocabulary or hallucinated action string
  is silently dropped, never surfaced.
- `blockedActions` (`MODIFY_LEGAL_TEXT`, `CHANGE_CHARGES`, `SIGN_FOR_CUSTOMER`,
  `SUBMIT_LEGAL_AGREEMENT_AUTONOMOUSLY`) is appended by `AgentReportFactory`
  unconditionally, the same way it was before this layer existed.

Configure with:

```powershell
$env:RA_SENTINEL_AI_API_KEY = "sk-ant-..."
```

If `RA_SENTINEL_AI_API_KEY` is unset, every agent falls back to its original
deterministic `if/else` — this is the default in local dev and in the test suite,
so no API key is required to build or test the project. Model, effort, and timeout
are configurable via `rasentinel.ai.model` / `rasentinel.ai.effort` /
`rasentinel.ai.timeout-ms` in `application.yml`.

## Project History (Phasewise)

1. **Build RA Sentinel operations assistant** — initial Spring Boot service:
   the 7 `ops-agents` endpoints, the `RaCompletionAgent`/`RaCompletionClassifier`
   deterministic diagnosis path, in-memory tool adapters and seed data, the
   audit store abstraction (in-memory + JDBC), Docker/Compose setup, and the
   first pass of docs, sequence diagram, and test cases.
2. **Read operational agent data from Postgres** — added `JdbcOperationalTools`
   and the full `schema.sql` source-state tables so the `docker` Spring profile
   reads signing/fleet/submission/vault/S3/Keyspace/log/health data from
   PostgreSQL instead of in-memory seeds, with docs and test cases updated to
   match.
3. **Add IDE editor tabs and data source config** — checked in `.idea` editor
   tab and PostgreSQL data source config for local development convenience.
4. **Add LLM reasoning layer to the ops agents, with deterministic fallback** —
   introduced `AnthropicAiReasoningClient` and `RaSentinelAiProperties` so each
   agent asks Claude to synthesize `severity`/`rootCause`/`recommendedAction`
   from the same evidence the deterministic tools gather, while keeping the
   original `if/else` as an automatic fallback when no API key is configured.
5. **Reflect the AI reasoning layer in the sequence diagram and test-case
   docs** — updated `docs/sequence-diagram.md` and `docs/test-cases.md` to
   document the new AI reasoning step and guardrails.
6. **Rename eRA/STL/RMS/Dash/TAS to generic system names** — renamed the
   internal tool classes and data records to vendor-neutral names
   (`SigningPortalTool`, `SubmissionGatewayTool`, `FleetLedgerTool`,
   `CounterConsoleTool`, `ContractVaultTool`) across code, schema, docs, and
   scripts, with no behavior change.
