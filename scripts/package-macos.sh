#!/bin/bash
# Package script for Forevernote - macOS (DMG installer)
# Usage: ./scripts/package-macos.sh
#
# Creates a native macOS DMG installer using jpackage.
# The resulting .app bundle includes a bundled JRE.

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ROOT_DIR="$( cd "$SCRIPT_DIR/.." && pwd )"
FORVERNOTE_DIR="$ROOT_DIR/Forevernote"

cd "$FORVERNOTE_DIR"

echo "========================================"
echo "  Forevernote - macOS Package Builder"
echo "========================================"
echo ""

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "Error: Java not found. Please install JDK 17 or higher."
    echo "Download from: https://adoptium.net/"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
echo "Java found: $JAVA_VERSION"
echo ""

# Check if jpackage is available
if ! command -v jpackage &> /dev/null; then
    echo "Error: jpackage not found. jpackage is included in JDK 17+."
    echo "Please install JDK (not JRE) from: https://adoptium.net/"
    exit 1
fi

echo "jpackage found"
echo ""

# Build the JAR first
echo "Building JAR..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "Error: Build failed"
    exit 1
fi

echo ""
echo "Creating macOS DMG installer..."
echo ""

OUTPUT_DIR="target/installers"
mkdir -p "$OUTPUT_DIR"

# Create a temporary input directory with only the JAR to avoid recursive copying
TEMP_INPUT_DIR=$(mktemp -d -t Forevernote-jpackage-input-XXXXXX)
JAR_PATH="target/forevernote-1.0.0-uber.jar"
cp "$JAR_PATH" "$TEMP_INPUT_DIR/"

# Cleanup function
cleanup() {
    rm -rf "$TEMP_INPUT_DIR"
}
trap cleanup EXIT

echo "Packaging application (this may take several minutes)..."
echo ""

# Use jpackage to create DMG installer
# Note: The uber-jar already includes JavaFX classes, so we don't need --module-path
jpackage \
    --input "$TEMP_INPUT_DIR" \
    --name Forevernote \
    --main-jar forevernote-1.0.0-uber.jar \
    --main-class com.example.forevernote.Launcher \
    --type dmg \
    --dest "$OUTPUT_DIR" \
    --app-version 1.0.0 \
    --vendor "Forevernote" \
    --description "A free and open-source note-taking application" \
    --copyright "Copyright 2025 Forevernote" \
    --mac-package-name "Forevernote" \
    --mac-app-category "public.app-category.productivity" \
    --java-options "-Dfile.encoding=UTF-8" \
    --java-options "-Dapple.awt.application.appearance=system"

if [ $? -eq 0 ]; then
    echo ""
    echo "========================================"
    echo "  Package created successfully!"
    echo "========================================"
    echo ""
    echo "Installer location: $OUTPUT_DIR/Forevernote-1.0.0.dmg"
    echo ""
    echo "Data will be stored in: ~/Library/Application Support/Forevernote/"
    echo ""
    echo "You can now distribute this DMG installer."
else
    echo ""
    echo "Error: Package creation failed"
    exit 1
fi
