@echo off
set KEY=F5kHGg2umuZb_SLWPXYWI9namRbBfXeZ8SOF_RbiyEA
echo Seeding traffic for dashboard demo...
curl -s -H "X-API-KEY: %KEY%" http://localhost:8080/api/dashboard/stats > nul
curl -s -H "X-API-KEY: %KEY%" http://localhost:8080/api/dashboard/stats > nul
curl -s -H "X-API-KEY: %KEY%" http://localhost:8080/api/analytics/summary > nul
curl -s -H "X-API-KEY: %KEY%" http://localhost:8080/api/analytics/methods > nul
curl -s -H "X-API-KEY: %KEY%" http://localhost:8080/api/analytics/recent-logs > nul
curl -s -H "X-API-KEY: %KEY%" http://localhost:8080/api/dashboard/stats > nul
curl -s -H "X-API-KEY: %KEY%" http://localhost:8080/api/dashboard/stats > nul
curl -s -H "X-API-KEY: %KEY%" http://localhost:8080/api/dashboard/stats > nul
curl -s -H "X-API-KEY: %KEY%" http://localhost:8080/api/dashboard/recent-logs > nul
curl -s -H "X-API-KEY: %KEY%" http://localhost:8080/api/dashboard/stats > nul
echo Done - 10 requests seeded!
echo.
echo Final analytics summary:
curl -s http://localhost:8080/api/analytics/summary
echo.
echo Admin stats:
curl -s http://localhost:8080/api/admin/stats
