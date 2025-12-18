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

- **Java 17**: Core programming language (required)
- **JavaFX 21**: Desktop user interface framework
- **SQLite**: Lightweight relational database
- **Maven 3.9+**: Build automation and dependency management
- **JUnit 5**: Unit testing framework

## Prerequisites

### Required Software

1. **Java JDK 17** (required)
   - Download from: https://adoptium.net/ or https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html
   - **Important**: You need JDK (Java Development Kit), not just JRE (Java Runtime Environment)
   - Verify installation:
     ```bash
     java -version
     # Should show: openjdk version "17" or java version "17"
     ```

2. **Apache Maven 3.6+** (required for building)
   - Download from: https://maven.apache.org/download.cgi
   - Or use package managers:
     - **Windows**: `choco install maven` or `winget install Apache.Maven`
     - **macOS**: `brew install maven`
     - **Linux (Ubuntu/Debian)**: `sudo apt-get install maven`
   - Verify installation:
     ```bash
     mvn -version
     # Should show: Apache Maven 3.x.x
     ```

### Optional: VS Code Extensions

For development in VS Code, install:
- **Extension Pack for Java** (includes Java Language Support, Debugger, Test Runner, etc.)
- **Maven for Java**

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

**Note**: During compilation, you may see warnings like "Failed to build parent project for org.openjfx:javafx-*". These are **normal and harmless** - they occur because Maven tries to build the JavaFX parent project, which is not necessary. The build will still succeed.

### 3. Run the Application

**Windows (PowerShell):**
```powershell
.\scripts\run_all.ps1
```

**macOS/Linux (Bash):**
```bash
./scripts/run_all.sh
```

The scripts automatically:
- Detect JavaFX modules in your Maven repository
- Configure the Java module-path correctly
- Launch the application

## Building and Running with VS Code

### Prerequisites

1. **Install Java 17 JDK** (not just JRE)
   - Ensure `java -version` shows version 17
   - VS Code will detect it automatically

2. **Install VS Code Extensions**:
   - Open VS Code
   - Press `Ctrl+Shift+X` (or `Cmd+Shift+X` on macOS)
   - Search and install: **Extension Pack for Java**
   - This includes all necessary Java extensions

3. **Configure Java Runtime** (if needed):
   - Press `Ctrl+Shift+P` (or `Cmd+Shift+P` on macOS)
   - Type: `Java: Configure Java Runtime`
   - Select Java 17 as the default runtime
   - If Java 17 doesn't appear, add it manually pointing to your JDK 17 installation

### Building in VS Code

**Method 1: Using Tasks (Recommended)**
1. Press `Ctrl+Shift+B` (or `Cmd+Shift+B` on macOS)
2. Select **"maven-compile"** to compile
3. Or select **"maven-package"** to build the JAR

**Method 2: Using Terminal**
1. Open integrated terminal: `Ctrl+`` (backtick)
2. Run:
   ```bash
   cd Forevernote
   mvn clean package -DskipTests
   ```

### Running in VS Code

**Method 1: Using Debug/Run (Recommended)**
1. Press `F5` or go to **Run and Debug** (Ctrl+Shift+D)
2. Select **"Launch Forevernote (Maven JavaFX)"** from the dropdown
3. Click the green play button or press `F5`
4. The application will launch with JavaFX properly configured

**Method 2: Using Tasks**
1. Press `Ctrl+Shift+P` (or `Cmd+Shift+P` on macOS)
2. Type: `Tasks: Run Task`
3. Select **"maven-exec-java"** to run via Maven

**Method 3: Using Terminal**
1. Open integrated terminal: `Ctrl+`` (backtick)
2. Run:
   ```bash
   cd Forevernote
   mvn javafx:run
   ```

### Troubleshooting VS Code

**Problem**: VS Code shows errors for JavaFX imports
- **Solution**: 
  1. Press `Ctrl+Shift+P` → `Java: Clean Java Language Server Workspace`
  2. Press `Ctrl+Shift+P` → `Java: Reload Projects`
  3. Wait 1-2 minutes for Maven to sync dependencies

**Problem**: "JavaFX runtime components are missing" when running
- **Solution**: Use the "Launch Forevernote (Maven JavaFX)" configuration, which uses Maven's JavaFX plugin

**Problem**: VS Code uses Java 21 instead of Java 17
- **Solution**: 
  1. Press `Ctrl+Shift+P` → `Java: Configure Java Runtime`
  2. Set Java 17 as default
  3. Update `.vscode/settings.json` if needed (see configuration section)

## Alternative: Build and Run Manually

### Build

```bash
cd Forevernote
mvn clean package -DskipTests
```

### Run JAR (with scripts - recommended)

The scripts handle JavaFX module-path automatically:

**Windows:**
```powershell
.\scripts\run_all.ps1
```

**macOS/Linux:**
```bash
./scripts/run_all.sh
```

### Run JAR (directly - requires manual module-path)

If you want to run the JAR directly, you need to specify JavaFX modules:

```bash
java --module-path "C:\Users\<you>\.m2\repository\org\openjfx\javafx-base\21\javafx-base-21.jar;C:\Users\<you>\.m2\repository\org\openjfx\javafx-controls\21\javafx-controls-21.jar;..." --add-modules javafx.base,javafx.controls,javafx.fxml,javafx.graphics,javafx.media,javafx.web -jar Forevernote/target/forevernote-1.0.0-uber.jar
```

**Note**: This is complex and error-prone. Use the scripts instead.

### Run from Source (Development)

```bash
cd Forevernote
mvn exec:java -Dexec.mainClass="com.example.forevernote.Main"
```

Or using JavaFX Maven plugin:
```bash
mvn javafx:run
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
│   │   │   │   ├── ui/                  # User interface
│   │   │   │   │   ├── controller/      # JavaFX controllers
│   │   │   │   │   ├── view/            # FXML layouts
│   │   │   │   │   └── css/             # Stylesheets
│   │   │   │   └── util/                # Utility classes
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
├── .vscode/
│   ├── settings.json                    # VS Code Java configuration
│   ├── tasks.json                       # VS Code build tasks
│   ├── launch.json                      # VS Code debug/run configurations
│   └── extensions.json                  # Recommended VS Code extensions
│
├── .gitignore                           # Git ignore rules
├── README.md                            # This file
├── SETUP.md                             # Quick setup guide
├── BUILD.md                              # Build documentation
├── AGENTS.md                            # Agent-oriented development guide
└── LICENSE                              # MIT License
```

## Configuration

### Database

The application automatically creates a SQLite database at `Forevernote/data/database.db` on first run (when executed from the `Forevernote/` directory). The database includes tables for notes, folders, tags, and their relationships.

**Note**: The `data/` and `logs/` directories are created automatically when the application runs, not during compilation. The build scripts do not create these directories.

### Logging

Logging configuration is defined in `src/main/resources/logging.properties`. Logs are written to the `Forevernote/logs/` directory by default (created automatically on first run).

### JavaFX Module-Path

The build and run scripts automatically detect JavaFX modules from your Maven repository (`~/.m2/repository/org/openjfx/`). They use specific JAR files (not directories) to avoid loading `-sources.jar` files as modules.

If running the JAR directly fails, use the provided scripts which handle module-path configuration automatically.

## VS Code Configuration

The project includes pre-configured VS Code settings:

- **`.vscode/settings.json`**: Java 17 configuration, Maven settings, excluded files
- **`.vscode/tasks.json`**: Build tasks (compile, package, test, run)
- **`.vscode/launch.json`**: Debug/run configurations with JavaFX support
- **`.vscode/extensions.json`**: Recommended extensions

### VS Code Tasks

Available tasks (press `Ctrl+Shift+P` → `Tasks: Run Task`):

- **maven-compile**: Compile the project
- **maven-package**: Build the JAR
- **maven-test**: Run unit tests
- **maven-exec-java**: Run via Maven (handles JavaFX automatically)

### VS Code Launch Configurations

Available configurations (press `F5` or go to Run and Debug):

- **Launch Forevernote (Maven JavaFX)**: Uses Maven JavaFX plugin (recommended)
- **Launch Forevernote (Debug)**: Manual module-path configuration for debugging

## Troubleshooting

### Build Warnings

**Warning**: "Failed to build parent project for org.openjfx:javafx-*"
- **Status**: Normal and harmless
- **Explanation**: Maven tries to build the JavaFX parent project, which is not necessary
- **Action**: Ignore these warnings - they don't affect functionality

**Warning**: "6 problems were encountered while building the effective model"
- **Status**: Normal and harmless
- **Explanation**: Related to the JavaFX parent project warnings
- **Action**: Ignore - build will still succeed

### Runtime Errors

**Error**: "JavaFX runtime components are missing"
- **Solution**: Use the run scripts (`.\scripts\run_all.ps1` or `./scripts/run_all.sh`)
- **Alternative**: Run via Maven: `mvn javafx:run` or `mvn exec:java -Dexec.mainClass="com.example.forevernote.Main"`

**Error**: "Invalid module name: '21' is not a Java identifier"
- **Solution**: This was fixed in the scripts. Make sure you're using the latest version of the scripts
- **Cause**: Scripts were pointing to directories containing `-sources.jar` files
- **Fix**: Scripts now use specific JAR file paths

### VS Code Issues

**Problem**: JavaFX imports show errors
- **Solution**: 
  1. `Ctrl+Shift+P` → `Java: Clean Java Language Server Workspace`
  2. `Ctrl+Shift+P` → `Java: Reload Projects`
  3. Wait for Maven to sync (1-2 minutes)

**Problem**: VS Code uses wrong Java version
- **Solution**: 
  1. `Ctrl+Shift+P` → `Java: Configure Java Runtime`
  2. Set Java 17 as default
  3. Check `.vscode/settings.json` has `"java.jdt.ls.java.home": "C:\\Program Files\\Java\\jdk-17"` (adjust path for your system)

**Problem**: Can't run from VS Code
- **Solution**: Use "Launch Forevernote (Maven JavaFX)" configuration which handles everything automatically

## Development

### Running Tests

```bash
cd Forevernote
mvn test
```

### Code Style

- Follow Java naming conventions (PascalCase for classes, camelCase for methods/variables)
- Use `LoggerConfig.getLogger(ClassName.class)` for logging (not `System.out.println`)
- Handle exceptions appropriately (use custom exceptions from `exceptions/` package)

### Adding Dependencies

Add dependencies to `Forevernote/pom.xml` in the `<dependencies>` section. Maven will automatically download them.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

For detailed development guidelines, see [AGENTS.md](AGENTS.md).
