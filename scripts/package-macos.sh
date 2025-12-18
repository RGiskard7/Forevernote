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

# Function to read property from app.properties
read_property() {
    local key=$1
    local default=$2
    local props_file="$FORVERNOTE_DIR/src/main/resources/app.properties"
    if [ -f "$props_file" ]; then
        local value=$(grep "^[[:space:]]*${key}[[:space:]]*=" "$props_file" 2>/dev/null | cut -d'=' -f2- | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
        if [ -n "$value" ]; then
            echo "$value"
            return
        fi
    fi
    echo "$default"
}

# Read application metadata from app.properties
APP_NAME=$(read_property "app.name" "Forevernote")
APP_VERSION=$(read_property "app.version" "1.0.0")
APP_VENDOR=$(read_property "app.vendor" "Forevernote")
APP_DESCRIPTION=$(read_property "app.description" "A free and open-source note-taking application")
APP_COPYRIGHT=$(read_property "app.copyright" "Copyright 2025 Forevernote")
APP_ICON=$(read_property "app.icon.macos" "src/main/resources/icons/app-icon.icns")
APP_CATEGORY=$(read_property "app.package.category.macos" "public.app-category.productivity")

echo "========================================"
echo "  $APP_NAME - macOS Package Builder"
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

# Build jpackage command
JPACKAGE_CMD="jpackage \
    --input \"$TEMP_INPUT_DIR\" \
    --name \"$APP_NAME\" \
    --main-jar forevernote-1.0.0-uber.jar \
    --main-class com.example.forevernote.Launcher \
    --type dmg \
    --dest \"$OUTPUT_DIR\" \
    --app-version \"$APP_VERSION\" \
    --vendor \"$APP_VENDOR\" \
    --description \"$APP_DESCRIPTION\" \
    --copyright \"$APP_COPYRIGHT\" \
    --mac-package-name \"$APP_NAME\" \
    --mac-app-category \"$APP_CATEGORY\" \
    --java-options \"-Dfile.encoding=UTF-8\" \
    --java-options \"-Dapple.awt.application.appearance=system\""

# Add icon if it exists
ICON_PATH="$FORVERNOTE_DIR/$APP_ICON"
if [ -f "$ICON_PATH" ]; then
    JPACKAGE_CMD="$JPACKAGE_CMD --icon \"$ICON_PATH\""
    echo "Using icon: $ICON_PATH"
else
    echo "Icon not found at $ICON_PATH, skipping icon..."
fi

# Use jpackage to create DMG installer
# Note: The uber-jar already includes JavaFX classes, so we don't need --module-path
eval $JPACKAGE_CMD

if [ $? -eq 0 ]; then
    echo ""
    echo "========================================"
    echo "  Package created successfully!"
    echo "========================================"
    echo ""
    echo "Installer location: $OUTPUT_DIR/$APP_NAME-$APP_VERSION.dmg"
    echo ""
    echo "Data will be stored in: ~/Library/Application Support/$APP_NAME/"
    echo ""
    echo "You can now distribute this DMG installer."
else
    echo ""
    echo "Error: Package creation failed"
    exit 1
fi
