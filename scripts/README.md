# Build and Run Scripts

This directory contains cross-platform scripts for building and running the Forevernote application.

## Available Scripts

| Script | Platform | Purpose |
|--------|----------|---------|
| `build_all.ps1` | Windows (PowerShell) | Builds and packages the project |
| `build_all.sh` | macOS/Linux (Bash) | Builds and packages the project |
| `build-plugins.ps1` | Windows (PowerShell) | Compiles plugins to `Forevernote/plugins/` |
| `package-windows.ps1` | Windows (PowerShell) | Creates Windows executable (app-image or MSI) with plugins |
| `run_all.ps1` | Windows (PowerShell) | Runs the compiled application |
| `run_all.sh` | macOS/Linux (Bash) | Runs the compiled application |
| `schema.txt` | All | Example SQLite database schema |

## Quick Start

### Windows (PowerShell)

```powershell
# Make scripts executable (one-time)
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass

# Build
.\build_all.ps1

# Run
.\run_all.ps1
```

### macOS/Linux (Bash)

```bash
# Make scripts executable (one-time)
chmod +x scripts/*.sh

# Build
./scripts/build_all.sh

# Run
./scripts/run_all.sh
```

## Features

### Build Scripts

- **Maven Detection**: Automatically detects if Maven is installed
- **Auto-Installation**: Attempts to install Maven if not found:
  - Windows: Chocolatey, Scoop, or winget
  - macOS: Homebrew or SDKMAN
  - Linux: apt-get (Debian/Ubuntu) or dnf (Fedora/RHEL)
- **JAR Packaging**: Creates an executable uber-JAR at `Forevernote/target/forevernote-1.0.0-uber.jar`

### Run Scripts

- **JavaFX Module Detection**: Automatically finds JavaFX modules in `~/.m2/repository/org/openjfx/`
- **Module-Path Configuration**: Correctly configures the Java module-path for JavaFX
- **Fallback Support**: Falls back to Maven exec:java if direct JAR execution fails
- **Error Handling**: Provides clear error messages if issues occur

## How They Work

### Build Process

1. Verifies Maven is available
2. Runs `mvn clean package -DskipTests`
3. Creates uber-JAR with all dependencies included

### Run Process

1. Verifies JAR exists at `Forevernote/target/forevernote-1.0.0-uber.jar`
2. Searches for JavaFX modules in Maven repository
3. Constructs appropriate module-path for your system
4. Launches JAR with: `java --module-path <path> --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.media -jar <jar>`
5. If that fails, attempts: `mvn exec:java -Dexec.mainClass="com.example.forevernote.Main"`

### Package Script (Windows)

`package-windows.ps1` creates a distributable Windows executable:

1. Builds the main JAR with Maven
2. **Builds plugins** via `build-plugins.ps1` (compiles to `Forevernote/plugins/`)
3. Runs jpackage to create app-image (portable folder) or MSI installer
4. **Includes plugins** in the package:
   - JDK 18+: Uses `--app-content` (works for both app-image and MSI)
   - JDK 17: Copies `plugins/` into the app-image folder after jpackage

The packaged app looks for `plugins/` next to the executable. Ensure plugins are built before packaging, or run `.\scripts\build-plugins.ps1` first.

## Database Schema

The `schema.txt` file contains an example SQLite schema. The application automatically creates the required tables on first run.

## Troubleshooting

### Maven Installation Fails

If automatic Maven installation fails, install manually:

- **Windows**: Download from https://maven.apache.org
- **macOS**: `brew install maven`
- **Linux**: `sudo apt-get install maven`

### JavaFX Modules Not Found

Ensure you have run the build script first to download dependencies:

```bash
mvn -f Forevernote/pom.xml clean package -DskipTests
```

This will download JavaFX modules to `~/.m2/repository/org/openjfx/`.

### Permission Denied (Linux/macOS)

Make scripts executable:

```bash
chmod +x scripts/*.sh
```


