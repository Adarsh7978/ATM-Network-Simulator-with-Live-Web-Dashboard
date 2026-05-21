#!/bin/bash
# ATM Network Simulator - Build & Run Script

set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

SRC_DIR="src/main/java"
OUT_DIR="out"
MAIN_CLASS="com.atm.Main"

echo "╔══════════════════════════════════════════╗"
echo "║    ATM Network Simulator — Build Tool    ║"
echo "╚══════════════════════════════════════════╝"
echo ""

# Clean
rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR"
mkdir -p data

echo "[1/3] Compiling Java sources..."
find "$SRC_DIR" -name "*.java" | sort > /tmp/atm_sources.txt
javac -source 11 -target 11 -d "$OUT_DIR" @/tmp/atm_sources.txt
echo "      ✔  Compilation successful"

echo "[2/3] Packaging as JAR..."
jar --create --file atm-simulator.jar --main-class="$MAIN_CLASS" -C "$OUT_DIR" .
echo "      ✔  atm-simulator.jar created"

echo "[3/3] Starting server..."
echo ""
java -jar atm-simulator.jar
