# Build Instructions

## Prerequisites

- **Java 17 JDK** (required - not just JRE)
- **Apache Maven 3.6** or higher

**Important**: You need the JDK (Java Development Kit), not just the JRE (Java Runtime Environment), as the JDK includes the compiler and development tools required for building.

Note: If `mvn` is not on your PATH, you can call a local Maven installation directly (example Windows):

```powershell
& 'C:\Users\<you>\.maven\maven-3.9.11\bin\mvn.cmd' -v
```

Environment variables (temporary examples):

PowerShell (temporary for session):

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
$env:Path = 'C:\Users\<you>\.maven\maven-3.9.11\bin;' + $env:Path
```

Verify installation:
```bash
java -version
# Should show: openjdk version "17" or java version "17"

mvn -version
# Should show: Apache Maven 3.x.x
```

## Build Methods

### Method 1: Using Build Scripts (Recommended)

The provided scripts handle directory setup and ensure proper execution context.

**Windows (PowerShell):**
```powershell
cd Forevernote
..\scripts\build_all.ps1
```

**macOS/Linux (Bash):**
```bash
cd Forevernote
../scripts/build_all.sh
```

This creates an executable JAR at `target/forevernote-1.0.0-uber.jar`.

**Note**: During compilation, you may see warnings like:
```
[WARNING] Failed to build parent project for org.openjfx:javafx-*
```

These warnings are **normal and harmless**. They occur because Maven tries to build the JavaFX parent project, which is not necessary. The build will still succeed and the application will work correctly.

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

For detailed VS Code setup instructions, see `.vscode/README.md`.

### Method 4: Using Other IDEs

1. Import project as Maven project in your IDE (IntelliJ IDEA, Eclipse, etc.)
2. Set Java 17 as project SDK
3. Open `src/main/java/com/example/forevernote/Main.java`
4. Run the file or use IDE's Run/Debug features

## Output

After a successful build, the output files are located in the `Forevernote/target/` directory:

- `forevernote-1.0.0-uber.jar` - Executable JAR with all dependencies (recommended, ~54MB)
- `forevernote-1.0.0.jar` - Basic JAR (requires dependencies on classpath)

## Running the Application

### Method 1: Using Run Scripts (Recommended)

The run scripts automatically configure the JavaFX module-path and handle all dependencies.

**Windows (PowerShell):**
```powershell
cd Forevernote
..\scripts\run_all.ps1
```

**macOS/Linux (Bash):**
```bash
cd Forevernote
../scripts/run_all.sh
```

### Method 2: From JAR (Requires JavaFX Module Path)

If you run the JAR directly, you must configure the JavaFX module-path manually:

```bash
java --module-path <path-to-javafx-jars> --add-modules javafx.base,javafx.controls,javafx.fxml,javafx.graphics,javafx.media,javafx.web -jar target/forevernote-1.0.0-uber.jar
```

**Note**: The `launch.bat` and `launch.sh` scripts in the root directory also handle this automatically.

### Method 3: From Maven (Development)

```bash
cd Forevernote
mvn exec:java -Dexec.mainClass="com.example.forevernote.Main"
```

Or using the JavaFX Maven plugin:

```bash
cd Forevernote
mvn javafx:run
```

## Runtime Directories

The application automatically creates the following directories at runtime (not during compilation):

- `Forevernote/data/` - Contains `database.db` (SQLite database)
- `Forevernote/logs/` - Contains `app.log` (application logs)

These directories are created automatically when the application starts. They are **not** created during the build process.

**Important**: These directories are excluded from Git (see `.gitignore`). Do not commit database files or logs to the repository.

## Troubleshooting

### JavaFX Module Errors

If you see "JavaFX runtime components are missing" when running the JAR directly:

1. **Use the run scripts** (recommended) - they automatically configure the module-path
2. **Or run via Maven**: `mvn exec:java -Dexec.mainClass="com.example.forevernote.Main"`
3. **Or use the JavaFX plugin**: `mvn javafx:run`

If you must run the JAR directly and encounter JavaFX module errors, run the app with the appropriate `--module-path` and `--add-modules` flags pointing at your JavaFX SDK or the JavaFX jars in your local Maven repository. The provided `launch.*` scripts already perform this step for you.

### Compilation Errors

Ensure:
- Java 17 JDK is installed and set as the project SDK
- `JAVA_HOME` environment variable points to Java 17
- Maven has access to internet (for downloading dependencies)
- `pom.xml` is in the `Forevernote/` directory
- You're running Maven commands from the `Forevernote/` directory

### Build Warnings

**JavaFX Parent Project Warnings**:
```
[WARNING] Failed to build parent project for org.openjfx:javafx-*
```

These warnings are **normal and can be ignored**. They occur because Maven attempts to build the JavaFX parent project, which is not necessary. The build will succeed and the application will work correctly.

### Clean Build

If you experience issues, perform a clean rebuild:

```bash
cd Forevernote
mvn clean
mvn package -DskipTests
```

### VS Code Issues

If VS Code shows compilation errors despite successful Maven builds:

1. Clean Java Language Server workspace: `Ctrl+Shift+P` → `Java: Clean Java Language Server Workspace`
2. Reload Maven projects: `Ctrl+Shift+P` → `Java: Reload Projects`
3. Verify Java 17 is configured: `Ctrl+Shift+P` → `Java: Configure Java Runtime`

For detailed VS Code troubleshooting, see `.vscode/README.md`.

## Project Structure

```
Forevernote/
├── src/
│   ├── main/
│   │   ├── java/com/example/forevernote/
│   │   │   ├── Main.java                    # Application entry point
│   │   │   ├── config/                      # Configuration classes
│   │   │   │   └── LoggerConfig.java        # Logging configuration
│   │   │   ├── data/                        # Data access layer
│   │   │   │   ├── SQLiteDB.java           # Database management
│   │   │   │   ├── dao/                    # Data Access Objects
│   │   │   │   │   ├── NoteDAOSQLite.java
│   │   │   │   │   ├── FolderDAOSQLite.java
│   │   │   │   │   └── TagDAOSQLite.java
│   │   │   │   └── models/                 # Data models
│   │   │   │       ├── Note.java
│   │   │   │       ├── Folder.java
│   │   │   │       └── Tag.java
│   │   │   ├── exceptions/                 # Custom exceptions
│   │   │   ├── ui/                         # User interface
│   │   │   │   ├── controller/             # FXML controllers
│   │   │   │   │   └── MainController.java
│   │   │   │   └── css/                    # Stylesheets
│   │   │   │       └── modern-theme.css
│   │   │   └── util/                       # Utility classes
│   │   │       ├── KeyboardShortcuts.java
│   │   │       ├── MarkdownProcessor.java
│   │   │       └── Animations.java
│   │   └── resources/
│   │       ├── logging.properties           # Logging configuration
│   │       └── com/example/forevernote/ui/
│   │           ├── css/                     # CSS files
│   │           ├── view/                    # FXML files
│   │           │   └── MainView.fxml
│   │           └── images/                  # Application icons
│   └── test/
│       └── java/com/example/forevernote/   # Unit tests
├── target/                                  # Build output (generated)
│   └── forevernote-1.0.0-uber.jar         # Executable JAR
├── data/                                    # Runtime directory (created at runtime)
│   └── database.db                         # SQLite database
├── logs/                                    # Runtime directory (created at runtime)
│   └── app.log                             # Application logs
├── pom.xml                                  # Maven configuration
└── scripts/                                    # Build and run scripts
    ├── build_all.ps1
    ├── build_all.sh
    ├── run_all.ps1
    └── run_all.sh
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
