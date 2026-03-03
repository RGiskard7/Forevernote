# Forevernote

<div align="center">
  <a href="README.es.md">Español</a> |
  <strong>English</strong>
</div>

<div align="center">
  <img src="resources/images/banner.png" alt="Forevernote Banner" style="width: 100%; max-width: 100%;">
</div>

<div align="center">

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Version](https://img.shields.io/badge/version-1.0.0-success.svg)](changelog.md)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-blue.svg)](https://openjfx.io/)
[![SQLite](https://img.shields.io/badge/SQLite-3-lightgrey.svg)](https://www.sqlite.org/)
[![Maven](https://img.shields.io/badge/Maven-3.9+-red.svg)](https://maven.apache.org/)
[![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20macOS%20%7C%20Linux-lightgrey.svg)]()

</div>

<div align="center">
  <strong>Local-first desktop note-taking app with Markdown preview, plugins, themes, and dual storage backends.</strong>
</div>

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Screenshots](#screenshots)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Scripts and Commands (All OS)](#scripts-and-commands-all-os)
- [Project Structure](#project-structure)
- [Configuration](#configuration)
- [Documentation](#documentation)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License](#license)

## Overview

Forevernote is a Java 17 + JavaFX 21 desktop application inspired by Obsidian-like workflows:

- Fast note writing/editing with Markdown preview
- Folder hierarchy + tags + favorites + recent + trash
- Command palette and quick switcher
- External plugins (`plugins/`) and external themes (`themes/`)
- Storage mode: SQLite or FileSystem vault

## Features

### Core

- Create, edit, save, delete, and restore notes
- Hierarchical folders and subfolders
- Tags with assignment/removal workflows
- Favorites and recent notes
- Trash with restore for notes and nested folders
- Global search and sorting

### Editor & Preview

- Markdown rendering with GFM tables, autolinks, strikethrough
- Live preview and split mode
- Syntax highlighting for fenced code blocks (highlight.js)

### UI/UX

- Light, dark, system themes + external themes
- Retro phosphor sample external theme
- Configurable sidebar/editor button presentation (text/icons/auto)
- List and grid note views
- Compact and responsive layout behavior

### Extensibility

- External plugin loading from JAR files in `plugins/`
- Plugin manager UI
- Plugin lifecycle support (load/enable/disable/shutdown)
- Theme catalog with external theme discovery and safe fallback

## Screenshots

### Main Interface

![Main Interface](resources/images/interfaz-3.png)

### Dark Theme

![Dark Theme](resources/images/interfaz-1.png)

### Light Theme

![Light Theme](resources/images/interfaz-2.png)

### Editor & Preview

![Editor Features](resources/images/interfaz-4.png)

## Technology Stack

- Java 17
- JavaFX 21
- Maven 3.9+
- SQLite JDBC
- CommonMark
- Ikonli (Feather icons)
- JUnit 5 + H2 (tests)

## Prerequisites

1. Java JDK 17
2. Maven 3.9+

Check installation:

```bash
java -version
mvn -version
```

## Quick Start

### 1) Clone

```bash
git clone https://github.com/RGiskard7/Forevernote.git
cd Forevernote
```

### 2) Build

```bash
./scripts/build_all.sh
```

```powershell
.\scripts\build_all.ps1
```

### 3) Run

```bash
./scripts/launch-forevernote.sh
```

```powershell
.\scripts\launch-forevernote.bat
# or
.\scripts\launch-forevernote.ps1
```

## Scripts and Commands (All OS)

All commands assume repository root:

`/Users/edu/visual-studio-code-workspace/Forevernote`

### Build / Run Matrix

| Purpose | Linux/macOS | Windows PowerShell | Windows CMD |
|---|---|---|---|
| Build app | `./scripts/build_all.sh` | `.\scripts\build_all.ps1` | N/A |
| Run app (dev runner) | `./scripts/run_all.sh` | `.\scripts\run_all.ps1` | N/A |
| Run app (launcher, recommended) | `./scripts/launch-forevernote.sh` | `.\scripts\launch-forevernote.ps1` | `.\scripts\launch-forevernote.bat` |

### Tests and Quality Gates

```bash
mvn -f Forevernote/pom.xml test
mvn -f Forevernote/pom.xml clean test
```

```bash
./scripts/smoke-phase-gate.sh
./scripts/hardening-storage-matrix.sh
```

```powershell
.\scripts\smoke-phase-gate.ps1
.\scripts\hardening-storage-matrix.ps1
```

### Plugins (external JARs)

```bash
./scripts/build-plugins.sh
./scripts/build-plugins.sh --clean
```

```powershell
.\scripts\build-plugins.ps1
.\scripts\build-plugins.ps1 -Clean
```

### Themes (external)

```bash
./scripts/build-themes.sh
./scripts/build-themes.sh --clean
./scripts/build-themes.sh --appdata
```

```powershell
.\scripts\build-themes.ps1
.\scripts\build-themes.ps1 -Clean
.\scripts\build-themes.ps1 -AppData
```

### Packaging

```bash
mvn -f Forevernote/pom.xml clean package -DskipTests
./scripts/package-linux.sh
./scripts/package-macos.sh
```

```powershell
.\scripts\package-windows.ps1
```

Packaging scripts now prepare both external plugins and external themes automatically before calling `jpackage`:
- `package-macos.sh` -> runs `build-plugins.sh` + `build-themes.sh`
- `package-linux.sh` -> runs `build-plugins.sh` + `build-themes.sh`
- `package-windows.ps1` -> runs `build-plugins.ps1` + `build-themes.ps1`

### Maven Development Run

```bash
mvn -f Forevernote/pom.xml clean compile exec:java -Dexec.mainClass="com.example.forevernote.Launcher"
```

## Project Structure

```text
Forevernote/
├── Forevernote/
│   ├── pom.xml
│   ├── src/main/java/com/example/forevernote/
│   │   ├── config/
│   │   ├── data/
│   │   ├── event/
│   │   ├── exceptions/
│   │   ├── plugin/
│   │   ├── service/
│   │   ├── sync/
│   │   ├── ui/
│   │   └── util/
│   ├── src/main/resources/com/example/forevernote/
│   │   ├── i18n/
│   │   ├── plugin/
│   │   ├── ui/css/
│   │   ├── ui/preview/
│   │   └── ui/view/
│   ├── src/test/
│   └── themes/                     # runtime-installed external themes
├── plugins/                        # external plugin jars
├── plugins-source/                 # sample plugin source workspace
├── themes/                         # source external themes
├── scripts/
├── doc/
├── AGENTS.md
├── changelog.md
├── README.md
└── README.es.md
```

## Configuration

### Storage

- SQLite database (default) or FileSystem vault mode
- Runtime folders are created automatically as needed:
  - `Forevernote/data/`
  - `Forevernote/logs/`

### Themes

External theme format:

```text
themes/<theme-id>/theme.properties
themes/<theme-id>/theme.css
```

### Plugins

- Place plugin JAR files in `plugins/`
- Open plugin manager from Tools menu for enable/disable operations

### Do I Need to Move Plugins/Themes Manually?

Short answer: usually **no**.

- Development run (`run_all.*` / `launch-forevernote.*`):
  - `build-plugins.*` places JARs into `Forevernote/plugins/`
  - `build-themes.*` places themes into `Forevernote/themes/`
  - App resolves both locations directly, no manual move required.

- Packaged installers (`package-*`):
  - Scripts already build plugins/themes automatically.
  - If your JDK supports `jpackage --app-content`, plugins/themes are bundled.
  - If not (common with JDK 17), app still works but users should place files in AppData:
    - Windows: `%APPDATA%\Forevernote\plugins` and `%APPDATA%\Forevernote\themes`
    - macOS: `~/Library/Application Support/Forevernote/plugins` and `~/Library/Application Support/Forevernote/themes`
    - Linux: `~/.config/Forevernote/plugins` and `~/.config/Forevernote/themes`

## Documentation

- [doc/BUILD.md](doc/BUILD.md)
- [doc/ARCHITECTURE.md](doc/ARCHITECTURE.md)
- [doc/PLUGINS.md](doc/PLUGINS.md)
- [doc/LAUNCH_APP.md](doc/LAUNCH_APP.md)
- [doc/PACKAGING.md](doc/PACKAGING.md)
- [doc/EVENT_BUS_CONTRACT.md](doc/EVENT_BUS_CONTRACT.md)
- [doc/DEFINITION_OF_DONE.md](doc/DEFINITION_OF_DONE.md)
- [doc/PROJECT_STATUS.md](doc/PROJECT_STATUS.md)
- [doc/PROJECT_ANALYSIS.md](doc/PROJECT_ANALYSIS.md)
- [AGENTS.md](AGENTS.md)

## Troubleshooting

### JavaFX Runtime Errors

If you get JavaFX runtime/module-path issues, use launcher scripts (`launch-forevernote.*`) instead of running JAR directly.

### Maven/Java Missing

Ensure both are available in `PATH`:

```bash
java -version
mvn -version
```

### JavaFX Parent-POM Warnings

Warnings such as `Failed to build parent project for org.openjfx:javafx-*` are known and non-blocking.

## Contributing

- Keep changes focused and incremental.
- Run tests before opening PR.
- Preserve SQLite/FileSystem and plugin compatibility.
- Update documentation when behavior changes.

## License

MIT. See [LICENSE](LICENSE).
