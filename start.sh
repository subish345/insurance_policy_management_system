#!/usr/bin/env bash
# ==============================================================================
# Insurance Agency Management System - Startup Script
# ==============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "------------------------------------------------------------------"
echo " Starting OmniShield Insurance Agency Management System..."
echo "------------------------------------------------------------------"

# 1. Check Java
if ! command -v java &> /dev/null || ! command -v javac &> /dev/null; then
    echo "❌ Error: JDK is required (javac and java). Please install JDK 17+."
    exit 1
fi
echo "✓ Java Runtime: $(java -version 2>&1 | head -n 1)"

# 2. Check Driver JAR
if [ ! -f "mysql-connector-j.jar" ]; then
    echo "❌ Error: mysql-connector-j.jar not found in $(pwd)."
    exit 1
fi
echo "✓ MySQL Connector JAR present."

# 3. Compile Java Source
echo "Compiling InsuranceBackend.java..."
javac -cp .:mysql-connector-j.jar InsuranceBackend.java
echo "✓ Compilation successful."

# 4. Launch Backend Server
PORT=${PORT:-8080}
DB_USER=${DB_USER:-root}
DB_PASS=${DB_PASS:-root}

echo "------------------------------------------------------------------"
echo " Launching Core Java HTTP Server on port $PORT..."
echo " Dashboard URL: http://localhost:$PORT"
echo " Press Ctrl+C to terminate the server."
echo "------------------------------------------------------------------"

exec java -cp .:mysql-connector-j.jar \
     -DDB_USER="$DB_USER" \
     -DDB_PASS="$DB_PASS" \
     InsuranceBackend
