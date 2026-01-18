# Forevernote Plugins Source

This directory contains the source code for Forevernote's built-in plugins.

## Important: Plugin Architecture

**All plugins (including built-in ones) must be packaged as JAR files and placed in the `plugins/` directory.**

The core application has **zero knowledge** of any specific plugins. This ensures complete decoupling and allows the community to create and share plugins independently.

## Building Plugins

To build these plugins into JAR files:

### Option 1: Use the Build Script (Recommended)

```powershell
# From the project root
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"  # Set your Java path
.\scripts\build-plugins.ps1
```

The script will:
1. Compile the core application
2. Compile each plugin
3. Package each plugin as a JAR with manifest
4. Copy JARs to `Forevernote/plugins/`

### Option 2: Manual Build

1. **Compile the core application first:**
   ```bash
   cd Forevernote
   mvn compile
   ```

2. **For each plugin, create a JAR:**
   ```bash
   # Example for AIPlugin
   javac -cp "target/classes;~/.m2/repository/org/openjfx/javafx-*/21/*/javafx-*.jar" \
         -d temp \
         com/example/forevernote/plugin/builtin/AIPlugin.java
   
   # Create manifest
   echo "Manifest-Version: 1.0" > MANIFEST.MF
   echo "Plugin-Class: com.example.forevernote.plugin.builtin.AIPlugin" >> MANIFEST.MF
   
   # Create JAR
   jar cfm AIPlugin.jar MANIFEST.MF -C temp .
   
   # Copy to plugins directory
   cp AIPlugin.jar ../Forevernote/plugins/
   ```

## Plugin Structure

Each plugin must:
- Implement the `Plugin` interface
- Have a unique plugin ID
- Be packaged as a JAR file
- Include `Plugin-Class` in the manifest (or be auto-detectable)

## Available Plugins

- **WordCountPlugin** - Word and character counting
- **ReadingTimePlugin** - Reading time estimation
- **DailyNotesPlugin** - Daily note management
- **TemplatesPlugin** - Note templates
- **TableOfContentsPlugin** - TOC generation
- **AutoBackupPlugin** - Automatic backups
- **AIPlugin** - AI-powered features (summarize, translate, etc.)

## Distribution

When distributing Forevernote:
1. Build the core application JAR
2. Build all plugins as JARs
3. Place plugin JARs in `plugins/` directory
4. Users can add/remove plugins by managing JAR files

## Community Plugins

Community members can create their own plugins by:
1. Implementing the `Plugin` interface
2. Packaging as a JAR
3. Placing in the `plugins/` directory

See `doc/PLUGINS.md` for detailed plugin development guide.
