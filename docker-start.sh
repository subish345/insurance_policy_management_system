#!/usr/bin/env bash
# ==============================================================================
# OmniShield Insurance Agency Management System - Container Launch Script
# Compatible with Linux and macOS using Docker or Podman
# ==============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "------------------------------------------------------------------"
echo " OmniShield Insurance Agency - Container Launcher"
echo "------------------------------------------------------------------"

if command -v docker &> /dev/null && docker compose version &> /dev/null; then
    echo "✓ Detected Docker with Docker Compose plugin."
    CMD="docker compose"
elif command -v docker-compose &> /dev/null; then
    echo "✓ Detected standalone docker-compose."
    CMD="docker-compose"
elif command -v podman &> /dev/null && podman compose version &> /dev/null; then
    echo "✓ Detected Podman with Compose support."
    CMD="podman compose"
elif command -v podman-compose &> /dev/null; then
    echo "✓ Detected podman-compose."
    CMD="podman-compose"
else
    echo "❌ Error: Neither Docker Compose nor Podman Compose was found."
    echo "Please install Docker Desktop or Podman Desktop."
    exit 1
fi

echo "Launching services using: $CMD up -d --build"
$CMD up -d --build

echo "------------------------------------------------------------------"
echo " Services are starting up!"
echo " Web Dashboard:  http://localhost:8080"
echo " REST API Base:  http://localhost:8080/api"
echo " Database Port:  localhost:3306 (user: root, pass: root)"
echo ""
echo " To view logs:   $CMD logs -f"
echo " To stop:        $CMD down"
echo "------------------------------------------------------------------"
