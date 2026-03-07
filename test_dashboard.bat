@echo off
curl -s -H "X-API-KEY: f9OxswREjlP24Cbb_i8yM1J84WrANQaWklxNhksXcVA" http://localhost:8080/api/dashboard/stats > dashboard_stats.txt
type dashboard_stats.txt
