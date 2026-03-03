# Changelog

All notable changes to this project are documented in this file.

## [1.0.0] - 2026-03-03

### Added

- External theme loading and runtime theme catalog.
- `scripts/build-themes.sh` and `scripts/build-themes.ps1`.
- Retro phosphor external theme sample (`themes/retro-phosphor`).
- Sidebar tabs presentation preferences (text/icons).
- Editor mode button presentation (text/icons/auto).
- Auto-save (idle-based) with user preferences.
- Expanded command routing/workflow modularization.

### Changed

- Main UI behavior hardening and command routing consistency.
- Trash/restore behavior stabilized for folders and nested notes.
- Notes/folders drag-and-drop behavior hardened.
- Plugin lifecycle handling and loader behavior improved.
- Documentation fully normalized for v1.0.0.
- Notes list row density reduced for cleaner layout.

### Fixed

- Event loop and save path regressions in command/event flows.
- Null-handling issues in note loading and metadata updates.
- Sidebar root i18n label (`app.all_notes`) regression.
- Command panel visibility regression.
- Retro theme CSS lookup recursion crash.
- Inconsistent icon/color rendering in retro theme.

### Notes

- JavaFX parent-POM warnings can appear during Maven dependency resolution and are non-blocking.
