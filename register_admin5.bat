@echo off
curl -s -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" -d "{\"email\":\"admin5@flowguard.com\", \"password\":\"securepass\"}" > admin5_key.json
type admin5_key.json
