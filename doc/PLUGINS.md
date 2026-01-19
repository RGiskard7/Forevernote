# Forevernote Plugin System

This document describes the plugin system architecture in Forevernote, enabling extensibility and customization.

## Overview

Forevernote includes a robust plugin system that allows extending the application's functionality. Plugins can:

- Register commands in the Command Palette (Ctrl+P)
- **Register menu items dynamically** in categorized submenus (Core, Productivity, Utilities, AI, etc.)
- Subscribe to application events
- Access notes, folders, and tags through services
- Create custom UI dialogs
- Request to open notes in the editor
- Trigger UI refreshes

**Important:** The core application is completely decoupled from plugins. All menu items are registered dynamically by plugins during initialization.

## Plugin Manager (Obsidian-style UI)

Access the Plugin Manager via:
- **Ctrl+P** then search "Plugins: Manage Plugins"
- **Tools > Plugins > Manage Plugins** from the menu

The Plugin Manager provides:
- Visual list of all installed plugins
- Toggle switches to enable/disable plugins
- Plugin information (name, version, author, description)
- Status indicators (enabled/disabled)
- Theme-aware styling (light/dark)

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      Plugin Layer                           │
│  ┌───────────┐ ┌───────────┐ ┌───────────┐ ┌─────────────┐ │
│  │ WordCount │ │DailyNotes │ │ Templates │ │ AutoBackup  │ │
│  └───────────┘ └───────────┘ └───────────┘ └─────────────┘ │
│  ┌───────────┐ ┌─────────────────────────────────────────┐ │
│  │ReadingTime│ │       Table of Contents                 │ │
│  └───────────┘ └─────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│                    Plugin Manager                           │
│  ┌─────────────────────────────────────────────────────────┐│
│  │ Registration | Lifecycle | Dependencies | State Mgmt   ││
│  └─────────────────────────────────────────────────────────┘│
├─────────────────────────────────────────────────────────────┤
│                    Plugin Context                           │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────────┐│
│  │ Services    │ │ EventBus    │ │ Command Palette         ││
│  │ - Note      │ │ - Subscribe │ │ - Register commands     ││
│  │ - Folder    │ │ - Publish   │ │ - Unregister commands   ││
│  │ - Tag       │ │             │ │                         ││
│  └─────────────┘ └─────────────┘ └─────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
```

## Plugin Interface

All plugins must implement the `Plugin` interface:

```java
public interface Plugin {
    // Required
    String getId();           // Unique ID (e.g., "word-count")
    String getName();         // Display name (e.g., "Word Count")
    String getVersion();      // Semantic version (e.g., "1.0.0")
    
    void initialize(PluginContext context);  // Called on startup
    void shutdown();                          // Called on shutdown
    
    // Optional (have defaults)
    String getDescription();  // Plugin description (default: "")
    String getAuthor();       // Plugin author (default: "")
    boolean isEnabled();      // Enable/disable (default: true)
    int getPriority();        // Load order (default: 100, lower = earlier)
    String[] getDependencies(); // Required plugins (default: empty)
}
```

## Plugin Context API

The `PluginContext` provides access to application services:

### Service Access

```java
// Access notes
NoteService noteService = context.getNoteService();
List<Note> allNotes = noteService.getAllNotes();
Note newNote = noteService.createNote("Title", "Content");

// Access folders
FolderService folderService = context.getFolderService();
List<Folder> folders = folderService.getAllFolders();
Folder folder = folderService.createFolder("My Folder");

// Access tags
TagService tagService = context.getTagService();
List<Tag> tags = tagService.getAllTags();
```

### Command Registration

```java
// Simple command
context.registerCommand("My Command", "Description", this::myAction);

// Command with shortcut
context.registerCommand("My Command", "Description", "Ctrl+Shift+M", this::myAction);

// Unregister on shutdown
context.unregisterCommand("My Command");
```

### Menu Registration (Dynamic)

Plugins can register menu items in the **Tools > Plugins** menu dynamically:

```java
// Register a menu item in a category
context.registerMenuItem("Core", "My Action", this::myAction);

// Register with keyboard shortcut
context.registerMenuItem("AI", "Generate Summary", "Ctrl+Shift+G", this::generateSummary);

// Add a separator
context.addMenuSeparator("Utilities");

// Available categories (create your own or use existing):
// - "Core" - Core functionality (Word Count, Reading Time)
// - "Productivity" - Productivity tools (Templates, TOC, Daily Notes)
// - "Utilities" - Utility functions (Backup, Export)
// - "AI" - AI-powered features
// - Custom categories are created automatically
```

**Note:** Menu items are automatically removed when a plugin is disabled.

### Side Panel Registration (UI Modification - Obsidian-style)

Plugins can add custom UI panels to the right sidebar:

```java
// Create a custom panel with JavaFX components
VBox myPanel = new VBox();
myPanel.getChildren().add(new Label("Hello from plugin!"));
myPanel.getChildren().add(new Button("Do Something"));

// Register the panel (appears in right sidebar when info panel is shown)
context.registerSidePanel("my-panel", "My Plugin Panel", myPanel);

// Register with an icon/emoji
context.registerSidePanel("calendar", "Calendar", calendarWidget, "📅");

// Remove when plugin shuts down
context.removeSidePanel("my-panel");
```

**Example: CalendarPlugin** creates a mini calendar in the sidebar that:
- Shows current month with day buttons
- Highlights today (blue) and days with notes (green)
- Click any date to open/create a daily note
- Navigates between months

### Event System

```java
// Subscribe to events
context.subscribe(NoteEvents.NoteSelectedEvent.class, event -> {
    Note note = event.getNote();
    context.log("Note selected: " + note.getTitle());
});

// Publish events
context.publish(new NoteEvents.NoteSavedEvent(note));
```

### UI Interaction

```java
// Open a note in the editor
context.requestOpenNote(note);

// Refresh the notes list
context.requestRefreshNotes();

// Show dialogs
context.showInfo("Title", "Header", "Content message");
context.showError("Error Title", "Error message");
```

### Logging

```java
context.log("Plugin action executed");
context.logError("Something went wrong", exception);
```

## Built-in Plugins (8 Total)

### 1. Word Count Plugin (`word-count`)

Displays word and character statistics for notes.

**Commands:**
| Command | Shortcut | Description |
|---------|----------|-------------|
| Word Count: Current Note | Ctrl+Shift+W | Statistics for current note |
| Word Count: All Notes | - | Total statistics across all notes |

**Statistics provided:**
- Word count
- Character count (with/without spaces)
- Line count
- Paragraph count

---

### 2. Daily Notes Plugin (`daily-notes`)

Creates and manages daily notes with automatic dating.

**Commands:**
| Command | Shortcut | Description |
|---------|----------|-------------|
| Daily Notes: Open Today | Ctrl+Alt+D | Opens/creates today's note |
| Daily Notes: Open Yesterday | - | Opens yesterday's note |
| Daily Notes: Open Tomorrow | - | Opens/creates tomorrow's note |
| Daily Notes: This Week | - | Shows weekly overview |

**Features:**
- Automatic "Daily Notes" folder organization
- Template with tasks, notes, and reflection sections
- Date-based note titles (YYYY-MM-DD format)
- Automatically opens the created note in the editor

---

### 3. Reading Time Plugin (`reading-time`)

Estimates reading time for notes.

**Commands:**
| Command | Shortcut | Description |
|---------|----------|-------------|
| Reading Time: Current Note | Ctrl+Shift+R | Detailed reading time |
| Reading Time: All Notes | - | Total reading time |
| Reading Time: Quick Estimate | - | Quick overview |

**Reading speeds:**
- Slow reading: 150 wpm
- Average reading: 200 wpm
- Speed reading: 400 wpm
- Speaking: 150 wpm

---

### 4. Templates Plugin (`templates`)

Create notes from predefined templates.

**Commands:**
| Command | Shortcut | Description |
|---------|----------|-------------|
| Templates: New from Template | Ctrl+Shift+T | Select and create from template |
| Templates: Meeting Notes | - | Quick meeting notes |
| Templates: Project Plan | - | Quick project plan |
| Templates: Weekly Review | - | Quick weekly review |
| Templates: Checklist | - | Quick checklist |

**Built-in Templates (7):**
1. **Meeting Notes** - Agenda, discussion, action items
2. **Project Plan** - Goals, milestones, resources
3. **Weekly Review** - Accomplishments, challenges, learnings
4. **Checklist** - Simple task list
5. **Cornell Notes** - Study method template
6. **Blog Post** - Introduction, content, conclusion
7. **Bug Report** - Steps to reproduce, expected/actual behavior

**Template Variables:**
- `{{title}}` - Note title
- `{{date}}` - Current date (YYYY-MM-DD)
- `{{week}}` - Current week number

---

### 5. Table of Contents Plugin (`table-of-contents`)

Generate table of contents from Markdown headers.

**Commands:**
| Command | Shortcut | Description |
|---------|----------|-------------|
| TOC: Generate Table of Contents | Ctrl+Shift+O | Generate TOC with links |
| TOC: Preview Table of Contents | - | Preview without inserting |
| TOC: Generate Numbered TOC | - | Numbered outline format |

**Features:**
- Parses Markdown headers (# to ######)
- Creates clickable anchor links
- Supports numbered or bulleted format
- Copy to clipboard functionality

---

### 6. Auto Backup Plugin (`auto-backup`)

Backup notes to files and export database.

**Commands:**
| Command | Shortcut | Description |
|---------|----------|-------------|
| Backup: Export All Notes | Ctrl+Shift+B | Export all notes as .md files |
| Backup: Database Backup | - | Copy database file |
| Backup: Full Backup | - | Notes + database backup |
| Backup: Export Current Note | - | Export single note |

**Export Features:**
- Exports to Markdown (.md) format
- YAML frontmatter with metadata (title, dates, favorite)
- Automatic timestamped folder names
- Backup report generation

---

### 7. Calendar Plugin (`calendar`) - **UI Modification Demo**

Adds a mini calendar widget to the sidebar, demonstrating **Obsidian-style UI modification**.

**Commands:**
| Command | Shortcut | Description |
|---------|----------|-------------|
| Calendar: Show/Hide Panel | Ctrl+Shift+C | Toggle calendar visibility |
| Calendar: Go to Today | - | Navigate to current month |
| Calendar: Refresh | - | Refresh note dates |

**Features:**
- Mini calendar in the right sidebar (info panel area)
- Navigate between months (◀/▶ buttons)
- Click any date to open/create a daily note
- **Visual indicators:**
  - 🔵 Blue: Today
  - 🟢 Green: Days with daily notes
  - 🔴 Red text: Weekends
- Integrates with Daily Notes plugin format

**UI Modification Example:**
This plugin demonstrates how plugins can add custom JavaFX components to the application interface using `context.registerSidePanel()`.

---

### 8. AI Assistant Plugin (`ai-assistant`) - **Example/Demo**

Integrates AI capabilities into Forevernote using external AI APIs.

**⚠️ Note:** This plugin is included as a **demonstration** of how to integrate AI services. It requires:
- An API key from an AI service (OpenAI, Anthropic, etc.)
- Configuration via the plugin's settings dialog

**Commands:**
| Command | Shortcut | Description |
|---------|----------|-------------|
| AI: Summarize Note | Ctrl+Shift+S | Generate AI summary |
| AI: Translate Note | - | Translate to another language |
| AI: Improve Writing | - | Fix grammar and style |
| AI: Generate Content | - | Generate new content from prompt |
| AI: Configure API | - | Set API key and endpoint |

**Features:**
- Summarize notes using AI
- Translate notes to multiple languages
- Improve grammar and writing style
- Generate new content from prompts
- Configurable API endpoint (OpenAI-compatible)

**To Enable:**
1. Place `AIPlugin.jar` in the `plugins/` directory
2. Run `.\scripts\build-plugins.ps1` if you need to rebuild
3. Configure your API key using "AI: Configure API" command (Tools > Plugins > AI)
3. Start using AI features!

**Example API Services:**
- OpenAI (GPT-3.5, GPT-4)
- Anthropic (Claude)
- Local LLM servers (Ollama, LM Studio)
- Any OpenAI-compatible API

---

## AI Integration Guide

### How AI Plugins Work

AI plugins use Java's `HttpClient` to make HTTP requests to AI APIs. The system supports:

✅ **HTTP/HTTPS requests** - Full network access
✅ **JSON parsing** - Process API responses
✅ **Async operations** - Non-blocking UI
✅ **Error handling** - Graceful failure handling
✅ **Configurable endpoints** - Support multiple AI providers

### Creating Your Own AI Plugin

```java
// Example: Simple AI call
private String callAI(String prompt) throws IOException, InterruptedException {
    HttpClient client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build();
    
    String jsonBody = String.format(
        "{\"model\":\"gpt-3.5-turbo\",\"messages\":[{\"role\":\"user\",\"content\":\"%s\"}]}",
        escapeJson(prompt)
    );
    
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("https://api.openai.com/v1/chat/completions"))
        .header("Content-Type", "application/json")
        .header("Authorization", "Bearer " + apiKey)
        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
        .build();
    
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    // Parse and return response
}
```

### AI Plugin Ideas

| Plugin Type | Description | Complexity |
|-------------|-------------|------------|
| **Summarizer** | Auto-summarize long notes | ⭐ Easy |
| **Translator** | Multi-language translation | ⭐ Easy |
| **Grammar Checker** | Fix grammar and spelling | ⭐⭐ Medium |
| **Content Generator** | Generate blog posts, articles | ⭐⭐ Medium |
| **Tag Suggester** | AI-suggested tags based on content | ⭐⭐ Medium |
| **Question Answering** | Answer questions about notes | ⭐⭐⭐ Hard |
| **Code Assistant** | Explain/improve code snippets | ⭐⭐⭐ Hard |
| **Sentiment Analysis** | Analyze note sentiment | ⭐⭐ Medium |

---

## Creating a Custom Plugin

### Step 1: Create the Plugin Class

```java
package com.example.forevernote.plugin.builtin;

import com.example.forevernote.plugin.Plugin;
import com.example.forevernote.plugin.PluginContext;

public class MyPlugin implements Plugin {
    
    private static final String ID = "my-plugin";
    private static final String NAME = "My Plugin";
    private static final String VERSION = "1.0.0";
    
    private PluginContext context;
    
    @Override
    public String getId() { return ID; }
    
    @Override
    public String getName() { return NAME; }
    
    @Override
    public String getVersion() { return VERSION; }
    
    @Override
    public String getDescription() {
        return "A custom plugin that does something useful";
    }
    
    @Override
    public String getAuthor() {
        return "Your Name";
    }
    
    @Override
    public void initialize(PluginContext context) {
        this.context = context;
        
        // Register commands
        context.registerCommand(
            "My Plugin: Do Something",
            "Description of the action",
            "Ctrl+Shift+M",  // Optional shortcut
            this::doSomething
        );
        
        // Subscribe to events
        context.subscribe(NoteEvents.NoteSelectedEvent.class, this::onNoteSelected);
        
        context.log("My Plugin initialized");
    }
    
    @Override
    public void shutdown() {
        context.unregisterCommand("My Plugin: Do Something");
        context.log("My Plugin shutdown");
    }
    
    private void doSomething() {
        // Your plugin logic here
        List<Note> notes = context.getNoteService().getAllNotes();
        context.showInfo("My Plugin", "Result", "Found " + notes.size() + " notes");
    }
    
    private void onNoteSelected(NoteEvents.NoteSelectedEvent event) {
        Note note = event.getNote();
        context.log("Note selected: " + note.getTitle());
    }
}
```

### Step 2: Register the Plugin

Add to `MainController.registerBuiltInPlugins()`:

```java
private void registerBuiltInPlugins() {
    // Existing plugins
    pluginManager.registerPlugin(new WordCountPlugin());
    pluginManager.registerPlugin(new DailyNotesPlugin());
    // ... other plugins ...
    
    // Your new plugin
    pluginManager.registerPlugin(new MyPlugin());
}
```

### Step 3: Register Menu Items (Automatic)

Plugins register their menu items dynamically in the `initialize()` method:

```java
@Override
public void initialize(PluginContext context) {
    this.context = context;
    
    // Register command (appears in Command Palette)
    context.registerCommand("My Plugin: Action", "Description", this::myAction);
    
    // Register menu item (appears in Tools > Plugins > [Category])
    context.registerMenuItem("MyCategory", "My Action", this::myAction);
    context.registerMenuItem("MyCategory", "Another Action", "Ctrl+M", this::anotherAction);
}
```

**No FXML or MainController changes needed!** The menu is built dynamically.

## Available Events

### Note Events (`NoteEvents`)

| Event | Description | Data |
|-------|-------------|------|
| `NoteSelectedEvent` | Note selected in list | `Note note` |
| `NoteCreatedEvent` | New note created | `Note note` |
| `NoteSavedEvent` | Note saved | `Note note` |
| `NoteDeletedEvent` | Note deleted | `int noteId, String title` |
| `NoteFavoriteChangedEvent` | Favorite toggled | `Note note, boolean isFavorite` |
| `NoteContentChangedEvent` | Content modified | `Note note, String content` |
| `NotesRefreshRequestedEvent` | Refresh requested | - |
| `NoteOpenRequestEvent` | Plugin requests note open | `Note note` |

### Folder Events (`FolderEvents`)

| Event | Description | Data |
|-------|-------------|------|
| `FolderSelectedEvent` | Folder selected | `Folder folder` |
| `FolderCreatedEvent` | Folder created | `Folder folder` |
| `FolderDeletedEvent` | Folder deleted | `Folder folder` |

### Tag Events (`TagEvents`)

| Event | Description | Data |
|-------|-------------|------|
| `TagCreatedEvent` | Tag created | `Tag tag` |
| `TagDeletedEvent` | Tag deleted | `Tag tag` |
| `TagAssignedEvent` | Tag assigned to note | `Note note, Tag tag` |
| `TagRemovedEvent` | Tag removed from note | `Note note, Tag tag` |

## Plugin States

```
REGISTERED → INITIALIZED → ENABLED ⟷ DISABLED
                ↓
              ERROR
```

1. **REGISTERED** - Plugin registered but not initialized
2. **INITIALIZED** - `initialize()` called successfully
3. **ENABLED** - Plugin active and responding
4. **DISABLED** - Temporarily disabled (can be re-enabled)
5. **ERROR** - Plugin encountered an error

## Plugin Capabilities Summary

| Capability | Available | Notes |
|------------|-----------|-------|
| Register commands | ✅ | Command Palette integration |
| Access notes | ✅ | Full CRUD via NoteService |
| Access folders | ✅ | Full CRUD via FolderService |
| Access tags | ✅ | Full CRUD via TagService |
| Subscribe to events | ✅ | All app events available |
| Publish events | ✅ | Custom events supported |
| Show dialogs | ✅ | Info, Error, Custom dialogs |
| Open notes in editor | ✅ | Via `requestOpenNote()` |
| Refresh UI | ✅ | Via `requestRefreshNotes()` |
| File system access | ✅ | Standard Java I/O |
| Network access | ✅ | Standard Java networking |
| Custom UI panels | ⚠️ | Via JavaFX dialogs only |
| Modify main UI | ⚠️ | Side panels only via `registerSidePanel()` |
| Add menu items | ✅ | Via `registerMenuItem()` |
| Add side panels | ✅ | Via `registerSidePanel()` (Obsidian-style) |
| **AI Integration** | ✅ | HTTP requests to AI APIs |
| **External APIs** | ✅ | Full network access |
| **JSON Processing** | ✅ | Standard Java libraries |

## Limitations

1. **No Runtime Plugin Loading**: Plugins must be compiled with the app
2. **No Sandboxing**: Plugins have full Java access
3. **UI Customization**: Supports side panels and menu items; can't modify toolbar or main editing area
4. **No Plugin Repository**: Manual installation required

## Best Practices

1. **Unique IDs**: Use lowercase with hyphens (`my-plugin`)
2. **Prefix Commands**: Use plugin name prefix (`MyPlugin: Action`)
3. **Clean Shutdown**: Unregister all commands and subscriptions
4. **Error Handling**: Wrap operations in try-catch
5. **UI Thread**: Use `Platform.runLater()` for UI operations
6. **Logging**: Use `context.log()` for debugging
7. **Dependencies**: Declare if your plugin needs others

## Plugin Manager API

```java
// Registration
boolean registerPlugin(Plugin plugin);
boolean unregisterPlugin(String pluginId);

// Lifecycle
boolean initializePlugin(String pluginId);
void initializeAll();
void shutdownPlugin(String pluginId);
void shutdownAll();

// State management
boolean enablePlugin(String pluginId);
boolean disablePlugin(String pluginId);
boolean isPluginEnabled(String pluginId);
PluginState getPluginState(String pluginId);

// Queries
Optional<Plugin> getPlugin(String pluginId);
List<Plugin> getAllPlugins();
List<Plugin> getEnabledPlugins();
List<Plugin> getDisabledPlugins();
int getPluginCount();
String getPluginInfo(String pluginId);
```

---

## Summary

The Forevernote plugin system provides a powerful and flexible way to extend the application. With access to services, events, and the Command Palette, plugins can add significant functionality while maintaining clean separation from the core application.

**What you CAN do with plugins:**
- Process and analyze note content
- Create notes from templates
- Export/backup notes
- Integrate with external services
- Add custom commands
- Respond to user actions

**What you CANNOT do (currently):**
- Modify the main toolbar (buttons, etc.)
- Replace or modify the editor/preview components
- Create background services that run independently

**Note**: External plugins can now be loaded dynamically from the `plugins/` directory without recompiling the application!

---

## External Plugins (All Plugins)

Forevernote has a **completely decoupled plugin system**. The core application has **zero knowledge** of any specific plugins. All plugins (including built-in ones) must be placed in the `plugins/` directory as JAR files.

### Architecture Philosophy

- **Core Isolation**: The main application code (`MainController`, `Main.java`) does NOT reference any specific plugin classes
- **Dynamic Loading**: All plugins are loaded at runtime from the `plugins/` directory
- **No Hardcoding**: No plugin names, IDs, or classes are hardcoded in the core
- **Community-Driven**: Anyone can create plugins without modifying the core application

### How Plugins Work

1. **Plugin Directory**: Place your plugin JAR file in the `plugins/` directory (created automatically on first run)
2. **Auto-Discovery**: The application automatically scans the `plugins/` directory on startup
3. **Dynamic Loading**: Plugins are loaded using Java's `URLClassLoader`
4. **No Recompilation**: Add or remove plugins by simply placing/deleting JAR files
5. **Built-in Plugins**: Even "built-in" plugins (Word Count, Daily Notes, etc.) must be packaged as JARs and placed in `plugins/`

### Creating an External Plugin

#### Step 1: Create Your Plugin Class

Create a Java class that implements the `Plugin` interface:

```java
package com.example.myplugin;

import com.example.forevernote.plugin.Plugin;
import com.example.forevernote.plugin.PluginContext;

public class MyCustomPlugin implements Plugin {
    
    private PluginContext context;
    
    @Override
    public String getId() {
        return "my-custom-plugin";
    }
    
    @Override
    public String getName() {
        return "My Custom Plugin";
    }
    
    @Override
    public String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public String getDescription() {
        return "A custom plugin example";
    }
    
    @Override
    public String getAuthor() {
        return "Your Name";
    }
    
    @Override
    public void initialize(PluginContext context) {
        this.context = context;
        
        // Register commands
        context.registerCommand(
            "MyPlugin: Do Something",
            "Does something cool",
            "Ctrl+Shift+M",
            this::doSomething
        );
        
        context.log("My Custom Plugin initialized!");
    }
    
    private void doSomething() {
        context.showInfo("My Plugin", null, "Hello from external plugin!");
    }
    
    @Override
    public void shutdown() {
        context.log("My Custom Plugin shutting down");
    }
}
```

#### Step 2: Package as JAR

Create a JAR file containing your plugin class. You have two options:

**Option A: Specify Plugin Class in Manifest (Recommended)**

Create a `META-INF/MANIFEST.MF` file:

```
Manifest-Version: 1.0
Plugin-Class: com.example.myplugin.MyCustomPlugin
```

Then build your JAR:
```bash
javac -cp "forevernote-core.jar" MyCustomPlugin.java
jar cfm my-custom-plugin.jar META-INF/MANIFEST.MF MyCustomPlugin.class
```

**Option B: Auto-Detection**

If you don't specify `Plugin-Class` in the manifest, the loader will automatically scan your JAR for classes implementing `Plugin`. However, this is slower and less reliable.

#### Step 3: Include Dependencies

If your plugin uses external libraries, you have two options:

1. **Fat JAR (Uber JAR)**: Include all dependencies in your plugin JAR
2. **Shared Dependencies**: Place common JARs in a `plugins/lib/` directory (future feature)

**Important**: Your plugin JAR must include:
- Your plugin class
- The `Plugin` interface (or reference to Forevernote's core JAR)
- Any dependencies your plugin needs

#### Step 4: Deploy Your Plugin

1. Copy your JAR file to the `plugins/` directory:
   ```
   Forevernote/
   └── plugins/
       └── my-custom-plugin.jar
   ```

2. Restart Forevernote (or reload plugins if supported)

3. Your plugin will appear in the Plugin Manager automatically!

### Plugin Directory Location

The `plugins/` directory is created in:
- **Relative to application**: `plugins/` (same directory as the JAR)
- **Or**: Set `forevernote.data.dir` system property to specify a custom data directory

### Plugin Loading Process

1. **Scan**: Application scans `plugins/` directory for `.jar` files
2. **Load**: Each JAR is loaded using `URLClassLoader`
3. **Detect**: Plugin class is detected from manifest or auto-scanned
4. **Validate**: Class must implement `Plugin` interface
5. **Register**: Plugin is registered with `PluginManager`
6. **Initialize**: Plugin is initialized with `PluginContext`

### Plugin Requirements

Your external plugin must:

✅ Implement the `Plugin` interface  
✅ Have a unique plugin ID  
✅ Be packaged as a JAR file  
✅ Include all dependencies (fat JAR)  
✅ Handle errors gracefully  

### Example: Minimal External Plugin

Here's a complete example of a minimal external plugin:

```java
package com.example.minimal;

import com.example.forevernote.plugin.Plugin;
import com.example.forevernote.plugin.PluginContext;

public class MinimalPlugin implements Plugin {
    private PluginContext context;
    
    @Override
    public String getId() { return "minimal"; }
    
    @Override
    public String getName() { return "Minimal Plugin"; }
    
    @Override
    public String getVersion() { return "1.0.0"; }
    
    @Override
    public void initialize(PluginContext context) {
        this.context = context;
        context.registerCommand("Minimal: Hello", "Says hello", null, 
            () -> context.showInfo("Hello", null, "Hello from minimal plugin!"));
    }
    
    @Override
    public void shutdown() {}
}
```

### Troubleshooting External Plugins

**Plugin not appearing:**
- Check that the JAR file is in the `plugins/` directory
- Verify the JAR contains your plugin class
- Check application logs for loading errors
- Ensure `Plugin-Class` is set in manifest (or class is auto-detectable)

**ClassNotFoundException / NoClassDefFoundError:**
- Your plugin JAR is missing dependencies
- Create a fat JAR with all dependencies included
- **Inner classes**: Ensure your JAR includes inner classes (e.g., `MyPlugin$InnerClass.class`)
  - The build script automatically includes inner classes
  - If using custom build, ensure all `.class` files are included in the JAR
  - Verify with: `jar tf your-plugin.jar | grep '\$'`

**Plugin fails to initialize:**
- Check plugin logs: `[Plugin:your-plugin-id] ...`
- Ensure `initialize()` method doesn't throw exceptions
- Verify all required services are available
- **Inner class errors**: If you see `NoClassDefFoundError` for inner classes, the JAR may be missing them

**Commands not appearing:**
- Ensure plugin is enabled in Plugin Manager
- Check that commands are registered in `initialize()`
- Verify command names don't conflict with existing commands

### Sharing Your Plugin

To share your plugin with the community:

1. **Documentation**: Create a README explaining what your plugin does
2. **Versioning**: Use semantic versioning (e.g., `1.0.0`)
3. **License**: Include a license file
4. **Testing**: Test on multiple platforms if possible
5. **Distribution**: Share via GitHub, GitLab, or other platforms

### Plugin Best Practices (External)

- ✅ Use unique plugin IDs (e.g., `github-username-plugin-name`)
- ✅ Include comprehensive error handling
- ✅ Log important operations using `context.log()`
- ✅ Unregister commands in `shutdown()`
- ✅ Use descriptive command names with plugin prefix
- ✅ Document dependencies and requirements
- ✅ Test your plugin before distributing
- ✅ Follow semantic versioning
- ❌ Don't modify core application files
- ❌ Don't use internal/private APIs
- ❌ Don't block the UI thread

---

For questions or issues, please refer to the main documentation or create an issue in the repository.
