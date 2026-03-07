@echo off
curl -s -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" -d "{\"email\":\"admin@flowguard.com\", \"password\":\"securepass\"}" > output.txt 2>&1
type output.txt
