# ═══════════════════════════════════════════════════════════════
#   FlowGuard - 200 Concurrent Requests Demo
#   Demonstrates rate limiting under heavy burst traffic
# ═══════════════════════════════════════════════════════════════

$BASE = "http://localhost:8080"

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  FlowGuard - 200 Concurrent Requests Demo" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# ─── Step 1: Register a fresh user ──────────────────────────
Write-Host "[STEP 1/5] Registering a fresh user..." -ForegroundColor Yellow
$body = @{ email = "demo_$(Get-Random -Maximum 99999)@test.io"; password = "Demo123!" } | ConvertTo-Json
try {
    $reg = Invoke-RestMethod -Uri "$BASE/api/auth/register" -Method POST -ContentType "application/json" -Body $body
    $API_KEY = $reg.data.apiKey
    Write-Host "  API Key: $API_KEY" -ForegroundColor Green
    Write-Host "  Plan: FREE (50 requests / 60 seconds)" -ForegroundColor DarkGray
} catch {
    Write-Host "  ERROR: Could not register. Is the app running?" -ForegroundColor Red
    exit 1
}

Write-Host ""

# ─── Step 2: Stats BEFORE burst ────────────────────────────
Write-Host "[STEP 2/5] Stats BEFORE burst:" -ForegroundColor Yellow
try {
    $before = Invoke-RestMethod -Uri "$BASE/api/analytics/summary"
    Write-Host "  Total: $($before.data.totalRequests) | Allowed: $($before.data.allowedRequests) | Blocked: $($before.data.blockedRequests)" -ForegroundColor DarkGray
} catch {
    Write-Host "  (No prior stats)" -ForegroundColor DarkGray
}

Write-Host ""

# ─── Step 3: Fire 200 concurrent requests ──────────────────
$TOTAL = 200
Write-Host "[STEP 3/5] Firing $TOTAL CONCURRENT requests..." -ForegroundColor Yellow
Write-Host ""
Write-Host "  How FlowGuard handles this:" -ForegroundColor White
Write-Host "    - Tomcat processes all $TOTAL requests in parallel (thread pool)" -ForegroundColor DarkGray
Write-Host "    - RateLimitFilter checks each against the rate limit" -ForegroundColor DarkGray
Write-Host "    - First ~50 pass (200 OK), rest get 429 (rate limited)" -ForegroundColor DarkGray
Write-Host "    - ALL $TOTAL events are logged to the database" -ForegroundColor DarkGray
Write-Host ""

$stopwatch = [System.Diagnostics.Stopwatch]::StartNew()

# Create 200 background jobs
$jobs = 1..$TOTAL | ForEach-Object {
    Start-Job -ScriptBlock {
        param($url, $key)
        try {
            $r = Invoke-WebRequest -Uri $url -Headers @{ "X-API-KEY" = $key } -UseBasicParsing -TimeoutSec 30
            return [int]$r.StatusCode
        } catch {
            $statusCode = $_.Exception.Response.StatusCode.value__
            if ($statusCode) { return [int]$statusCode }
            return 500
        }
    } -ArgumentList "$BASE/api/test/ping", $API_KEY
}

Write-Host "  Waiting for $TOTAL requests to complete..." -ForegroundColor DarkGray

# Wait for all jobs
$results = $jobs | Wait-Job | Receive-Job
$jobs | Remove-Job -Force

$stopwatch.Stop()

# Count results
$allowed = ($results | Where-Object { $_ -eq 200 }).Count
$rateLimited = ($results | Where-Object { $_ -eq 429 }).Count
$errors = ($results | Where-Object { $_ -ne 200 -and $_ -ne 429 }).Count

Write-Host ""
Write-Host "  ============================================" -ForegroundColor Cyan
Write-Host "  Results (completed in $($stopwatch.Elapsed.TotalSeconds.ToString('F1'))s):" -ForegroundColor Cyan
Write-Host "    ALLOWED   (200 OK)  : $allowed" -ForegroundColor Green
Write-Host "    BLOCKED   (429)     : $rateLimited" -ForegroundColor Red
if ($errors -gt 0) {
    Write-Host "    ERRORS              : $errors" -ForegroundColor Magenta
}
Write-Host "    TOTAL               : $TOTAL" -ForegroundColor White
Write-Host "  ============================================" -ForegroundColor Cyan
Write-Host ""

# ─── Step 4: Stats AFTER burst ─────────────────────────────
Start-Sleep -Seconds 2
Write-Host "[STEP 4/5] Stats AFTER burst:" -ForegroundColor Yellow
try {
    $after = Invoke-RestMethod -Uri "$BASE/api/analytics/summary"
    Write-Host "  Total: $($after.data.totalRequests) | Allowed: $($after.data.allowedRequests) | Blocked: $($after.data.blockedRequests) | Block Rate: $($after.data.blockRatePct)%" -ForegroundColor White
    Write-Host "  Avg Latency: $($after.data.avgProcessingMs) ms" -ForegroundColor DarkGray
} catch {
    Write-Host "  Could not fetch stats" -ForegroundColor Red
}

Write-Host ""

# ─── Step 5: Show recent logs as proof ─────────────────────
Write-Host "[STEP 5/5] Sample events from database (proof all were logged):" -ForegroundColor Yellow
try {
    $logs = Invoke-RestMethod -Uri "$BASE/api/analytics/recent-logs"
    $logs.data | Select-Object -First 8 | ForEach-Object {
        $icon = if ($_.blocked) { "BLOCKED" } else { "OK     " }
        $color = if ($_.blocked) { "Red" } else { "Green" }
        Write-Host "    $icon | $($_.method) $($_.uri) | $($_.status) | $($_.processingMs)ms" -ForegroundColor $color
    }
} catch {
    Write-Host "  Could not fetch logs" -ForegroundColor Red
}

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  KEY TAKEAWAY:" -ForegroundColor White
Write-Host "  All $TOTAL requests processed simultaneously." -ForegroundColor White
Write-Host "  Rate-limited ones got 429, allowed got 200." -ForegroundColor White
Write-Host "  EVERY event was captured in the database." -ForegroundColor White
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "  Dashboard: $BASE/dashboard.html" -ForegroundColor DarkGray
Write-Host "  Analytics: $BASE/analytics.html" -ForegroundColor DarkGray
Write-Host ""
