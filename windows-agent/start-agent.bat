@echo off
cd /d "%~dp0"
if not exist config.json (
 echo Copy config.example.json to config.json and edit your settings.
 pause
 exit /b 1
)
if "%IPMONITOR_AGENT_TOKEN%"=="" (
 echo Set IPMONITOR_AGENT_TOKEN environment variable first.
 pause
 exit /b 1
)
py -3 agent.py
pause
