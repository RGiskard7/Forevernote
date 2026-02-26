# Hardening Baseline

## Scope
- Project: Forevernote
- Objective: provide a reproducible baseline and strict per-phase quality gates.
- Compatibility mode: strict (SQLite, FileSystem, existing plugins).

## Current Technical Baseline
- Java: 17
- JavaFX: 21
- Build tool: Maven 3.x
- Main module path: `Forevernote/pom.xml`
- Test command: `mvn -f Forevernote/pom.xml clean test`

## Validation Matrix
- Storage mode:
  - SQLite (`storage_type=sqlite`)
  - FileSystem (`storage_type=filesystem`)
- Plugin mode:
  - Core only
  - Core + external plugins in `plugins/`
- Theme mode:
  - Light
  - Dark
  - System

## Mandatory Phase Gate
1. `mvn -f Forevernote/pom.xml clean test` passes.
2. Smoke checklist completed (manual):
   - Create/edit/save note.
   - Move note to trash and restore.
   - Create folder and subfolder.
   - Add/remove tags in note.
   - Switch themes.
   - Open plugin manager.
3. No critical runtime errors in logs.
4. No visible UI regressions in main views.

## Rollback Policy
- Work in small commits by phase.
- If gate fails:
  - Revert phase commit(s) only.
  - Keep previous phase as release candidate baseline.

## Notes
- This file is the baseline reference for hardening execution.
- Update this file at the end of every completed phase.

## Execution status
- Phase 0: completed
- Phase 1: completed
- Phase 2: completed
- Phase 3: in progress
- Phase 4: in progress (initial NoteWorkflow extraction)
- Phase 5: in progress
- Phase 6+: pending
