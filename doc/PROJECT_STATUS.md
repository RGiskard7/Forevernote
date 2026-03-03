# Project Status (v1.0.0)

## Overall

- Release state: Stable candidate
- Version: 1.0.0
- Last update: 2026-03-03

## What is Stable

- Core note/folder/tag workflows.
- SQLite/FileSystem backend operation.
- Plugin loading and management.
- External theme loading and fallback.
- Autosave and command routing.
- Trash/restore (including nested folder scenarios).

## Active Technical Debt

- Some UI components still rely on runtime inline styles (`setStyle`) and should be migrated to CSS classes.
- Built-in theme CSS files contain historical duplication that can be consolidated.
- MainController has improved modularization but remains large compared to target end-state.

## Quality Gate Snapshot

- Maven tests: passing
- Build scripts: operational
- Hardening scripts: available
- Documentation: normalized for v1.0.0
