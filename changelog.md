# Forevernote Changelog

All notable changes to this project will be documented in this file.

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
