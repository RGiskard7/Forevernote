# UI Modernization & Layout Refinement Plan

## Objective
Modernize the Forevernote UI to resemble Obsidian, including vector icons (Feather), a cleaner toolbar, and flexible layout options (Sidebar/Notes vertical stack).

## Changes Implemented

### 1. Vector Icons (SVG)
- **Replaced Emojis:** All toolbar and sidebar buttons now use SVG paths mimicking Feather Icons (e.g., File Plus, Folder Plus, Hash, Trash, Save).
- **Technology:** Used JavaFX `SVGPath` directly in FXML for sharp, scalable icons without external dependencies.
- **Styling:** Added `.feather-icon` class to `modern-theme.css` to handle stroke/fill and hover states correctly.

### 2. Flexible Layout (Obsidian-style)
- **Problem:** User wanted to switch between "3 Columns" (standard) and "2 Columns" (Sidebar & Notes stacked on the left, Editor on the right).
- **Solution:** 
    - Implemented `handleViewLayoutSwitch` in `MainController`.
    - Added a toggle button in the toolbar to switch layouts instantly.
    - Dynamically reparents `sidebarPane` and `notesPanel` into a new vertical `navSplitPane` when in "Stacked Mode".

### 3. Toolbar & Sidebar Cleanup
- **Toolbar:** Adjusted to include new "Layout Switch", "Sidebar Toggle", and "Notes Toggle" buttons. Moved "New Tag" to the toolbar.
- **Sidebar:** Removed duplicate "New Note", "New Folder", "New Tag" buttons from the bottom of the sidebar to reduce clutter.
- **Functionality:** 
    - `Sidebar Toggle` (Left Icon): Collapses sidebar.
    - `Notes Toggle` (List Icon): Collapses notes list.
    - `New Tag` (Hash Icon): Creates a new tag.

### 4. Controller Refactoring
- **Cleaned Fields:** Removed unused `sidebarNew*` buttons and duplicate field declarations.
- **Logic:** Updated `handleToggleSidebar` and `handleToggleNotesPanel` to sync with the new toggle buttons in the toolbar.

## files Modified
- `src/main/resources/com/example/forevernote/ui/view/MainView.fxml`: Complete rewrite of Toolbar and Sidebar structure; conversion to SVG icons.
- `src/main/java/com/example/forevernote/ui/controller/MainController.java`: Added layout logic, cleaned up fields, updated handlers.
- `src/main/resources/com/example/forevernote/ui/css/modern-theme.css`: Added SVG icon styling.

## Verification
1. **Icons:** Check that all buttons (New Note, Save, etc.) show crisp vector icons instead of text/emojis.
2. **Layout Switch:** Click the "Columns" icon in the toolbar. The layout should switch between:
   - **Default:** Sidebar | Notes | Editor
   - **Stacked:** [Sidebar / Notes] | Editor
3. **Collapsing:** Click the Sidebar (Left) or Notes (List) icons to collapse/expand those sections.
4. **New Tag:** Click the Hash icon to create a tag.
