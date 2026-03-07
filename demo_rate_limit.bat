@echo off
set KEY=ofqCvsgxD6GlLk-uFzSAppNd0gVmtHsqE3DHTXnymYI
echo.
echo ==========================================
echo  FlowGuard Rate Limit Demo
echo  FREE Plan = 50 requests / 60 seconds
echo ==========================================
echo Firing 60 rapid requests... watch for 429!
echo.

for /L %%i in (1,1,60) do (
    for /F "tokens=*" %%r in ('curl -s -o nul -w "%%{http_code}" -H "X-API-KEY: %KEY%" http://localhost:8080/api/dashboard/stats') do (
        if "%%r"=="200" (
            echo Request %%i : [200 OK]      ALLOWED
        ) else if "%%r"=="429" (
            echo Request %%i : [429 TOO MANY] BLOCKED ^<--- RATE LIMITED!
        ) else (
            echo Request %%i : [%%r]
        )
    )
)

echo.
echo ==========================================
echo Final dashboard stats:
curl -s -H "X-API-KEY: %KEY%" http://localhost:8080/api/dashboard/stats
echo.
echo ==========================================
