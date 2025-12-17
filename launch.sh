#!/usr/bin/env bash
# Forevernote Launcher for macOS/Linux
# This script launches Forevernote with proper JavaFX module-path configuration

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR="$SCRIPT_DIR/Forevernote/target/forevernote-1.0.0-uber.jar"
M2_REPO="$HOME/.m2/repository"

# Check if JAR exists
if [ ! -f "$JAR" ]; then
    echo "Error: JAR not found at $JAR"
    echo "Please run: ./scripts/build_all.sh"
    exit 1
fi

# Build JavaFX module path from Maven repository
JAVAFX_MODULES=""
if [ -d "$M2_REPO/org/openjfx" ]; then
    # Include all required JavaFX modules
    for module in javafx-base javafx-controls javafx-fxml javafx-graphics javafx-web; do
        for module_dir in "$M2_REPO"/org/openjfx/$module/21*; do
            if [ -d "$module_dir" ]; then
                if [ -z "$JAVAFX_MODULES" ]; then
                    JAVAFX_MODULES="$module_dir"
                else
                    JAVAFX_MODULES="$JAVAFX_MODULES:$module_dir"
                fi
                break  # Only take the first matching version
            fi
        done
    done
fi

# Launch with module-path if JavaFX modules were found
if [ -n "$JAVAFX_MODULES" ]; then
    echo "Launching Forevernote with JavaFX module-path..."
    echo "Module path: $JAVAFX_MODULES"
    java --module-path "$JAVAFX_MODULES" --add-modules javafx.base,javafx.controls,javafx.fxml,javafx.graphics,javafx.web -jar "$JAR"
else
    echo "JavaFX modules not found in Maven repository. Attempting standard JAR launch..."
    java -jar "$JAR"
fi
