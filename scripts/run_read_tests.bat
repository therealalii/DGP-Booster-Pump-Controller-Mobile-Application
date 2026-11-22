@echo off
setlocal
set ARG1=%~1
set ARG2=%~2
if "%ARG1%"=="" set ARG1=COM4
if /I "%ARG1:~0,3%"=="COM" (
    powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0bpc02_full_read_test.ps1" -Port %ARG1%
) else (
    if "%ARG2%"=="" set ARG2=502
    powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0bpc02_full_read_test.ps1" -TcpHost %ARG1% -TcpPort %ARG2%
)
pause
