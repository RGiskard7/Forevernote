# 🚀 How to Use Forevernote as a Standalone Application

This guide explains how to build and run Forevernote as a standalone application, without needing VS Code or Maven.

## 📦 Option 1: Executable JAR (Recommended)

### Step 1: Build the Executable JAR

Open PowerShell/CMD (Windows) or Terminal (macOS/Linux) in the project root and run:

**Windows:**
```powershell
.\scripts\build_all.ps1
```

**macOS/Linux:**
```bash
./scripts/build_all.sh
```

**Or manually with Maven:**
```bash
cd Forevernote
mvn clean package -DskipTests
```

This will generate: `Forevernote/target/forevernote-1.0.0-uber.jar`

### Step 2: Run the JAR

**Option A: Use the launch script (Recommended)**

**Windows:**
```powershell
# PowerShell (recommended)
.\scripts\launch-forevernote.ps1

# CMD (alternative)
.\scripts\launch-forevernote.bat
```

**macOS/Linux:**
```bash
./scripts/launch-forevernote.sh
```

**Option B: Run directly with Java**

**Windows:**
```bash
java --module-path "%USERPROFILE%\.m2\repository\org\openjfx\javafx-controls\21.0.1" --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.web -jar Forevernote\target\forevernote-1.0.0-uber.jar
```

**macOS/Linux:**
```bash
java --module-path "$HOME/.m2/repository/org/openjfx/javafx-controls/21.0.1" --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.web -jar Forevernote/target/forevernote-1.0.0-uber.jar
```

**Note**: Adjust the JavaFX version (21.0.1) according to what you have installed.

## 🎯 Option 2: Native Installers (Best for Distribution)

### Generate Native Installers

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

This will create native installers that include Java, so users don't need to install Java separately.

## 📋 Requirements

To run Forevernote you need:

1. **Java 17 or higher** installed
   - Check with: `java -version`
   - Download from: https://adoptium.net/

2. **The executable JAR** (see Step 1)

3. **JavaFX** (automatically downloaded with Maven, or use the launch script)

## 🔧 Troubleshooting

### Error: "JavaFX runtime components are missing"

The JAR needs JavaFX in the module-path. Use the `scripts/launch-forevernote.bat` (Windows) or `scripts/launch-forevernote.sh` (macOS/Linux) scripts that configure this automatically.

### Error: "Java not found"

Install Java 17 or higher from https://adoptium.net/ and make sure it's in your PATH.

### Error: "JAR not found"

Run the build script first: `.\scripts\build_all.ps1` (Windows) or `./scripts/build_all.sh` (macOS/Linux)

## 💡 Create a Desktop Shortcut

**Windows:**
1. Create a shortcut to `scripts/launch-forevernote.bat`
2. Right-click → Properties
3. In "Start in", enter the full path to the `Forevernote` folder
4. Optional: Change the icon

**macOS:**
1. Create an Automator application that runs the shell script
2. Or use the native installer (DMG) which creates a proper app bundle

**Linux:**
1. Create a `.desktop` file in `~/.local/share/applications/`
2. Or use the native installer (DEB/RPM) which integrates with the system

## 📁 File Structure

After building, you'll have:

```
Forevernote/
├── target/
│   └── forevernote-1.0.0-uber.jar  ← Executable JAR
├── data/
│   └── database.db                  ← Database (created automatically)
└── logs/
    └── app.log                      ← Logs (created automatically)
```

## 🎁 Distributing the Application

To share Forevernote with other users:

**Option 1: JAR + Launch Script**
1. Share the JAR: `forevernote-1.0.0-uber.jar`
2. Share the launch script: `scripts/launch-forevernote.bat` (Windows) or `scripts/launch-forevernote.sh` (macOS/Linux)
3. Indicate they need Java 17+ installed

**Option 2: Native Installer (Recommended)**
1. Generate the native installer for the target platform
2. Share the installer file (.msi, .dmg, .deb, or .rpm)
3. Users can install it like any other application (Java included)

---

**Note**: The "uber" JAR includes all dependencies, so it's large (~50-60 MB) but completely standalone.
