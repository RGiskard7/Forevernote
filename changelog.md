# Forevernote Changelog

All notable changes to this project will be documented in this file.

---

## [4.6.0] - 2026-01-19

### UI Modification Support (Obsidian-style)

**NEW:** Plugins can now add custom UI panels to the sidebar, similar to Obsidian.

**Added:**
- `SidePanelRegistry` interface for dynamic UI panel registration
- Plugins can register collapsible panels in the right sidebar
- New **CalendarPlugin** demonstrating UI modification capabilities:
  - Mini calendar widget in sidebar
  - Highlights days with daily notes (green)
  - Click date to open/create daily note
  - Month navigation (prev/next/today)
  - Integration with Daily Notes plugin

**New methods in PluginContext:**
- `registerSidePanel(panelId, title, content)` - Add UI panel
- `registerSidePanel(panelId, title, content, icon)` - Add UI panel with icon
- `removeSidePanel(panelId)` - Remove UI panel
- `getSidePanelRegistry()` - Get registry for advanced operations

**Files Added:**
- `SidePanelRegistry.java` - Interface for side panel registration
- `CalendarPlugin.java` - New plugin with sidebar calendar widget

**Files Modified:**
- `PluginContext.java` - Added side panel methods
- `PluginManager.java` - Now receives `SidePanelRegistry`
- `MainController.java` - Implements `SidePanelRegistry`
- `MainView.fxml` - Added `pluginPanelsContainer` in right panel

**Plugin Count:** 8 plugins (7 original + CalendarPlugin)

---

## [4.5.0] - 2026-01-18

### Core Decoupling - Dynamic Plugin Menus

**BREAKING CHANGE:** Plugin menu items are now registered dynamically by plugins.

**Added:**
- `PluginMenuRegistry` interface for dynamic menu registration
- Plugins can now register menu items in categorized submenus
- Support for keyboard shortcuts in plugin menus
- Menu separators support for plugin menus
- Automatic cleanup of plugin menus when plugins are disabled

**Removed:**
- All hardcoded plugin handlers from `MainController`:
  - `handlePluginWordCount()`
  - `handlePluginDailyNotes()`
  - `handlePluginReadingTime()`
  - `handlePluginTemplates()`
  - `handlePluginTOC()`
  - `handlePluginBackup()`
  - `handlePluginAI*()` methods
- Hardcoded plugin menu items from `MainView.fxml`

**Modified files:**
- `PluginMenuRegistry.java` (NEW) - Interface for plugin menu registration
- `PluginContext.java` - Added `registerMenuItem()`, `addMenuSeparator()` methods
- `PluginManager.java` - Now receives `PluginMenuRegistry` in constructor
- `MainController.java` - Implements `PluginMenuRegistry`, removed all hardcoded plugin handlers
- `MainView.fxml` - Simplified plugins menu (dynamic content only)
- All plugins updated to register their own menu items

**How it works:**
1. Plugins call `context.registerMenuItem("Category", "Item Name", action)` in `initialize()`
2. MainController (as PluginMenuRegistry) creates category submenus dynamically
3. Menu items are automatically removed when plugins are disabled

---

## [4.4.1] - 2026-01-18

### Plugin Build Fix - Inner Classes & ClassLoader

**Fixed:**
- **Plugin JARs now include inner classes** - Fixed `NoClassDefFoundError` for inner classes like `TemplatesPlugin$Template`
  - Script now correctly includes all `.class` files from plugin compilation
  - JARs now contain only plugin classes (not dependency classes)
  - Removed source `.java` files from JARs (only `.class` files included)
  - Improved JAR verification to check for main class and inner classes
- **ClassLoader lifecycle fix** - Fixed issue where `URLClassLoader` was closed too early
  - Classloaders are now kept open while plugins are active
  - Inner classes remain accessible during plugin initialization and execution
  - Added `closeAllClassLoaders()` method for proper cleanup on shutdown

**Modified files:**
- `scripts/build-plugins.ps1`:
  - Fixed JAR creation to include inner classes (e.g., `TemplatesPlugin$Template.class`)
  - Changed from `-C $TempDir .` to `-C $TempDir com` to include only plugin classes
  - Added removal of `.java` source files before JAR creation
  - Improved verification to check only plugin classes in JAR
  - Better error messages and inner class detection
- `PluginLoader.java`:
  - **CRITICAL FIX**: Keep `URLClassLoader` instances open while plugins are active
  - Added `activeClassLoaders` list to track all classloaders
  - Removed premature `classLoader.close()` calls that prevented inner class access
  - Added `closeAllClassLoaders()` method for proper cleanup on application shutdown
  - Inner classes now accessible during plugin initialization and execution

**Verified plugins:**
- ✅ `TemplatesPlugin` - Now includes `TemplatesPlugin$Template.class`
- ✅ `TableOfContentsPlugin` - Now includes `TableOfContentsPlugin$TocEntry.class`
- ✅ `WordCountPlugin` - Now includes `WordCountPlugin$Statistics.class`
- ✅ `AIPlugin` - Now includes `AIPlugin$ProviderInfo.class`
- ✅ All other plugins verified working

---

## [4.4.0] - 2026-01-18

### Complete Plugin Decoupling + AI Plugin Fixes

**BREAKING CHANGE**: The core application is now **completely decoupled** from all plugins. No plugins are hardcoded in the core.

**New features:**
- **External Plugin Loading** - Plugins can now be loaded dynamically from `plugins/` directory
  - No recompilation needed - just place JAR files in `plugins/` folder
  - Auto-discovery on application startup
  - Supports manifest-based and auto-detection plugin class discovery
  - Community can create and share plugins independently

**New files:**
- `PluginLoader.java` - Dynamic plugin loader that scans `plugins/` directory
  - Loads JAR files using URLClassLoader
  - Reads `Plugin-Class` from manifest or auto-detects plugin classes
  - Creates `plugins/` directory automatically if missing
  - Comprehensive error handling and logging

**Fixed:**
- **AI Plugin Commands** - Commands now work from menu and Command Palette
  - Added cases for all AI commands in `executeCommand()` method
  - Added `executePluginCommand()` helper method
  - Added `findCommand()` method to CommandPalette
  - Commands now fallback to CommandPalette lookup for plugin commands

**Modified files:**
- `MainController.java`:
  - **REMOVED** `registerBuiltInPlugins()` method - no plugins are hardcoded anymore
  - **REMOVED** all hardcoded plugin references (WordCountPlugin, DailyNotesPlugin, AIPlugin, etc.)
  - **REMOVED** hardcoded AI command cases from `executeCommand()` switch
  - **REMOVED** `executePluginCommand()` method (redundant)
  - Renamed `loadExternalPlugins()` to just load all plugins from `plugins/` directory
  - Core application now has **zero knowledge** of specific plugins
- `CommandPalette.java`:
  - Added `findCommand(String name)` method to search commands by name
- `AIPlugin.java`:
  - Improved error handling in `initialize()` method
  - Added try-catch for Preferences loading with fallback to defaults

**Documentation:**
- `doc/PLUGINS.md` - Added comprehensive "External Plugins" section
  - Step-by-step guide for creating external plugins
  - JAR packaging instructions
  - Manifest configuration
  - Troubleshooting guide
  - Best practices for community plugins
  - Example minimal plugin code

**How it works:**
1. Place plugin JAR files in `plugins/` directory (created automatically)
2. Plugin JAR must contain a class implementing `Plugin` interface
3. Optionally specify `Plugin-Class` in JAR manifest
4. Application automatically loads plugins on startup
5. Plugins appear in Plugin Manager and can be enabled/disabled
6. Remove plugins by deleting JAR files

**Plugin requirements:**
- Must implement `Plugin` interface
- Must be packaged as JAR file
- Should include all dependencies (fat JAR)
- Must have unique plugin ID

---

## [4.3.0] - 2026-01-18

### AI Assistant Plugin + Enhanced Configuration

**New plugin:**
- `AIPlugin.java` - AI-powered features: summarize, translate, improve writing, generate content
  - Supports multiple providers (OpenAI, Anthropic, Local LLMs)
  - Persistent configuration via Preferences API
  - Provider selector with auto-fill for endpoints and models
  - Full menu integration (Tools > Plugins > AI Assistant)

**Modified files:**
- `MainController.java` - Registered AI plugin, added 5 menu handlers for AI commands
- `MainView.fxml` - Added "AI Assistant" submenu with all commands
- `AIPlugin.java` - Enhanced with Preferences-based configuration, provider selector, improved error handling

**AI Plugin Features:**
- **Summarize Note** (Ctrl+Shift+S) - Generate AI summary
- **Translate Note** - Translate to multiple languages
- **Improve Writing** - Fix grammar and style
- **Generate Content** - Generate from prompts
- **Configure API** - Set provider, API key, endpoint, model

**Supported Providers:**
- OpenAI (GPT-3.5, GPT-4)
- Anthropic (Claude)
- Local LLMs (Ollama, LM Studio)
- Custom endpoints

---

## [4.2.0] - 2026-01-18

### New Useful Plugins + Improved Plugin System

**New plugin files:**
- `TemplatesPlugin.java` - Create notes from 7 built-in templates (Meeting Notes, Project Plan, Weekly Review, Checklist, Cornell Notes, Blog Post, Bug Report)
- `TableOfContentsPlugin.java` - Generate table of contents from Markdown headers
- `AutoBackupPlugin.java` - Backup notes to files and database

**Modified files:**
- `DailyNotesPlugin.java` - Now opens notes directly in editor after creating
- `PluginContext.java` - Added `requestOpenNote()`, `requestRefreshNotes()`, `showInfo()`, `showError()` methods
- `NoteEvents.java` - Added `NoteOpenRequestEvent` for plugin→UI communication
- `MainController.java` - Subscribes to plugin events, registers 6 plugins, added menu handlers
- `MainView.fxml` - Reorganized plugins menu into Core/Productivity/Utilities submenus

**Summary:**

#### New Plugins (7 total):

| Plugin | Description | Shortcut |
|--------|-------------|----------|
| **Word Count** | Statistics: words, chars, lines | - |
| **Reading Time** | Estimated reading time | - |
| **Daily Notes** | Create/open daily notes (opens in editor!) | Ctrl+Alt+D |
| **Templates** | Create notes from 7 templates | Ctrl+Shift+T |
| **Table of Contents** | Generate TOC from headers | Ctrl+Shift+O |
| **Auto Backup** | Export notes to files, backup DB | Ctrl+Shift+B |
| **AI Assistant** | Summarize, translate, improve, generate | Ctrl+Shift+S |

#### Plugin System Improvements:
- Plugins can now open notes directly in the editor via `context.requestOpenNote(note)`
- Plugins can request UI refresh via `context.requestRefreshNotes()`
- Helper methods: `context.showInfo()`, `context.showError()` for user feedback
- Event-based communication between plugins and MainController

#### Templates Available:
1. **Meeting Notes** - Agenda, discussion, action items
2. **Project Plan** - Goals, milestones, resources
3. **Weekly Review** - Accomplishments, challenges, next week
4. **Checklist** - Simple task list
5. **Cornell Notes** - Academic note-taking method
6. **Blog Post** - Draft structure with meta checklist
7. **Bug Report** - Steps to reproduce, environment, etc.

---

## [4.1.0] - 2026-01-18

### Menu Access for Tools & Plugins (Obsidian-style)

**Modified files:**
- `Forevernote/src/main/resources/com/example/forevernote/ui/view/MainView.fxml` - Added menu handlers
- `Forevernote/src/main/java/com/example/forevernote/ui/controller/MainController.java` - Added FXML handlers
- `Forevernote/src/main/java/com/example/forevernote/plugin/PluginManager.java` - Added `isPluginEnabled()` method
- `Forevernote/src/main/java/com/example/forevernote/ui/components/CommandPalette.java` - Fixed theme colors
- `Forevernote/src/main/java/com/example/forevernote/ui/components/QuickSwitcher.java` - Fixed theme colors
- `Forevernote/src/main/java/com/example/forevernote/ui/components/PluginManagerDialog.java` - Fixed unmodifiable list bug

**Summary:**

#### Full Menu Access (No Shortcuts Required)
All tools are now accessible via the Tools menu, not just keyboard shortcuts:

**Tools Menu:**
- `Command Palette...` (Ctrl+P) - Opens Command Palette
- `Quick Switcher...` (Ctrl+O) - Opens Quick Switcher
- `Global Search` (Ctrl+Shift+F) - Focus search field
- `Tags Manager...` - Opens tag management
- **Plugins submenu:**
  - `Manage Plugins...` - Opens Plugin Manager dialog
  - `Word Count` - Execute word count for current note
  - `Daily Notes` - Open today's daily note
  - `Reading Time` - Show reading time estimate

#### Bug Fixes:
- Fixed FXML menu items missing `onAction` handlers
- Added `isPluginEnabled()` method to PluginManager for checking plugin state
- Fixed `UnsupportedOperationException` when opening Plugin Manager (unmodifiable list sorting)
- Fixed Command Palette and Quick Switcher theme colors to match app's Catppuccin theme
- Added logging and error handling for uninitialized Command Palette

#### Theme Consistency:
Updated all modal dialogs (Command Palette, Quick Switcher, Plugin Manager) to use consistent colors:
- Dark theme: `#1e1e2e` (bg), `#313244` (secondary), `#cdd6f4` (fg), `#7c3aed` (accent)
- Light theme: `#ffffff` (bg), `#f5f5f5` (secondary), `#1e1e2e` (fg), `#7c3aed` (accent)

**Rationale:**
Users should be able to access all features through menus, not just keyboard shortcuts (which may conflict with other applications). This follows Obsidian's UX pattern.

---

## [4.0.0] - 2026-01-18

### Plugin System Implementation (Obsidian-style)

**Added files:**
- `Forevernote/src/main/java/com/example/forevernote/plugin/builtin/WordCountPlugin.java`
- `Forevernote/src/main/java/com/example/forevernote/plugin/builtin/DailyNotesPlugin.java`
- `Forevernote/src/main/java/com/example/forevernote/plugin/builtin/ReadingTimePlugin.java`
- `Forevernote/src/main/java/com/example/forevernote/plugin/builtin/package-info.java`
- `Forevernote/src/main/java/com/example/forevernote/ui/components/PluginManagerDialog.java` - Obsidian-style plugin manager UI
- `doc/PLUGINS.md` - Complete plugin system documentation

**Modified files:**
- `Forevernote/src/main/java/com/example/forevernote/ui/controller/MainController.java` - Plugin system integration

**Summary:**

#### Plugin System Integration
The existing plugin architecture (PluginManager, PluginContext, Plugin interface) has been fully integrated into the application. The system is now functional and ready for use.

#### Plugin Manager UI (Obsidian-style):
- Visual dialog to manage plugins (Ctrl+Shift+P or Command Palette → "Plugins: Manage Plugins")
- Toggle switches to enable/disable plugins
- Shows plugin info: name, version, author, description, status
- Professional styling matching the app theme (light/dark)

#### Built-in Plugins (3 included):

1. **Word Count Plugin** (`word-count`)
   - Commands: `Word Count: Current Note` (Ctrl+Shift+W), `Word Count: All Notes`
   - Statistics: words, characters, lines, paragraphs
   
2. **Daily Notes Plugin** (`daily-notes`)
   - Commands: `Daily Notes: Open Today` (Ctrl+Alt+D), `Open Yesterday`, `Open Tomorrow`, `This Week`
   - Creates dated notes in "Daily Notes" folder with template
   
3. **Reading Time Plugin** (`reading-time`)
   - Commands: `Reading Time: Current Note` (Ctrl+Shift+R), `All Notes`, `Quick Estimate`
   - Calculates slow, average, fast reading and speaking times

#### Technical Changes:
- Added Services (NoteService, FolderService, TagService) instantiation in MainController
- Added PluginManager initialization with CommandPalette integration
- Plugins register commands that appear in Command Palette (Ctrl+P)
- Plugins can subscribe to application events
- Full plugin lifecycle management (register, initialize, enable, disable, shutdown)
- PluginManagerDialog for visual plugin management

#### Documentation:
- Complete `doc/PLUGINS.md` with architecture, API reference, and examples
- Instructions for creating custom plugins

---

## [3.3.1] - 2026-01-14

### Project Status Analysis & Documentation

**Added files:**
- `doc/PROJECT_STATUS.md` - Comprehensive project status analysis

**Summary:**
- Complete analysis of project viability and current state
- Documentation of all implemented features (100% core features)
- Architecture review and code quality assessment
- Identification of pending features (low priority)
- Project metrics and recommendations

---

## [3.3.0] - 2026-01-14

### Professional UI Improvements & Icon System

**Changed files:**
- `Forevernote/src/main/resources/com/example/forevernote/ui/view/MainView.fxml` - Icon updates for buttons
- `Forevernote/src/main/resources/com/example/forevernote/ui/css/dark-theme.css` - Folder icons, ComboBox styles, button improvements
- `Forevernote/src/main/resources/com/example/forevernote/ui/css/modern-theme.css` - Folder icons, ComboBox styles, button improvements
- `Forevernote/src/main/java/com/example/forevernote/ui/controller/MainController.java` - Enhanced folder tree with visual icons
- `README.md` - Professional update with screenshots and improved documentation

**Summary:**

#### Visual Icon System:
- **Note buttons**: Now use notebook emoji (📓) instead of plus sign
- **Folder buttons**: Use folder emoji (📁) consistently
- **Folder tree**: Enhanced with color-coded folder icons:
  - `[=]` for "All Notes" (purple)
  - `[/]` for open folders (green)
  - `[+]` for closed folders (orange)
- **Note counters**: Styled as badges with rounded corners and background colors

#### UI Improvements:
- **ComboBox styling**: Fixed readability in light theme with proper text colors and backgrounds
- **Primary button**: "New Note" button now has distinctive purple styling
- **Folder count display**: Improved visibility with badge-style formatting `(count)`
- **Consistent sizing**: All buttons have proper min-height and alignment

#### Documentation:
- **README.md**: Completely professionalized without emojis
- **Screenshots**: Added four interface screenshots showing main interface, dark theme, light theme, and editor features
- **Structure**: Improved organization and clarity of all documentation

---

## [3.2.0] - 2026-01-14

### Improved Icons & Responsive Format Toolbar

**Changed files:**
- `Forevernote/src/main/resources/com/example/forevernote/ui/view/MainView.fxml` - Added text to all buttons, ScrollPane for format toolbar
- `Forevernote/src/main/resources/com/example/forevernote/ui/css/dark-theme.css` - Larger icons, scrollable toolbar styles
- `Forevernote/src/main/resources/com/example/forevernote/ui/css/modern-theme.css` - Larger icons, scrollable toolbar styles

**Summary:**

#### Larger, Consistent Icons:
- All icons now have a consistent font-size of 14-16px
- All buttons have a consistent min-height of 32px
- Icons are clearly visible and distinguishable

#### Scrollable Format Toolbar (Professional Standard Solution):
- The format toolbar is now wrapped in a ScrollPane with horizontal scrolling
- When the window is resized, the toolbar scrolls instead of icons disappearing
- This is the same approach used by Obsidian, VS Code, and other professional applications
- Scroll bar is minimal (6px) and appears only when needed

#### Button Labels (Obsidian-style):
- **Toolbar**: ＋ (new note), 📁 (folder), 💾 (save), 🗑 (delete)
- **Sidebar**: + Note, 📁 Folder, # Tag
- **View Modes**: ✎ (edit), ☰ (split), 👁 (preview)
- **Actions**: ☆/★ (favorite), ⓘ (info), ✕ (delete)
- **Format**: H1, H2, H3, B, I, S̶, U̲, ✱, 🔗, 🖼, ☑, •, 1., ❝, ⟨⟩

#### CSS Improvements:
- Added `.format-toolbar-container` for border and padding
- Added `.format-toolbar-scroll` for scrollable area styling
- Improved `.icon-btn` style for consistent appearance
- All buttons have proper alignment and sizing

---

## [3.1.0] - 2026-01-14

### Professional Button Labels & Icons (Simplified)

**Changed files:**
- `Forevernote/src/main/resources/com/example/forevernote/ui/view/MainView.fxml` - Button IDs for programmatic control
- `Forevernote/src/main/java/com/example/forevernote/ui/controller/MainController.java` - Simplified icon initialization
- `Forevernote/src/main/resources/com/example/forevernote/ui/css/dark-theme.css` - Improved button styles
- `Forevernote/src/main/resources/com/example/forevernote/ui/css/modern-theme.css` - Improved button styles

**Summary:**

#### Simple, Reliable Icons:
After testing Ikonli (which had compatibility issues with uber-jar packaging),
we opted for a simpler, more reliable approach using Unicode characters and text labels.

#### Button Labels:
- **Toolbar**: +, 📁, 💾, 🗑, ↻
- **Sidebar**: +, 📁, #
- **View Modes**: ✎, ▦, 👁
- **Actions**: ☆/★ (favorite toggle), ℹ, ✕
- **Format**: H1, H2, H3, B, I, S, U, ~, Lk, Img, [], •, 1., >, </>

#### Benefits of This Approach:
- Works reliably across all platforms
- No external dependencies
- Compact and professional appearance
- Tooltips provide full context
- Responsive by design (min-width ensures visibility)

#### Helper Methods Added:
- `setButtonText(Button, text, tooltip)` - Configure button with text and tooltip
- `setToggleText(ToggleButton, text, tooltip)` - Configure toggle button
- `updateFavoriteButtonIcon()` - Toggle ☆/★ based on favorite status

---

## [3.0.2] - 2026-01-13

### Obsidian-style Toolbar & Folder Note Count

**Changed files:**
- `Forevernote/src/main/resources/com/example/forevernote/ui/view/MainView.fxml`
- `Forevernote/src/main/resources/com/example/forevernote/ui/css/modern-theme.css`
- `Forevernote/src/main/resources/com/example/forevernote/ui/css/dark-theme.css`
- `Forevernote/src/main/java/com/example/forevernote/ui/controller/MainController.java`

**Summary:**

#### Toolbar Redesigned (Obsidian-style):
- **H2, H3, Hn** - Heading levels
- **B, I, S, U** - Bold, Italic, Strikethrough, Underline
- **~** - Highlight
- **@** - Insert link
- **[ ]** - Checkbox/Todo
- **:-** - Bullet list
- **1.** - Numbered list
- **>** - Blockquote
- **{}** - Code block

#### Folder Note Count (like Obsidian):
- Each folder now shows the number of notes it contains
- "All Notes" shows total count
- Count includes notes in subfolders recursively
- Styled with subtle gray color on the right side

#### New Methods Added:
- `handleHeading3()` - Insert H3 heading
- `handleRealUnderline()` - HTML underline
- `handleHighlight()` - Markdown highlight (==text==)
- `getNoteCountForFolder()` - Count notes recursively

#### CSS Improvements:
- Better format button styling with borders
- Folder name/count styles for TreeView
- Fixed dark theme consistency

---

## [3.0.1] - 2026-01-13

### Obsidian-style Format Icons & Tab Selection Fix

**Changed files:**
- `Forevernote/src/main/resources/com/example/forevernote/ui/view/MainView.fxml`
- `Forevernote/src/main/resources/com/example/forevernote/ui/css/modern-theme.css`
- `Forevernote/src/main/resources/com/example/forevernote/ui/css/dark-theme.css`
- `Forevernote/src/main/java/com/example/forevernote/ui/controller/MainController.java`

**Summary:**

#### Format Toolbar Improvements:
- **Bold (B)**: Styled with bold font weight
- **Italic (I)**: Styled with italic font style
- **Strikethrough (S)**: Styled with strikethrough
- **H1, H2**: New heading buttons with distinct sizes
- **Link (🔗)**: Unicode link icon
- **Image (🖼)**: Unicode image icon  
- **Todo (☐)**: Unicode checkbox icon
- **Bullet (•)**: Unicode bullet icon
- **List (1.)**: Numbered list indicator
- **Code (</>)**: Code block button
- **Quote (")**: Blockquote button

#### New Format Methods Added:
- `handleHeading1()` - Insert H1 heading
- `handleHeading2()` - Insert H2 heading
- `handleBulletList()` - Insert bullet list item
- `handleCode()` - Insert inline code or code block
- `handleQuote()` - Insert blockquote
- `insertLinePrefix()` - Helper for line-start insertions

#### Tab Selection Fix (Dark Theme):
- Selected tabs now show purple background (#9f7aea)
- Text turns white on selected tabs
- Hover states properly differentiated
- Removed white/gray selection issue

---

## [3.0.0] - 2026-01-13

### Major UI Overhaul - Professional Obsidian-style Interface

**Changed files:**
- `Forevernote/src/main/resources/com/example/forevernote/ui/view/MainView.fxml`
- `Forevernote/src/main/resources/com/example/forevernote/ui/css/modern-theme.css`
- `Forevernote/src/main/resources/com/example/forevernote/ui/css/dark-theme.css`

**Summary:**
Complete redesign of the user interface following Obsidian design principles:

#### UI Changes:
- **Removed Grid button** (non-functional, replaced with hidden compatibility toggles)
- **New button labels**: Clear, readable text labels instead of cryptic symbols
  - "Edit", "Split", "Read" for view modes
  - "Bold", "Italic", "Strike", "Link", "Image", "Todo", "List" for formatting
  - "Fav", "Info", "Del" for note actions
  - "Refresh" instead of "R"
- **Professional CSS classes**: Added semantic class names throughout FXML
- **Improved typography**: Consistent font sizes and weights
- **Better visual hierarchy**: Clear section delimitation with borders and backgrounds

#### Light Theme (modern-theme.css):
- Clean white/gray color palette (#fafafa, #f5f5f5, #f0f0f0)
- Purple accent color (#7c3aed) for selections and interactive elements
- Proper contrast for all text elements
- Subtle shadows and borders for depth
- Minimal, elegant scrollbars

#### Dark Theme (dark-theme.css):
- Professional dark palette (#1e1e1e, #252525, #2d2d2d)
- Purple accent (#9f7aea) matching light theme
- High contrast text (#e0e0e0, #d0d0d0)
- Proper border visibility (#3a3a3a, #404040)
- All UI elements clearly visible

#### Responsive Design:
- Flexible layouts using HBox.hgrow and VBox.vgrow
- Min/max widths for sidebars and panels
- Proper spacing and padding throughout

#### Bug Fixes:
- Fixed themes not applying correctly
- Fixed text visibility in all UI states
- Fixed button text contrast issues
- Removed non-functional Grid toggle button

---

## [2.5.0] - 2026-01-12

### Command Palette and Quick Switcher Implementation

**Added files:**
- `Forevernote/src/main/java/com/example/forevernote/ui/components/CommandPalette.java`
- `Forevernote/src/main/java/com/example/forevernote/ui/components/QuickSwitcher.java`

**Summary:**
Implemented Obsidian-style command palette (Ctrl+P) and quick switcher (Ctrl+O) for enhanced navigation and productivity.

---

## [2.4.0] - 2026-01-11

### Service Layer and Event Bus Architecture

**Added files:**
- `Forevernote/src/main/java/com/example/forevernote/services/NoteService.java`
- `Forevernote/src/main/java/com/example/forevernote/services/FolderService.java`
- `Forevernote/src/main/java/com/example/forevernote/services/TagService.java`
- `Forevernote/src/main/java/com/example/forevernote/events/EventBus.java`
- `Forevernote/src/main/java/com/example/forevernote/plugin/PluginManager.java`
- `Forevernote/src/main/java/com/example/forevernote/plugin/Plugin.java`

**Summary:**
Major architectural improvements:
- Created service layer for business logic separation
- Implemented event bus for decoupled component communication
- Added plugin system foundation for future extensibility

---

## [2.3.0] - 2026-01-08

### CSS Warnings Fix and Feature Verification

**Changed files:**
- `Forevernote/src/main/resources/com/example/forevernote/ui/css/modern-theme.css`
- `Forevernote/src/main/resources/com/example/forevernote/ui/css/dark-theme.css`

**Summary:**
- Fixed CSS ClassCastException warnings by replacing CSS variables with direct pixel values
- Verified all 20 core features are functional
- Deferred file attachments feature to future development

---

## [2.2.0] - 2026-01-07

### Professional UI and Bug Fixes

**Changed files:**
- `Forevernote/src/main/resources/com/example/forevernote/ui/css/modern-theme.css`
- `Forevernote/src/main/resources/com/example/forevernote/ui/css/dark-theme.css`
- `Forevernote/src/main/java/com/example/forevernote/ui/controller/MainController.java`

**Summary:**
- Complete CSS overhaul for professional look
- Fixed listener duplication bug in loadRecentNotes() and loadFavorites()
- Added null-safe comparisons in sortNotes()
- Implemented theme persistence using Preferences API
- Implemented import/export functionality
- Added About dialog

---

## [2.1.0] - 2026-01-06

### Info Panel and UI Improvements

**Changed files:**
- `Forevernote/src/main/resources/com/example/forevernote/ui/view/MainView.fxml`
- `Forevernote/src/main/java/com/example/forevernote/ui/controller/MainController.java`

**Summary:**
- Info panel now hidden by default
- Removed Attachments section (deferred feature)
- Improved Obsidian-style layout

---

## [2.0.0] - 2025-12-17

### Complete Project Fix and Feature Implementation

**Summary:**
Major overhaul of the entire project:
- Migrated to Maven standard directory structure
- Fixed all FXML bindings and compilation errors
- Implemented all missing UI features
- Fixed folder hierarchy display
- Fixed tag synchronization
- Fixed Markdown rendering with WebView
- Improved emoji rendering
- Made "All Notes" root visible
- Auto-refresh notes list on save/delete

---

## [1.0.0] - Initial Release

**Summary:**
Initial release of Forevernote desktop note-taking application.
- Basic note management (CRUD)
- Folder organization
- Tag system
- Markdown editor with preview
- SQLite database storage
- JavaFX desktop interface
