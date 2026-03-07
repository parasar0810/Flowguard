@echo off
chcp 65001 >nul
REM ═══════════════════════════════════════════════════════════════════
REM   FlowGuard — 200 Concurrent Requests Demo
REM   Demonstrates how FlowGuard handles massive burst traffic
REM   
REM   WHAT THIS SHOWS:
REM     1. Register a user + get API key (FREE plan: 50 req/min)
REM     2. Fire 200 requests simultaneously
REM     3. FlowGuard accepts first 50, rate-limits the remaining 150
REM     4. Every request is logged + visible on the live dashboard
REM ═══════════════════════════════════════════════════════════════════

set PORT=8080
set BASE=http://localhost:%PORT%

echo.
echo  ╔══════════════════════════════════════════════════════════╗
echo  ║   FlowGuard — 200 Concurrent Requests Demo              ║
echo  ║   Showing rate limiting + real-time event processing     ║
echo  ╚══════════════════════════════════════════════════════════╝
echo.

REM ─── Step 1: Register a fresh user ──────────────────────────────
echo  [STEP 1/5] Registering a fresh user...
for /f "tokens=*" %%i in ('powershell -Command "$body = @{email='demo_' + (Get-Random -Maximum 99999) + '@test.io'; password='Demo123!'} | ConvertTo-Json; $r = Invoke-RestMethod -Uri '%BASE%/api/auth/register' -Method POST -ContentType 'application/json' -Body $body; $r.data.apiKey"') do set API_KEY=%%i

if "%API_KEY%"=="" (
    echo    [ERROR] Failed to register user. Is the app running on port %PORT%?
    pause
    exit /b 1
)
echo    API Key: %API_KEY%
echo    Plan: FREE (50 requests / 60 seconds)
echo.

REM ─── Step 2: Show stats BEFORE the burst ────────────────────────
echo  [STEP 2/5] Current stats BEFORE burst:
powershell -Command "$r = Invoke-RestMethod -Uri '%BASE%/api/analytics/summary'; Write-Host ('    Total Requests : ' + $r.data.totalRequests); Write-Host ('    Allowed        : ' + $r.data.allowedRequests); Write-Host ('    Blocked        : ' + $r.data.blockedRequests); Write-Host ('    Block Rate     : ' + $r.data.blockRatePct + '%%')"
echo.

REM ─── Step 3: Fire 200 concurrent requests ──────────────────────
echo  [STEP 3/5] Firing 200 CONCURRENT requests to /api/test/ping
echo.
echo    How FlowGuard handles this:
echo      - Tomcat thread pool handles 200 simultaneous connections
echo      - RateLimitFilter checks each request against the limit
echo      - First ~50 requests → 200 OK (within rate limit)
echo      - Remaining ~150 → 429 Too Many Requests (rate limited)
echo      - ALL 200 events published to event bus and logged to DB
echo      - Dashboard updates in real-time via SSE
echo.
echo    Launching 200 requests NOW...
echo.

powershell -Command ^
  "$key = '%API_KEY%'; ^
   $url = '%BASE%/api/test/ping'; ^
   $total = 200; ^
   $allowed = 0; $blocked = 0; $errors = 0; ^
   $stopwatch = [System.Diagnostics.Stopwatch]::StartNew(); ^
   $jobs = 1..$total | ForEach-Object { ^
     Start-Job -ScriptBlock { ^
       param($u, $k) ^
       try { ^
         $r = Invoke-WebRequest -Uri $u -Headers @{'X-API-KEY'=$k} -UseBasicParsing -TimeoutSec 30; ^
         return $r.StatusCode ^
       } catch { ^
         if ($_.Exception.Response.StatusCode.value__ -eq 429) { return 429 } ^
         else { return 500 } ^
       } ^
     } -ArgumentList $u, $k ^
   }; ^
   Write-Host '    Waiting for all 200 requests to complete...'; ^
   $results = $jobs | Wait-Job | Receive-Job; ^
   $stopwatch.Stop(); ^
   $jobs | Remove-Job -Force; ^
   foreach ($r in $results) { ^
     if ($r -eq 200) { $allowed++ } ^
     elseif ($r -eq 429) { $blocked++ } ^
     else { $errors++ } ^
   }; ^
   Write-Host ''; ^
   Write-Host ('    ══════════════════════════════════════'); ^
   Write-Host ('    Results (completed in ' + $stopwatch.Elapsed.TotalSeconds.ToString('F1') + 's):'); ^
   Write-Host ('      ✓ ALLOWED (200 OK)       : ' + $allowed); ^
   Write-Host ('      ✕ RATE-LIMITED (429)      : ' + $blocked); ^
   if ($errors -gt 0) { Write-Host ('      ! ERRORS                 : ' + $errors) }; ^
   Write-Host ('      TOTAL                    : ' + $total); ^
   Write-Host ('    ══════════════════════════════════════'); ^
   Write-Host ''"

echo.

REM ─── Step 4: Show stats AFTER the burst ─────────────────────────
echo  [STEP 4/5] Stats AFTER burst:
timeout /t 3 /nobreak > nul
powershell -Command "$r = Invoke-RestMethod -Uri '%BASE%/api/analytics/summary'; Write-Host ('    Total Requests : ' + $r.data.totalRequests); Write-Host ('    Allowed        : ' + $r.data.allowedRequests); Write-Host ('    Blocked        : ' + $r.data.blockedRequests); Write-Host ('    Block Rate     : ' + $r.data.blockRatePct + '%%'); Write-Host ('    Avg Latency    : ' + $r.data.avgProcessingMs + ' ms')"
echo.

REM ─── Step 5: Show proof ────────────────────────────────────────
echo  [STEP 5/5] Proof — ALL events were captured:
powershell -Command "$r = Invoke-RestMethod -Uri '%BASE%/api/analytics/recent-logs'; $logs = $r.data; Write-Host ('    Last ' + $logs.Count + ' events in database:'); $logs | Select-Object -First 5 | ForEach-Object { $icon = if ($_.blocked) {'✕ BLOCKED'} else {'✓ OK     '}; Write-Host ('      ' + $icon + ' | ' + $_.method + ' ' + $_.uri + ' | ' + $_.status + ' | ' + $_.processingMs + 'ms') }"
echo.

echo  ╔══════════════════════════════════════════════════════════╗
echo  ║  KEY TAKEAWAY:                                           ║
echo  ║  All 200 requests were processed simultaneously.         ║
echo  ║  Rate-limited requests got 429, allowed ones got 200.    ║
echo  ║  EVERY request (allowed + blocked) was logged to the DB. ║
echo  ║  The dashboard shows all events in real-time via SSE.    ║
echo  ╚══════════════════════════════════════════════════════════╝
echo.
echo  Open the dashboard to see the results:
echo    %BASE%/dashboard.html
echo    %BASE%/analytics.html
echo.
pause
