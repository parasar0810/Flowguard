@echo off
REM ═══════════════════════════════════════════════════════════════
REM   FlowGuard Kafka Load Test — 50 Concurrent Requests
REM   Shows how Kafka handles burst traffic without losing events
REM ═══════════════════════════════════════════════════════════════

echo [1/4] Registering a fresh user...
for /f "tokens=*" %%i in ('powershell -Command "$r = Invoke-WebRequest -Uri http://localhost:8081/api/auth/register -Method POST -ContentType 'application/json' -Body ('{\"email\":\"load_' + (Get-Random) + '@test.io\",\"password\":\"Demo123!\",\"fullName\":\"Load Tester\"}') -UseBasicParsing; ($r.Content | ConvertFrom-Json).data.apiKey"') do set API_KEY=%%i
echo    API Key: %API_KEY%

echo.
echo [2/4] Stats BEFORE burst:
powershell -Command "(Invoke-WebRequest -Uri http://localhost:8081/api/analytics/summary -UseBasicParsing).Content"

echo.
echo [3/4] Firing 50 CONCURRENT requests to /api/test/ping ...
echo      Each request goes: Filter -^> Kafka -^> Consumer -^> DB
echo.

for /L %%i in (1,1,50) do (
    start /B powershell -Command "try { $r = Invoke-WebRequest -Uri 'http://localhost:8081/api/test/ping' -Headers @{'X-API-KEY'='%API_KEY%'} -UseBasicParsing; Write-Host ('  Request %%i: ' + $r.StatusCode) } catch { Write-Host ('  Request %%i: RATE-LIMITED') }"
)

echo      Waiting for all 50 requests to complete...
timeout /t 10 /nobreak > nul

echo.
echo [4/4] Stats AFTER burst (Kafka consumed all events into DB):
powershell -Command "(Invoke-WebRequest -Uri http://localhost:8081/api/analytics/summary -UseBasicParsing).Content"

echo.
echo ═══════════════════════════════════════════════════════════════
echo   Open Kafka UI:   http://localhost:9090
echo   Open Dashboard:  http://localhost:8081/dashboard.html
echo   Open Analytics:  http://localhost:8081/analytics.html
echo ═══════════════════════════════════════════════════════════════
pause
