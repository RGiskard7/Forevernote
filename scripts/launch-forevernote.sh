#!/bin/bash
# Forevernote - Simplified launcher for macOS/Linux
# This script automatically detects Java and JavaFX and launches the application

# Colors for messages
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo ""
echo "========================================"
echo "  Forevernote - Launcher"
echo "========================================"
echo ""

# Get script directory and navigate to Forevernote directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
FORVERNOTE_DIR="$( cd "$SCRIPT_DIR/../Forevernote" && pwd )"
JAR="$FORVERNOTE_DIR/target/forevernote-1.0.0-uber.jar"

# Check if JAR exists
if [ ! -f "$JAR" ]; then
    echo -e "${RED}Error: JAR not found at $JAR${NC}"
    echo ""
    echo "Please build the project first:"
    echo "  ./scripts/build_all.sh"
    echo ""
    echo "Or manually:"
    echo "  cd Forevernote"
    echo "  mvn clean package -DskipTests"
    echo ""
    exit 1
fi

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo -e "${RED}Error: Java not found in PATH${NC}"
    echo ""
    echo "Please install Java 17 or higher from:"
    echo "  https://adoptium.net/"
    echo ""
    echo "And make sure it's added to your PATH."
    echo ""
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
echo -e "${GREEN}Java found: $JAVA_VERSION${NC}"
echo ""

# Find JavaFX in Maven repository
M2_REPO="$HOME/.m2/repository"
JAVAFX_BASE="$M2_REPO/org/openjfx"

if [ ! -d "$JAVAFX_BASE" ]; then
    echo -e "${YELLOW}Warning: JavaFX not found in Maven repository${NC}"
    echo ""
    echo "Attempting to launch without module-path (may fail)..."
    echo ""
    cd "$FORVERNOTE_DIR"
    java -jar "$JAR"
    exit $?
fi

# Find JavaFX version (21.x.x)
JAVAFX_VERSION=$(ls -1 "$JAVAFX_BASE/javafx-controls" 2>/dev/null | grep "^21" | sort -V | tail -1)

if [ -z "$JAVAFX_VERSION" ]; then
    echo -e "${YELLOW}Warning: JavaFX 21 not found in Maven repository${NC}"
    echo ""
    echo "Attempting to launch without module-path..."
    echo ""
    cd "$FORVERNOTE_DIR"
    java -jar "$JAR"
    exit $?
fi

# Build module-path using specific JAR files (not directories)
# This prevents Java from scanning directories and picking up -sources.jar files
MODULE_PATH=""
MODULES=""

# Include all required JavaFX modules (javafx.web requires javafx.media)
for module in base controls fxml graphics media web; do
    MODULE_DIR="$JAVAFX_BASE/javafx-$module/$JAVAFX_VERSION"
    if [ -d "$MODULE_DIR" ]; then
        # Find the actual JAR file (not -sources.jar or -javadoc.jar)
        # Use find instead of ls+grep for better compatibility (BSD/macOS and GNU/Linux)
        jar_file=$(find "$MODULE_DIR" -name "javafx-$module-*.jar" -not -name "*-sources.jar" -not -name "*-javadoc.jar" 2>/dev/null | head -n 1)
        if [ -n "$jar_file" ] && [ -f "$jar_file" ]; then
            # Use the JAR file path directly (Java module-path accepts individual JAR files)
            if [ -z "$MODULE_PATH" ]; then
                MODULE_PATH="$jar_file"
            else
                MODULE_PATH="$MODULE_PATH:$jar_file"
            fi
            if [ -z "$MODULES" ]; then
                MODULES="javafx.$module"
            else
                MODULES="$MODULES,javafx.$module"
            fi
        fi
    fi
done

if [ -z "$MODULE_PATH" ]; then
    echo -e "${YELLOW}Warning: Could not find JavaFX modules${NC}"
    echo ""
    echo "Attempting to launch without module-path..."
    echo ""
    cd "$FORVERNOTE_DIR"
    java -jar "$JAR"
    exit $?
fi

echo -e "${GREEN}JavaFX found (version $JAVAFX_VERSION)${NC}"
echo ""
echo "Launching Forevernote..."
echo ""

# Change to Forevernote directory so relative paths work
cd "$FORVERNOTE_DIR"

# Launch with module-path
java --module-path "$MODULE_PATH" --add-modules "$MODULES" -jar "$JAR"

EXIT_CODE=$?

if [ $EXIT_CODE -ne 0 ]; then
    echo ""
    echo -e "${RED}Error launching application (code: $EXIT_CODE)${NC}"
    echo ""
fi

exit $EXIT_CODE

