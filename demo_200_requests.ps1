# ============================================================
#  FlowGuard Demo - 200 Concurrent Requests
#  This script fires 200 parallel HTTP requests using the
#  DEMO plan API key to demonstrate rate limiting in action.
# ============================================================

$ErrorActionPreference = "Continue"

$API_KEY = (Get-Content "$PSScriptRoot\demo_api_key_final.txt" -Raw).Trim()
$BASE_URL = "http://localhost:8080"
$TOTAL = 200

Write-Host "`n=============================================" -ForegroundColor Cyan
Write-Host "  FlowGuard - 200 Concurrent Request Demo"    -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "  API Key   : $($API_KEY.Substring(0,12))..."  -ForegroundColor Gray
Write-Host "  Plan      : DEMO (300 req/min)"              -ForegroundColor Gray
Write-Host "  Requests  : $TOTAL"                          -ForegroundColor Gray
Write-Host "  Endpoint  : /api/test/ping"                  -ForegroundColor Gray
Write-Host "=============================================`n" -ForegroundColor Cyan

# ------ Fire all 200 requests in parallel via RunspacePool -------
$stopwatch = [System.Diagnostics.Stopwatch]::StartNew()

$runspacePool = [runspacefactory]::CreateRunspacePool(1, 50)
$runspacePool.Open()

$scriptBlock = {
    param($url, $key, $id)
    try {
        $sw = [System.Diagnostics.Stopwatch]::StartNew()
        $response = Invoke-WebRequest -Uri $url -Headers @{"X-API-KEY" = $key } -UseBasicParsing -TimeoutSec 30 -ErrorAction Stop
        $sw.Stop()
        return @{ Id = $id; Status = $response.StatusCode; Ms = $sw.ElapsedMilliseconds; Error = $null }
    }
    catch {
        $statusCode = 0
        if ($_.Exception.Response) {
            $statusCode = [int]$_.Exception.Response.StatusCode
        }
        return @{ Id = $id; Status = $statusCode; Ms = -1; Error = $_.Exception.Message }
    }
}

$jobs = @()
for ($i = 1; $i -le $TOTAL; $i++) {
    $ps = [PowerShell]::Create().AddScript($scriptBlock).AddArgument("$BASE_URL/api/test/ping").AddArgument($API_KEY).AddArgument($i)
    $ps.RunspacePool = $runspacePool
    $jobs += @{ Pipe = $ps; Handle = $ps.BeginInvoke() }
}

Write-Host "  [*] $TOTAL requests launched in parallel..." -ForegroundColor Yellow

# ------ Collect results ------
$results = @()
foreach ($job in $jobs) {
    $result = $job.Pipe.EndInvoke($job.Handle)
    $results += $result
    $job.Pipe.Dispose()
}

$stopwatch.Stop()
$runspacePool.Close()

# ------ Analyze results ------
$success = ($results | Where-Object { $_.Status -eq 200 }).Count
$rateLimited = ($results | Where-Object { $_.Status -eq 429 }).Count
$forbidden = ($results | Where-Object { $_.Status -eq 403 }).Count
$errors = ($results | Where-Object { $_.Status -ne 200 -and $_.Status -ne 429 -and $_.Status -ne 403 }).Count
$avgMs = ($results | Where-Object { $_.Ms -gt 0 } | Measure-Object -Property Ms -Average).Average

Write-Host "`n=============================================" -ForegroundColor Green
Write-Host "  RESULTS" -ForegroundColor Green
Write-Host "=============================================" -ForegroundColor Green
Write-Host "  Total Requests  : $TOTAL"                    -ForegroundColor White
Write-Host "  Allowed (200)   : $success"                  -ForegroundColor Green
Write-Host "  Rate Limited (429): $rateLimited"            -ForegroundColor Yellow
Write-Host "  Forbidden (403) : $forbidden"                -ForegroundColor Red
Write-Host "  Other Errors    : $errors"                   -ForegroundColor Red
Write-Host "  Total Time      : $($stopwatch.ElapsedMilliseconds) ms" -ForegroundColor Cyan
if ($avgMs) {
    Write-Host "  Avg Response    : $([math]::Round($avgMs,1)) ms" -ForegroundColor Cyan
}
Write-Host "=============================================`n" -ForegroundColor Green

# ------ Dashboard check ------
Write-Host "  [*] Checking dashboard stats..." -ForegroundColor Yellow
Start-Sleep -Seconds 2
try {
    $stats = Invoke-RestMethod -Uri "$BASE_URL/api/dashboard/stats"
    Write-Host "`n=============================================" -ForegroundColor Magenta
    Write-Host "  DASHBOARD STATS (After Demo)" -ForegroundColor Magenta
    Write-Host "=============================================" -ForegroundColor Magenta
    Write-Host "  Total Requests  : $($stats.data.totalRequests)"   -ForegroundColor White
    Write-Host "  Blocked         : $($stats.data.blockedRequests)" -ForegroundColor Yellow
    Write-Host "  Allowed         : $($stats.data.allowedRequests)" -ForegroundColor Green
    Write-Host "  Block Rate      : $($stats.data.blockRate)%"      -ForegroundColor Cyan
    Write-Host "  System Status   : $($stats.data.status)"         -ForegroundColor Green
    Write-Host "=============================================`n"    -ForegroundColor Magenta
}
catch {
    Write-Host "  [!] Could not fetch dashboard: $($_.Exception.Message)" -ForegroundColor Red
}

# ------ Analytics check ------
try {
    $analytics = Invoke-RestMethod -Uri "$BASE_URL/api/analytics/summary"
    Write-Host "=============================================" -ForegroundColor Blue
    Write-Host "  ANALYTICS SUMMARY" -ForegroundColor Blue
    Write-Host "=============================================" -ForegroundColor Blue
    Write-Host "  Total Processed : $($analytics.data.totalRequests)"   -ForegroundColor White
    Write-Host "  Allowed         : $($analytics.data.allowedRequests)" -ForegroundColor Green
    Write-Host "  Blocked         : $($analytics.data.blockedRequests)" -ForegroundColor Yellow
    Write-Host "  Block Rate      : $($analytics.data.blockRatePct)%"   -ForegroundColor Cyan
    Write-Host "  Avg Processing  : $($analytics.data.avgProcessingMs) ms" -ForegroundColor Cyan
    Write-Host "=============================================`n" -ForegroundColor Blue
}
catch {
    Write-Host "  [!] Could not fetch analytics: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "  Demo complete! The results above show FlowGuard" -ForegroundColor White
Write-Host "  handling 200 concurrent requests with rate limiting." -ForegroundColor White
Write-Host ""
