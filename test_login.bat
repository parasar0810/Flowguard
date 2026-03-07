@echo off
echo Attempting to login with:
echo Email: admin7@flowguard.com
echo Password: securepass
echo.
curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d "{\"email\":\"admin7@flowguard.com\", \"password\":\"securepass\"}"
echo.
