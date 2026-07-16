# RA Sentinel Test Cases

Use these cases to test the complete project flow. For local Maven runs, use
`localhost:8080`. For the production-like Docker stack, use `localhost:8081`.

Set a base URL once and reuse it:

```powershell
$baseUrl = "http://localhost:8081"
```

For the local production-like Docker stack:

```powershell
$env:RA_SENTINEL_PORT=8081
$env:RA_SENTINEL_POSTGRES_PORT=5433
docker --context default compose up --build -d
```

Confirm both containers are healthy:

```powershell
docker --context default compose ps
```

Expected:

```text
ra-sentinel            Up healthy   0.0.0.0:8081->8080
ra-sentinel-postgres   Up healthy   0.0.0.0:5433->5432
ra-sentinel-adminer    Up           0.0.0.0:8082->8080
```

Open Adminer:

```text
http://localhost:8082
```

Login:

```text
System: PostgreSQL
Server: postgres
Username: ra_sentinel
Password: ra_sentinel
Database: ra_sentinel
```

## Run Automated Tests

```powershell
mvn test
```

Expected:

```text
Tests run: 14, Failures: 0, Errors: 0
```

## Run Endpoint Regression Tests

The repository includes a PowerShell script that executes the documented endpoint
cases against a running app.

```powershell
.\scripts\run-test-cases.ps1 -BaseUrl http://localhost:8081
```

Expected:

```text
All cases return PASS.
```

## Smoke Test

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

Expected:

```text
status: UP
```

When running Docker, the health response should include the DB component:

```powershell
Invoke-RestMethod http://localhost:8081/actuator/health
```

Expected:

```text
status: UP
components includes db
```

## Audit Persistence Test

Purpose: prove the Docker profile persists agent audit runs into PostgreSQL.

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri http://localhost:8081/api/agent/ra-completion `
  -ContentType "application/json" `
  -Body '{"raId":"123","question":"Why cannot this customer finish eRA?"}'

Invoke-RestMethod http://localhost:8081/api/agent/audit

docker --context default exec ra-sentinel-postgres `
  psql -U ra_sentinel -d ra_sentinel `
  -c "select count(*) from agent_audit_runs;"
```

Expected:

```text
The API returns the saved audit run.
PostgreSQL count is greater than 0.
```

## Operational Data Source Test

Purpose: prove the Docker profile reads agent source data from PostgreSQL.

Update a source table, call the agent endpoint, then restore the value.

```powershell
docker --context default exec ra-sentinel-postgres `
  psql -U ra_sentinel -d ra_sentinel `
  -c "update tas_agreement_status set state = 'SIGNED' where ra_id = '489965957';"

Invoke-RestMethod `
  -Method Post `
  -Uri "$baseUrl/api/ops-agents/ra-troubleshooting" `
  -ContentType "application/json" `
  -Body '{"raId":"489965957","location":"ORD","question":"Customer completed eRA but agreement is not visible in TAS."}'

docker --context default exec ra-sentinel-postgres `
  psql -U ra_sentinel -d ra_sentinel `
  -c "update tas_agreement_status set state = 'API_TIMEOUT' where ra_id = '489965957';"
```

Expected:

```text
When TAS state is changed away from API_TIMEOUT in the database, the agent no
longer returns the TAS timeout root cause. Restoring the row restores the
original test behavior.
```

Direct table inspection:

```powershell
docker --context default exec ra-sentinel-postgres `
  psql -U ra_sentinel -d ra_sentinel `
  -c "select run_id, ra_id, status, likely_cause, created_at from agent_audit_runs order by created_at desc limit 5;"
```

## TC-01 TAS Timeout After Successful Signing

Purpose: prove the troubleshooting agent can correlate successful signing with a
downstream TAS timeout.

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "$baseUrl/api/ops-agents/ra-troubleshooting" `
  -ContentType "application/json" `
  -Body '{"raId":"489965957","location":"ORD","question":"Customer completed eRA but agreement is not visible in TAS."}'
```

Expected:

```text
severity: High
rootCause: Customer signed successfully, PDF exists in S3, STL submit succeeded, and TAS API timeout occurred.
recommendedAction: Resubmit the transaction to TAS after human approval and attach the correlation timeline.
blockedActions includes MODIFY_LEGAL_TEXT
```

## TC-02 Signed PDF Missing From S3

Purpose: prove the compliance agent treats missing PDF archival as an exception,
not as a new signing transaction.

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "$baseUrl/api/ops-agents/compliance-audit" `
  -ContentType "application/json" `
  -Body '{"raId":"200","location":"DFW","question":"Check audit state for missing signed PDF."}'
```

Expected:

```text
severity: High
rootCause: Signature is present but archived PDF is missing.
allowedActions includes OPEN_AUDIT_TICKET
blockedActions includes SUBMIT_LEGAL_AGREEMENT_AUTONOMOUSLY
```

## TC-03 PDF Exists But Audit Metadata Missing

Purpose: prove the agent separates PDF upload success from Keyspace/audit sync
failure.

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "$baseUrl/api/ops-agents/compliance-audit" `
  -ContentType "application/json" `
  -Body '{"raId":"300","location":"DFW","question":"Check audit metadata."}'
```

Expected:

```text
severity: High
rootCause: Signed PDF exists but audit metadata is missing.
allowedActions includes RETRY_SYNC_AFTER_APPROVAL
```

## TC-04 Complete Agreement Happy Path

Purpose: prove the agent can return a no-action result when TAS, S3, and
Keyspace agree.

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "$baseUrl/api/ops-agents/compliance-audit" `
  -ContentType "application/json" `
  -Body '{"raId":"500","location":"DFW","question":"Check audit state."}'
```

Expected:

```text
severity: Low
rootCause: No compliance gap detected for this RA.
requiresHumanApproval: false
allowedActions includes NO_ACTION
```

## TC-05 Customer Abandoned Signing

Purpose: prove the recovery agent detects abandoned eRA sessions.

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "$baseUrl/api/ops-agents/completion-recovery" `
  -ContentType "application/json" `
  -Body '{"raId":"777","location":"MDW","question":"Customer started eRA but never completed signing."}'
```

Expected:

```text
rootCause: Customer abandoned eRA at SIGNATURE step.
allowedActions includes SEND_REMINDER_AFTER_APPROVAL
```

## TC-06 Missing License Scan

Purpose: prove the counter copilot can find a counter-side prerequisite issue.

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "$baseUrl/api/ops-agents/counter-copilot" `
  -ContentType "application/json" `
  -Body '{"raId":"123456","location":"ORD","question":"Why is RA not ready?"}'
```

Expected:

```text
rootCause: Customer license scan is missing.
recommendedAction: Rescan the driver's license before continuing the deterministic signing flow.
```

## TC-07 Chicago Platform Incident

Purpose: prove the incident agent can classify a location-level outage.

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "$baseUrl/api/ops-agents/incident-management" `
  -ContentType "application/json" `
  -Body '{"raId":"489965957","location":"Chicago","question":"Chicago location is unable to retrieve agreements."}'
```

Expected:

```text
severity: High
rootCause: RMS endpoint unavailable.
evidence includes Affected locations: [ORD, MDW]
```

## TC-08 Health Monitoring Warning

Purpose: prove the monitoring agent detects TAS latency and queue backlog.

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "$baseUrl/api/ops-agents/health-monitoring" `
  -ContentType "application/json" `
  -Body '{"raId":"489965957","location":"Chicago","question":"Check health."}'
```

Expected:

```text
severity: Warning
rootCause: Operational latency and backlog indicate counter delays are likely.
```

## TC-09 End-to-End Timeline

Purpose: prove the transaction investigator builds a cross-system timeline.

```powershell
Invoke-RestMethod `
  -Method Post `
  -Uri "$baseUrl/api/ops-agents/transaction-investigation" `
  -ContentType "application/json" `
  -Body '{"raId":"489965957","location":"ORD","question":"Trace transaction."}'
```

Expected:

```text
rootCause: Transaction reached TAS but timed out during downstream processing.
timeline includes eRA, STL, RMS, TAS events
```

## TC-10 Request Validation

Purpose: prove requests without an RA ID are rejected.

```powershell
Invoke-WebRequest `
  -Method Post `
  -Uri "$baseUrl/api/ops-agents/ra-troubleshooting" `
  -ContentType "application/json" `
  -Body '{"raId":"","location":"ORD","question":"Missing RA"}'
```

Expected:

```text
HTTP 400 Bad Request
```
