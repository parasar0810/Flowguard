@echo off
set KEY=qZyXg3FlTwCP1hDPS2Xcpp_plfFEx3a9f7sTsjGZbcU
echo.
echo ====================================================
echo  FlowGuard Industry Upgrade — Endpoint Verification
echo ====================================================
echo.
echo [1] Analytics Summary...
curl -s -H "X-API-KEY: %KEY%" http://localhost:8080/api/analytics/summary
echo.
echo [2] Admin Stats...
curl -s http://localhost:8080/api/admin/stats
echo.
echo [3] Network Rules (empty = all IPs allowed)...
curl -s http://localhost:8080/api/admin/network-rules
echo.
echo [4] Analytics Methods...
curl -s -H "X-API-KEY: %KEY%" http://localhost:8080/api/analytics/methods
echo.
echo [5] Analytics Timeline (5 mins)...
curl -s -H "X-API-KEY: %KEY%" "http://localhost:8080/api/analytics/timeline?minutes=5"
echo.
echo [6] Block Reasons...
curl -s -H "X-API-KEY: %KEY%" http://localhost:8080/api/analytics/block-reasons
echo.
echo [7] Top Blocked IPs...
curl -s -H "X-API-KEY: %KEY%" http://localhost:8080/api/analytics/top-blocked-ips
echo.
echo [8] Recent Logs...
curl -s -H "X-API-KEY: %KEY%" http://localhost:8080/api/analytics/recent-logs
echo.
echo ====================================================
echo Done! Check X-Request-ID in response headers:
curl -v -o nul -s -H "X-API-KEY: %KEY%" http://localhost:8080/api/analytics/summary 2>&1 | findstr /i "X-Request"
echo ====================================================
