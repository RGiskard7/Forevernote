#!/usr/bin/env bash
set -euo pipefail

# Run script for Forevernote (Linux / macOS)
# Usage: ./scripts/run_all.sh
# 
# This script launches Forevernote with JavaFX module-path configuration if needed.

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR/Forevernote"

JAR="target/forevernote-1.0.0-uber.jar"
M2_REPO="$HOME/.m2/repository"

if [ -f "$JAR" ]; then
  echo "Launching Forevernote..."
  echo "JAR: $(pwd)/$JAR"
  
  # Build JavaFX module path from Maven repository
  JAVAFX_MODULES=""
  if [ -d "$M2_REPO/org/openjfx" ]; then
    for module_dir in "$M2_REPO"/org/openjfx/javafx-*/21*; do
      if [ -d "$module_dir" ]; then
        if [ -z "$JAVAFX_MODULES" ]; then
          JAVAFX_MODULES="$module_dir"
        else
          JAVAFX_MODULES="$JAVAFX_MODULES:$module_dir"
        fi
      fi
    done
  fi
  
  # Launch with module-path if JavaFX modules were found
  if [ -n "$JAVAFX_MODULES" ]; then
    echo "Using JavaFX module-path..."
    java --module-path "$JAVAFX_MODULES" --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.media -jar "$JAR"
  else
    echo "JavaFX modules not found in Maven repository. Attempting standard JAR launch..."
    java -jar "$JAR"
  fi
else
  echo "Packaged JAR not found, running via Maven..."
  mvn clean compile exec:java -Dexec.mainClass=com.example.forevernote.Main
fi
