#!/bin/bash

# Script to clean Maven target directory on macOS
# This fixes permission issues that can occur when Maven tries to copy files
# 
# Usage: ./scripts/clean-target-macos.sh

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
ROOT_DIR="$( cd "$SCRIPT_DIR/.." && pwd )"
FORVERNOTE_DIR="$ROOT_DIR/Forevernote"

cd "$FORVERNOTE_DIR" || exit 1

echo "========================================"
echo "  Cleaning Maven Target Directory"
echo "========================================"
echo ""

if [ -d "target" ]; then
    echo "Removing target directory..."
    rm -rf "target"
    echo "✅ Target directory cleaned successfully."
    echo ""
    echo "You can now run: mvn clean compile"
else
    echo "ℹ️  Target directory does not exist, nothing to clean."
fi

echo ""
echo "Done."

