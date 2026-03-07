@echo off
curl -s -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" -d "{\"email\":\"admin6@flowguard.com\", \"password\":\"securepass\"}" > admin6_key.json
type admin6_key.json
