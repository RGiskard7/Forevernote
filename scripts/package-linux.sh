#!/bin/bash
# Package script for Forevernote - Linux (DEB/RPM installer)
# Usage: ./scripts/package-linux.sh

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ROOT_DIR="$( cd "$SCRIPT_DIR/.." && pwd )"
FORVERNOTE_DIR="$ROOT_DIR/Forevernote"

cd "$FORVERNOTE_DIR"

echo "========================================"
echo "  Forevernote - Linux Package Builder"
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

# Detect Linux distribution
if [ -f /etc/debian_version ]; then
    PACKAGE_TYPE="deb"
    echo "Detected Debian/Ubuntu - will create DEB package"
elif [ -f /etc/redhat-release ] || [ -f /etc/fedora-release ]; then
    PACKAGE_TYPE="rpm"
    echo "Detected RedHat/Fedora - will create RPM package"
else
    # Default to DEB, but try both
    PACKAGE_TYPE="deb"
    echo "Unknown distribution - will create DEB package (you can modify to create RPM)"
fi

echo ""

# Build the JAR first
echo "Building JAR..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "Error: Build failed"
    exit 1
fi

echo ""
echo "Creating Linux $PACKAGE_TYPE installer..."
echo ""
echo "Running jpackage (this may take several minutes)..."
echo ""
echo "Packaging application (downloading Java runtime if needed)..."
echo ""

OUTPUT_DIR="target/installers"
mkdir -p "$OUTPUT_DIR"

# Use jpackage to create installer
jpackage \
    --input target \
    --name Forevernote \
    --main-jar forevernote-1.0.0-uber.jar \
    --main-class com.example.forevernote.Main \
    --type "$PACKAGE_TYPE" \
    --dest "$OUTPUT_DIR" \
    --app-version 1.0.0 \
    --vendor "Forevernote" \
    --description "A free and open-source note-taking application" \
    --copyright "Copyright 2025 Forevernote" \
    --linux-package-name "forevernote" \
    --linux-app-category "Office" \
    --linux-shortcut

if [ $? -eq 0 ]; then
    echo ""
    echo "========================================"
    echo "  Package created successfully!"
    echo "========================================"
    echo ""
    
    if [ "$PACKAGE_TYPE" = "deb" ]; then
        echo "Installer location: $OUTPUT_DIR/forevernote_1.0.0-1_amd64.deb"
        echo ""
        echo "Install with: sudo dpkg -i $OUTPUT_DIR/forevernote_1.0.0-1_amd64.deb"
    else
        echo "Installer location: $OUTPUT_DIR/forevernote-1.0.0-1.x86_64.rpm"
        echo ""
        echo "Install with: sudo rpm -i $OUTPUT_DIR/forevernote-1.0.0-1.x86_64.rpm"
    fi
    
    echo ""
    echo "You can now distribute this installer."
    echo "Users can install it like any other Linux package."
else
    echo ""
    echo "Error: Package creation failed"
    exit 1
fi

