@echo off
curl -s -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" -d "{\"email\":\"admin7@flowguard.com\", \"password\":\"securepass\"}" > admin7_key.json
type admin7_key.json
