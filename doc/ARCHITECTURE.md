# Architecture Overview

## System Style

- Desktop monolith (JavaFX).
- Local-first data model.
- Event-driven UI orchestration through EventBus.
- Storage abstraction with backend parity target.

## Layers

1. UI Layer (`ui/`)
- JavaFX controllers, FXML views, workflows, reusable components.
- `MainController` acts as composition root and delegates domain workflows.

2. Application/Domain Services (`service/`)
- `NoteService`, `FolderService`, `TagService`.
- Encapsulate business rules and storage operations.

3. Persistence (`data/`)
- DAO interfaces + SQLite and FileSystem implementations.
- Database bootstrap/migrations in `SQLiteDB`.

4. Eventing (`event/`)
- `EventBus` + typed events for note/folder/ui/system actions.
- Explicit subscription lifecycle and no-op safe subscription patterns.

5. Extensibility (`plugin/`)
- External plugin loading from `plugins/`.
- Plugin lifecycle: load -> init -> enable/disable -> shutdown.

6. Theming
- Built-in CSS themes and external theme catalog loading.

## Key Runtime Flows

- Note editing: UI input -> modified events -> autosave debounce -> service update -> list/tree refresh events.
- Folder operations: command/context menu/drag-drop -> folder service -> backend persistence -> UI reload.
- Plugin load: loader scans `plugins/`, validates descriptors, registers capabilities, exposes commands/UI.

## Quality Constraints

- Keep storage contracts stable across SQLite/FileSystem.
- Avoid UI blocking operations on heavy data loads.
- Keep EventBus publications deterministic and loop-safe.
- Prefer CSS classes over runtime inline style for maintainable theming.
