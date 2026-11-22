@echo off
set PORT=%1
if "%PORT%"=="" set PORT=COM4
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0bpc02_write_test.ps1" -Port %PORT%
pause
