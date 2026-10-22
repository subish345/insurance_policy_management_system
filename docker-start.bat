@echo off
REM ==============================================================================
REM OmniShield Insurance Agency Management System - Windows Docker Launch Script
REM ==============================================================================

echo ------------------------------------------------------------------
echo  OmniShield Insurance Agency - Docker Launcher (Windows)
echo ------------------------------------------------------------------

REM Check if Docker or Podman is installed
where docker >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo Detected Docker. Launching via Docker Compose...
    docker compose up -d --build
    goto SUCCESS
)

where podman >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo Detected Podman. Launching via Podman Compose...
    podman compose up -d --build
    goto SUCCESS
)

echo [ERROR] Neither Docker nor Podman was detected in your PATH.
echo Please install Docker Desktop (https://www.docker.com/) or Podman Desktop (https://podman-desktop.io/).
pause
exit /b 1

:SUCCESS
echo ------------------------------------------------------------------
echo  Containers started successfully!
echo  Access Web Dashboard at: http://localhost:8080
echo  To stop containers run:  docker compose down
echo ------------------------------------------------------------------
pause
