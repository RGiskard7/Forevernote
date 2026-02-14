# Implementation Plan - UI Refinement & Icon Standardization

## 1. Icon Standardization (Feather Icons)
- [x] **Replace SVG Paths with Ikonli**:
    - [x] Add `ikonli-javafx` and `ikonli-feather-pack` dependencies to `pom.xml`.
    - [x] Update `MainView.fxml` to use `kordamp:FontIcon` instead of `SVGPath` or `Button` graphics.
    - [x] Ensure all icons (Toolbar, Sidebar, Context Menus) use uniform `feather-[icon-name]` codes.
- [x] **Fix Icon Styling**:
    - [x] Update `modern-theme.css` to style `.feather-icon`.
        - [x] Set `-fx-icon-color` and `-fx-fill` to current text color.
        - [x] Ensure hover states change icon color appropriately.
    - [x] Update `dark-theme.css` to include the same `.feather-icon` styles.
- [x] **Clean Up**:
    - [x] Remove unused SVG helper methods in `MainController`.
    - [x] Remove duplicate button definitions in `MainController`.

## 2. Layout Refinement (Stacked vs Column)
- [x] **Implement Stacked Layout Logic**:
    - [x] Update `handleViewLayoutSwitch` in `MainController`.
    - [x] Ensure toggling logic (Sidebar/Notes Panel) works correctly in both modes.
        - [x] **Stacked Mode**: "Toggle Sidebar" hides the entire left stack (Sidebar + Notes). "Toggle Notes" hides just the notes list within the stack.
        - [x] **Column Mode**: Standard behavior (independent toggles).
- [x] **Fix Visibility Glitches**:
    - [x] Ensure Notes Panel reappears when a folder is selected if it was hidden.
    - [x] Fix split pane divider positions reset logic.

## 3. Build & Packaging
- [x] **Fix Uber JAR Creation**:
    - [x] Replace `maven-assembly-plugin` with `maven-shade-plugin`.
    - [x] Add `ServicesResourceTransformer` to `maven-shade-plugin` configuration (Critical for Ikonli ServiceLoader).
    - [x] Add `Launcher` class to avoid "JavaFX runtime components are missing" error.
    - [x] Update `pom.xml` to use `Launcher` as Main-Class.
- [x] **Verify Build**:
    - [x] Run `mvn clean package -DskipTests`. (Success)
    - [x] Run `java -jar target/forevernote-1.0.0-uber.jar`. (Success - App launches)

## 4. Verification Steps (User)
1. **Launch App**: Run `java -jar target/forevernote-1.0.0-uber.jar`.
2. **Check Icons**:
   - Verify all buttons have visible Feather icons.
   - Switch to **Dark Theme** (View -> Dark Theme) and verify icons remain visible and white/gray.
3. **Check Layout**:
   - Click "Layout Switch" button (columns icon) to toggle Stacked Mode.
   - In Stacked Mode, click "Toggle Sidebar" and "Toggle Notes" to ensure they collapse correctly.
   - Click a folder in the sidebar to ensure the notes list appears.
