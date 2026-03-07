@echo off
curl -s -X POST http://localhost:8080/api/auth/register -H "Content-Type: application/json" -d "{\"email\":\"admin4@flowguard.com\", \"password\":\"securepass\"}" > admin4_key.json
type admin4_key.json
