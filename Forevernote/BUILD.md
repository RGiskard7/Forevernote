# Build Instructions

## Prerequisites

- **Java 17** or higher
- **Apache Maven 3.6** or higher

Verify installation:
```bash
java -version
mvn -version
```

## Build Methods

### Method 1: Using Build Scripts (Recommended)

**Windows:**
```powershell
cd Forevernote
..\scripts\build_all.ps1
```

**macOS/Linux:**
```bash
cd Forevernote
../scripts/build_all.sh
```

This creates an executable JAR at `target/forevernote-1.0.0-uber.jar`.

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

### Method 3: Using IDE

1. Import project as Maven project in your IDE
2. Set Java 17 as project SDK
3. Open `src/main/java/com/example/forevernote/Main.java`
4. Run the file or use IDE's Run/Debug features

## Output

After a successful build, the output files are located in the `target/` directory:

- `forevernote-1.0.0-uber.jar` - Executable JAR with all dependencies (recommended)
- `forevernote-1.0.0.jar` - Basic JAR (requires dependencies on classpath)

## Running the Application

### From JAR

```bash
java -jar target/forevernote-1.0.0-uber.jar
```

### From Maven (Development)

```bash
mvn exec:java -Dexec.mainClass="com.example.forevernote.Main"
```

### Using Run Scripts

**Windows:**
```powershell
..\scripts\run_all.ps1
```

**macOS/Linux:**
```bash
../scripts/run_all.sh
```

## Troubleshooting

### JavaFX Module Errors

If you see "JavaFX runtime components are missing" when running the JAR directly:

1. Use the run scripts which automatically configure the module-path
2. Or run via Maven: `mvn exec:java -Dexec.mainClass="com.example.forevernote.Main"`

### Compilation Errors

Ensure:
- Java 17 is set as the project SDK
- Maven has access to internet (for downloading dependencies)
- `pom.xml` is in the Forevernote directory

### Clean Build

If you experience issues, perform a clean rebuild:

```bash
mvn clean
mvn package -DskipTests
```

## Project Structure

```
src/
├── main/
│   ├── java/com/example/forevernote/
│   │   ├── Main.java                    # Application entry point
│   │   ├── config/                      # Configuration classes
│   │   ├── data/                        # Data access layer
│   │   │   ├── SQLiteDB.java           # Database management
│   │   │   ├── dao/                    # Data Access Objects
│   │   │   └── models/                 # Data models
│   │   ├── exceptions/                 # Custom exceptions
│   │   └── ui/                         # User interface
│   │       ├── controller/             # FXML controllers
│   │       ├── view/                   # FXML layouts
│   │       └── css/                    # Stylesheets
│   └── resources/
│       ├── logging.properties           # Logging configuration
│       └── com/example/forevernote/ui/
│           ├── css/                     # CSS files
│           ├── view/                    # FXML files
│           └── images/                  # Application icons
└── test/
    └── java/com/example/forevernote/   # Unit tests
```

## Dependencies

Main dependencies are configured in `pom.xml`:

- **JavaFX 21**: GUI framework (`javafx-controls`, `javafx-fxml`, `javafx-graphics`, `javafx-media`)
- **SQLite JDBC**: Database driver (`sqlite-jdbc`)
- **JUnit 5**: Testing framework (`junit-jupiter`)
- **SLF4J**: Logging bridges (`slf4j-api`, `slf4j-jdk14`)

All dependencies are automatically downloaded by Maven.


## Features Implemented
- ✅ Bug fixes for critical issues
- ✅ Modern JavaFX UI with SplitPane layout
- ✅ TreeView for folder hierarchy
- ✅ Note editor with metadata
- ✅ CRUD operations from UI
- ✅ Modern CSS styling
- ✅ Search functionality
- ✅ Tags management
- ✅ Recent notes and favorites

## Next Steps
- [ ] Add dark theme support
- [ ] Implement markdown rendering
- [ ] Add attachment support
- [ ] Implement export/import functionality
- [ ] Add keyboard shortcuts
- [ ] Implement auto-save
- [ ] Add spell checking
- [ ] Implement search with filters