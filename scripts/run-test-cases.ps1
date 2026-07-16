param(
    [string]$BaseUrl = "http://localhost:8081"
)

$ErrorActionPreference = "Stop"

function Invoke-AgentPost {
    param(
        [string]$Path,
        [hashtable]$Body
    )

    $json = $Body | ConvertTo-Json -Compress
    Invoke-RestMethod `
        -Method Post `
        -Uri "$BaseUrl$Path" `
        -ContentType "application/json" `
        -Body $json
}

$results = New-Object System.Collections.Generic.List[object]

function Add-Result {
    param(
        [string]$Case,
        [string]$Expected,
        [string]$Actual
    )

    $results.Add([pscustomobject]@{
        Case = $Case
        Result = if ($Actual -eq $Expected) { "PASS" } else { "FAIL" }
        Expected = $Expected
        Actual = $Actual
    })
}

try {
    $health = Invoke-RestMethod "$BaseUrl/actuator/health"
    Add-Result "Smoke" "UP" $health.status
} catch {
    Add-Result "Smoke" "UP" $_.Exception.Message
}

$cases = @(
    @{
        Name = "TC-01 TAS timeout"
        Path = "/api/ops-agents/ra-troubleshooting"
        Body = @{ raId = "489965957"; location = "ORD"; question = "Customer completed eRA but agreement is not visible in TAS." }
        Expected = "Customer signed successfully, PDF exists in S3, STL submit succeeded, and TAS API timeout occurred."
    },
    @{
        Name = "TC-02 PDF missing"
        Path = "/api/ops-agents/compliance-audit"
        Body = @{ raId = "200"; location = "DFW"; question = "Check audit state for missing signed PDF." }
        Expected = "Signature is present but archived PDF is missing."
    },
    @{
        Name = "TC-03 Metadata missing"
        Path = "/api/ops-agents/compliance-audit"
        Body = @{ raId = "300"; location = "DFW"; question = "Check audit metadata." }
        Expected = "Signed PDF exists but audit metadata is missing."
    },
    @{
        Name = "TC-04 Happy path"
        Path = "/api/ops-agents/compliance-audit"
        Body = @{ raId = "500"; location = "DFW"; question = "Check audit state." }
        Expected = "No compliance gap detected for this RA."
    },
    @{
        Name = "TC-05 Abandoned signing"
        Path = "/api/ops-agents/completion-recovery"
        Body = @{ raId = "777"; location = "MDW"; question = "Customer started eRA but never completed signing." }
        Expected = "Customer abandoned eRA at SIGNATURE step."
    },
    @{
        Name = "TC-06 Missing license"
        Path = "/api/ops-agents/counter-copilot"
        Body = @{ raId = "123456"; location = "ORD"; question = "Why is RA not ready?" }
        Expected = "Customer license scan is missing."
    },
    @{
        Name = "TC-07 Chicago incident"
        Path = "/api/ops-agents/incident-management"
        Body = @{ raId = "489965957"; location = "Chicago"; question = "Chicago location is unable to retrieve agreements." }
        Expected = "RMS endpoint unavailable."
    },
    @{
        Name = "TC-08 Health warning"
        Path = "/api/ops-agents/health-monitoring"
        Body = @{ raId = "489965957"; location = "Chicago"; question = "Check health." }
        Expected = "Operational latency and backlog indicate counter delays are likely."
    },
    @{
        Name = "TC-09 Timeline"
        Path = "/api/ops-agents/transaction-investigation"
        Body = @{ raId = "489965957"; location = "ORD"; question = "Trace transaction." }
        Expected = "Transaction reached TAS but timed out during downstream processing."
    }
)

foreach ($case in $cases) {
    try {
        $response = Invoke-AgentPost -Path $case.Path -Body $case.Body
        Add-Result $case.Name $case.Expected $response.rootCause
    } catch {
        Add-Result $case.Name $case.Expected $_.Exception.Message
    }
}

try {
    Invoke-WebRequest `
        -Method Post `
        -Uri "$BaseUrl/api/ops-agents/ra-troubleshooting" `
        -ContentType "application/json" `
        -Body '{"raId":"","location":"ORD","question":"Missing RA"}' `
        -ErrorAction Stop | Out-Null

    Add-Result "TC-10 Bad request" "HTTP 400" "Request succeeded"
} catch {
    $status = [int]$_.Exception.Response.StatusCode
    Add-Result "TC-10 Bad request" "HTTP 400" "HTTP $status"
}

try {
    $auditRun = Invoke-RestMethod `
        -Method Post `
        -Uri "$BaseUrl/api/agent/ra-completion" `
        -ContentType "application/json" `
        -Body '{"raId":"123","question":"Why cannot this customer finish eRA?"}'
    $audit = Invoke-RestMethod "$BaseUrl/api/agent/audit"
    $actual = if ($audit[0].runId -eq $auditRun.runId) { "Persisted" } else { "Not latest audit run" }
    Add-Result "TC-11 Audit API persistence" "Persisted" $actual
} catch {
    Add-Result "TC-11 Audit API persistence" "Persisted" $_.Exception.Message
}

$results | Format-Table -AutoSize

$failed = $results | Where-Object { $_.Result -ne "PASS" }
if ($failed.Count -gt 0) {
    exit 1
}
