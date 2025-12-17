# Forevernote

A lightweight desktop application for managing notes with hierarchical organization through folders and tags. Built with Java and JavaFX, featuring SQLite for persistent storage, Markdown support with live preview, and a modern, intuitive user interface.

## Features

- **Note Management**: Create, edit, and delete notes with titles and content
- **Folder Organization**: Organize notes hierarchically using notebooks/folders with visible "All Notes" root
- **Tags**: Categorize and search notes using tags with full tag management interface
- **Markdown Support**: Write notes in Markdown with live preview and emoji support
- **Rich Text Formatting**: Bold, italic, underline, links, images, todo lists, and numbered lists
- **Search**: Global search across all notes (titles and content)
- **Auto-refresh**: Notes list automatically updates on save/delete operations
- **Zoom Controls**: Adjust text size (50%-300%)
- **Theme Support**: Light, dark, and system theme options (placeholder)
- **Keyboard Shortcuts**: Comprehensive keyboard shortcuts for all operations
- **To-Do Support**: Mark notes as to-do items with completion tracking
- **Logging**: Comprehensive application logging for debugging and monitoring

## Technology Stack

- **Java 17**: Core programming language
- **JavaFX 21**: Desktop user interface framework
- **SQLite**: Lightweight relational database
- **Maven 3.9+**: Build automation and dependency management
- **JUnit 5**: Unit testing framework

## Prerequisites

- **JDK 17** or later
- **Apache Maven 3.6+** (or use the scripts for automatic setup)

### Verify Installation

```bash
java -version
mvn -version
```

## Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/RGiskard7/Forevernote.git
cd Forevernote
```

### 2. Build the Project

**Windows (PowerShell):**
```powershell
.\scripts\build_all.ps1
```

**macOS/Linux (Bash):**
```bash
./scripts/build_all.sh
```

This creates an executable JAR at `Forevernote/target/forevernote-1.0.0-uber.jar`.

### 3. Run the Application

**Windows (PowerShell):**
```powershell
.\scripts\run_all.ps1
```

Or directly:
```powershell
.\launch.bat
```

**macOS/Linux (Bash):**
```bash
./scripts/run_all.sh
```

Or directly:
```bash
./launch.sh
```

## Alternative: Build and Run Manually

### Build

```bash
mvn -f Forevernote/pom.xml clean package -DskipTests
```

### Run JAR

```bash
java -jar Forevernote/target/forevernote-1.0.0-uber.jar
```

### Run from Source (Development)

```bash
mvn -f Forevernote/pom.xml exec:java -Dexec.mainClass="com.example.forevernote.Main"
```

## Project Structure

```
Forevernote/
├── Forevernote/                          # Main project module
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/forevernote/
│   │   │   │   ├── Main.java            # Application entry point
│   │   │   │   ├── config/              # Configuration classes
│   │   │   │   ├── data/                # Data access layer
│   │   │   │   │   ├── SQLiteDB.java    # Database connection management
│   │   │   │   │   ├── dao/             # Data Access Objects
│   │   │   │   │   └── models/          # Data models (Note, Folder, Tag)
│   │   │   │   ├── exceptions/          # Custom exceptions
│   │   │   │   └── ui/                  # User interface
│   │   │   │       ├── controller/      # JavaFX controllers
│   │   │   │       ├── view/            # FXML layouts
│   │   │   │       └── css/             # Stylesheets
│   │   │   └── resources/               # Configuration and assets
│   │   └── test/                        # Unit tests
│   ├── pom.xml                          # Maven configuration
│   ├── target/                          # Build output directory
│   ├── data/                            # Runtime data directory (created on first run)
│   └── logs/                            # Runtime logs directory (created on first run)
│
├── scripts/
│   ├── build_all.ps1                    # Windows build script
│   ├── build_all.sh                     # Unix build script
│   ├── run_all.ps1                      # Windows run script
│   ├── run_all.sh                       # Unix run script
│   ├── schema.txt                       # SQLite schema example
│   └── README.md                        # Scripts documentation
│
├── .gitignore                           # Git ignore rules
├── .vscode/
│   └── tasks.json                       # VS Code build tasks
├── README.md                            # This file
├── SETUP.md                             # Quick setup guide
├── BUILD.md                             # Build documentation
└── LICENSE                              # MIT License
```

## Configuration

### Database

The application automatically creates a SQLite database at `Forevernote/data/database.db` on first run (when executed from the `Forevernote/` directory). The database includes tables for notes, folders, tags, and their relationships.

**Note**: The `data/` and `logs/` directories are created automatically when the application runs, not during compilation. The build scripts do not create these directories.

### Logging

Logging configuration is defined in `src/main/resources/logging.properties`. Logs are written to the `Forevernote/logs/` directory by default (created automatically on first run).

### JavaFX Module-Path

The build and run scripts automatically detect JavaFX modules from your Maven repository (`~/.m2/repository/org/openjfx/`). If running the JAR directly fails, use the provided scripts which handle module-path configuration automatically.

## VS Code Integration

The project includes `.vscode/tasks.json` with predefined build and run tasks:

- **Build Forevernote** (Ctrl+Shift+B): Compiles and packages the project
- **Run Forevernote (Script)**: Executes via PowerShell/Bash script
- **Run Forevernote (Direct)**: Executes via launch.bat/launch.sh

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.
