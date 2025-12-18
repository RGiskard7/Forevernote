# Build and Setup Guide

Complete guide for building, running, and distributing Forevernote.

## Prerequisites

### Required Software

1. **Java 17 JDK** (required - not just JRE)
   - **Important**: You need the JDK (Java Development Kit), not just the JRE (Java Runtime Environment)
   - Download from: https://adoptium.net/ or https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html
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
- **Extension Pack for Java** (includes Java support, debugger, test runner, etc.)
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

**Or manually with Maven:**
```bash
cd Forevernote
mvn clean package -DskipTests
```

This creates an executable JAR at `Forevernote/target/forevernote-1.0.0-uber.jar`.

**Note**: During compilation, you may see warnings like:
```
[WARNING] Failed to build parent project for org.openjfx:javafx-*
```

These warnings are **normal and harmless**. They occur because Maven tries to build the JavaFX parent project, which is not necessary. The build will still succeed.

### 3. Run the Application

**Windows (PowerShell):**
```powershell
.\scripts\run_all.ps1
```

**macOS/Linux (Bash):**
```bash
./scripts/run_all.sh
```

**Or use the launch scripts:**
```powershell
# Windows
.\scripts\launch-forevernote.bat
```

```bash
# macOS/Linux
./scripts/launch-forevernote.sh
```

The scripts automatically:
- Detect JavaFX modules in your Maven repository
- Configure the Java module-path correctly
- Launch the application

## Build Methods

### Method 1: Using Build Scripts (Recommended)

The provided scripts handle directory setup and ensure proper execution context.

**Windows (PowerShell):**
```powershell
.\scripts\build_all.ps1
```

**macOS/Linux (Bash):**
```bash
./scripts/build_all.sh
```

### Method 2: Using Maven Directly

```bash
cd Forevernote

# Clean and compile
mvn clean compile

# Package as uber-JAR (includes all dependencies)
mvn clean package -DskipTests

# Run tests
mvn test

# Full build with tests
mvn clean package
```

### Method 3: Using VS Code

1. Install the **Extension Pack for Java** in VS Code
2. Open the project folder in VS Code
3. VS Code will automatically detect the Maven project
4. Use the integrated terminal to run Maven commands, or:
   - Press `F5` to run (uses configured launch.json)
   - Use the Run and Debug panel (Ctrl+Shift+D)

**Compile in VS Code:**
- Press `Ctrl+Shift+B` (or `Cmd+Shift+B` on macOS)
- Select **"maven-compile"** to compile
- Or select **"maven-package"** to build the JAR

**Run in VS Code:**
- Press `F5` or go to **Run and Debug** (Ctrl+Shift+D)
- Select **"Launch Forevernote (Maven JavaFX)"** from the dropdown
- Click the green play button or press `F5`

For detailed VS Code setup instructions, see `.vscode/README.md`.

### Method 4: Using Other IDEs

1. Import project as Maven project in your IDE (IntelliJ IDEA, Eclipse, etc.)
2. Set Java 17 as project SDK
3. Open `src/main/java/com/example/forevernote/Main.java`
4. Run the file or use IDE's Run/Debug features

## Running the Application

### Method 1: Using Run Scripts (Recommended)

The run scripts automatically configure the JavaFX module-path and handle all dependencies.

**Windows (PowerShell):**
```powershell
.\scripts\run_all.ps1
```

**macOS/Linux (Bash):**
```bash
./scripts/run_all.sh
```

### Method 2: Using Launch Scripts

**Windows:**
```powershell
.\scripts\launch-forevernote.bat
```

**macOS/Linux:**
```bash
./scripts/launch-forevernote.sh
```

### Method 3: From JAR (Requires JavaFX Module Path)

If you run the JAR directly, you must configure the JavaFX module-path manually:

```bash
java --module-path <path-to-javafx-jars> --add-modules javafx.base,javafx.controls,javafx.fxml,javafx.graphics,javafx.media,javafx.web -jar target/forevernote-1.0.0-uber.jar
```

**Note**: The `scripts/launch-forevernote.bat` (Windows) and `scripts/launch-forevernote.sh` (macOS/Linux) scripts also handle this automatically.

### Method 4: From Maven (Development)

```bash
cd Forevernote
mvn exec:java -Dexec.mainClass="com.example.forevernote.Main"
```

Or using the JavaFX Maven plugin:

```bash
cd Forevernote
mvn javafx:run
```

## Output

After a successful build, the output files are located in the `Forevernote/target/` directory:

- `forevernote-1.0.0-uber.jar` - Executable JAR with all dependencies (recommended, ~50-60 MB)
- `forevernote-1.0.0.jar` - Basic JAR (requires dependencies on classpath)

## Runtime Directories

The application automatically creates the following directories at runtime (not during compilation):

- `Forevernote/data/` - Contains `database.db` (SQLite database)
- `Forevernote/logs/` - Contains `app.log` (application logs)

These directories are created automatically when the application starts. They are **not** created during the build process.

**Important**: 
- The build scripts (`build_all.ps1` / `build_all.sh`) **do NOT** create these directories
- They are only created automatically when you run the application for the first time
- The run scripts (`run_all.ps1` / `run_all.sh`) ensure the application runs from the `Forevernote/` directory, so the directories are created in the correct location
- These directories are excluded from Git (see `.gitignore`). Do not commit database files or logs to the repository.

## Native Installers

For creating native installers (MSI, DMG, DEB/RPM), see `scripts/PACKAGING.md`.

**Windows (MSI):**
```powershell
.\scripts\package-windows.ps1
```

**macOS (DMG):**
```bash
./scripts/package-macos.sh
```

**Linux (DEB/RPM):**
```bash
./scripts/package-linux.sh
```

## Troubleshooting

### Build Warnings

**Warning**: "Failed to build parent project for org.openjfx:javafx-*"
- **Status**: Normal and harmless
- **Explanation**: Maven tries to build the JavaFX parent project, which is not necessary
- **Action**: Ignore these warnings - they don't affect functionality

**Warning**: "6 problems were encountered while building the effective model"
- **Status**: Normal and harmless
- **Explanation**: Related to JavaFX parent project warnings
- **Action**: Ignore - the build will still succeed

### Runtime Errors

**Error**: "JavaFX runtime components are missing"

**Solution 1 (Recommended)**: Use the provided scripts:
```powershell
# Windows
.\scripts\run_all.ps1
# or
.\scripts\launch-forevernote.bat
```

```bash
# macOS/Linux
./scripts/run_all.sh
# or
./scripts/launch-forevernote.sh
```

**Solution 2**: Run via Maven:
```bash
cd Forevernote
mvn javafx:run
```

Or:
```bash
mvn exec:java -Dexec.mainClass="com.example.forevernote.Main"
```

**Error**: "Invalid module name: '21' is not a Java identifier"
- **Solution**: This error was fixed in the scripts. Make sure you're using the latest version of the scripts
- **Cause**: Scripts were pointing to directories containing `-sources.jar` files
- **Fix**: Scripts now use specific JAR paths

### Maven Issues

**Maven not found**

The scripts will attempt to install it automatically. If you prefer to install manually:

- **Windows**: Download from https://maven.apache.org/download.cgi
- **macOS**: `brew install maven`
- **Linux (Ubuntu/Debian)**: `sudo apt-get install maven`

**Maven not in PATH**

If Maven is installed but not in PATH, you can use the full path:

**Windows:**
```powershell
& 'C:\Users\<you>\.maven\maven-3.9.11\bin\mvn.cmd' -f Forevernote/pom.xml clean package -DskipTests
```

**macOS/Linux:**
```bash
/usr/local/bin/mvn -f Forevernote/pom.xml clean package -DskipTests
```

Or configure temporary environment variables:

**PowerShell (temporary for session):**
```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
$env:Path = 'C:\Users\<you>\.maven\maven-3.9.11\bin;' + $env:Path
```

**Bash (temporary for session):**
```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export PATH=$JAVA_HOME/bin:$PATH
```

### VS Code Issues

**Problem**: VS Code shows errors for JavaFX imports
- **Solution**: 
  1. `Ctrl+Shift+P` → `Java: Clean Java Language Server Workspace`
  2. `Ctrl+Shift+P` → `Java: Reload Projects`
  3. Wait for Maven to sync (1-2 minutes)

**Problem**: VS Code uses wrong Java version
- **Solution**: 
  1. `Ctrl+Shift+P` → `Java: Configure Java Runtime`
  2. Set Java 17 as default
  3. Verify `.vscode/settings.json` has `"java.jdt.ls.java.home": "C:\\Program Files\\Java\\jdk-17"` (adjust path for your system)

**Problem**: Can't run from VS Code
- **Solution**: Use the "Launch Forevernote (Maven JavaFX)" configuration which handles everything automatically

**Problem**: "Could not find or load main class"
- **Solution**: Make sure compilation was successful. Recompile with:
  ```bash
  cd Forevernote
  mvn clean compile
  ```

**Problem**: Application opens but closes immediately
- **Solution**: Verify all JavaFX dependencies are downloaded:
  ```bash
  cd Forevernote
  mvn clean compile
  ```

### Clean Build

If you experience issues, perform a clean rebuild:

```bash
cd Forevernote
mvn clean
mvn package -DskipTests
```

## Project Structure

```
Forevernote/
├── Forevernote/              # Main project module
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/forevernote/
│   │   │   │   ├── Main.java                    # Application entry point
│   │   │   │   ├── config/                      # Configuration classes
│   │   │   │   │   └── LoggerConfig.java        # Logging configuration
│   │   │   │   ├── data/                        # Data access layer
│   │   │   │   │   ├── SQLiteDB.java           # Database management
│   │   │   │   │   ├── dao/                    # Data Access Objects
│   │   │   │   │   │   ├── NoteDAOSQLite.java
│   │   │   │   │   │   ├── FolderDAOSQLite.java
│   │   │   │   │   │   └── TagDAOSQLite.java
│   │   │   │   │   └── models/                 # Data models
│   │   │   │   │       ├── Note.java
│   │   │   │   │       ├── Folder.java
│   │   │   │   │       └── Tag.java
│   │   │   │   ├── exceptions/                 # Custom exceptions
│   │   │   │   ├── ui/                         # User interface
│   │   │   │   │   ├── controller/             # FXML controllers
│   │   │   │   │   │   └── MainController.java
│   │   │   │   │   └── css/                    # Stylesheets
│   │   │   │   └── util/                       # Utility classes
│   │   │   │       ├── KeyboardShortcuts.java
│   │   │   │       ├── MarkdownProcessor.java
│   │   │   │       └── Animations.java
│   │   │   └── resources/
│   │   │       ├── logging.properties           # Logging configuration
│   │   │       └── com/example/forevernote/ui/
│   │   │           ├── css/                     # CSS files
│   │   │           ├── view/                    # FXML files
│   │   │           │   └── MainView.fxml
│   │   │           └── images/                  # Application icons
│   │   └── test/
│   │       └── java/com/example/forevernote/   # Unit tests
│   ├── target/                                  # Build output (generated)
│   │   └── forevernote-1.0.0-uber.jar         # Executable JAR
│   ├── data/                                    # Runtime directory (created at runtime)
│   │   └── database.db                         # SQLite database
│   ├── logs/                                    # Runtime directory (created at runtime)
│   │   └── app.log                             # Application logs
│   └── pom.xml                                  # Maven configuration
├── scripts/                                     # Build and run scripts
│   ├── build_all.ps1
│   ├── build_all.sh
│   ├── run_all.ps1
│   ├── run_all.sh
│   ├── launch-forevernote.bat
│   ├── launch-forevernote.sh
│   └── package-*.{ps1,sh}                      # Native installer scripts
├── doc/                                         # Documentation
│   ├── BUILD.md                                 # This file
│   ├── LAUNCH_APP.md                           # Standalone application guide
│   └── PACKAGING.md                            # Native installer guide
├── .vscode/                                     # VS Code configuration
│   ├── settings.json                            # Java configuration
│   ├── tasks.json                               # Build tasks
│   ├── launch.json                              # Run configurations
│   └── extensions.json                          # Recommended extensions
├── README.md                                    # Main project README
├── AGENTS.md                                    # Agent-oriented development guide
└── changelog.md                                 # Project changelog
```

## Dependencies

Main dependencies are configured in `pom.xml`:

- **JavaFX 21**: GUI framework
  - `javafx-controls` - UI controls
  - `javafx-fxml` - FXML layout support
  - `javafx-graphics` - Graphics rendering
  - `javafx-media` - Media support (required by javafx.web)
  - `javafx-web` - WebView for Markdown preview
- **SQLite JDBC**: Database driver (`sqlite-jdbc`)
- **CommonMark**: Markdown processing (`commonmark`, `commonmark-ext-gfm-tables`, `commonmark-ext-gfm-strikethrough`, `commonmark-ext-autolink`)
- **JUnit 5**: Testing framework (`junit-jupiter`)
- **SLF4J**: Logging bridges (`slf4j-api`, `slf4j-jdk14`)
- **ControlsFX**: Additional JavaFX controls (`controlsfx`)

All dependencies are automatically downloaded by Maven from Maven Central.

## Build Configuration

The project uses Maven with the following key plugins:

- **maven-compiler-plugin**: Compiles Java 17 source code
- **maven-assembly-plugin**: Creates the uber-JAR with all dependencies
- **javafx-maven-plugin**: Provides JavaFX-specific build goals
- **exec-maven-plugin**: Allows running the application via Maven

## Features Implemented

- ✅ Modern JavaFX UI with SplitPane layout
- ✅ TreeView for folder hierarchy with visible "All Notes" root
- ✅ Note editor with metadata (title, content, dates, tags)
- ✅ Full CRUD operations from UI (Create, Read, Update, Delete)
- ✅ Modern CSS styling with theme support
- ✅ Global search functionality across all notes
- ✅ Tags management with full CRUD interface
- ✅ Markdown support with live preview and emoji rendering
- ✅ Rich text formatting (Bold, Italic, Underline, Links, Images)
- ✅ Lists support (Todo lists, Numbered lists)
- ✅ Zoom controls (50%-300%)
- ✅ Keyboard shortcuts for all operations
- ✅ Auto-refresh notes list on save/delete
- ✅ Recent notes and favorites
- ✅ Comprehensive logging system
- ✅ Folder hierarchy display (subfolders correctly nested)
- ✅ Tag synchronization (tags appear in sidebar after creation)
- ✅ Root folder creation (easy creation of folders at root level)

## Next Steps (Future Enhancements)

- [ ] Complete dark theme implementation
- [ ] Add attachment support (file attachments)
- [ ] Implement export/import functionality (Markdown, PDF, etc.)
- [ ] Add spell checking
- [ ] Implement advanced search with filters
- [ ] Add note templates
- [ ] Implement note versioning/history
- [ ] Add collaboration features (if needed)

## Notes

- The application is **offline-first** - no network connection required
- Database and logs are stored locally in `Forevernote/data/` and `Forevernote/logs/`
- The build process does **not** create runtime directories (`data/`, `logs/`) - these are created automatically when the application starts

