@echo off
echo Registering fresh user for frontend demo...
curl -s -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" -d "{\"email\":\"demo@flowguard.com\", \"password\":\"demo1234\"}" > demo_key.json
type demo_key.json
echo.
echo Done! Check demo_key.json for your API key.
