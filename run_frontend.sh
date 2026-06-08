#!/bin/bash
# run_frontend.sh — Run from project root: bash run_frontend.sh
set -e

echo "=== TrueTone Frontend Launcher ==="

FRONTEND_DIR="frontend"
SRC_DIR="$FRONTEND_DIR/src"
JSON_JAR="$FRONTEND_DIR/json.jar"

# Download JSON jar if missing
if [ ! -f "$JSON_JAR" ]; then
    echo "Downloading org.json library…"
    curl -L -o "$JSON_JAR" \
        "https://search.maven.org/remotecontent?filepath=org/json/json/20240303/json-20240303.jar"
    echo "✅ json.jar downloaded"
fi

# Compile
echo "Compiling Java source…"
cd "$SRC_DIR"
javac -cp .:../json.jar TrueToneApp.java
echo "✅ Compilation successful"

# Run
echo "Launching TrueTone GUI…"
java -cp .:../json.jar TrueToneApp
