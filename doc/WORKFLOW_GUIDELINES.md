# Workflow Guidelines

## UI Workflows

- Keep controller methods thin; move orchestration into `ui/workflow/*`.
- Prefer additive refactors over broad rewrites.
- Keep behavior stable; avoid contract changes unless explicitly planned.

## Data Workflows

- Route persistence via services and DAO interfaces.
- Keep SQLite and FileSystem semantics aligned.
- Test edge cases for trash/restore and move operations.

## Theming Workflows

- Prefer CSS classes over inline Java styles.
- Use theme tokens/variables for color consistency.
- Provide safe fallback for invalid external theme descriptors.

## Plugin Workflows

- Treat plugins as external modules.
- Handle load/enable failures gracefully.
- Ensure lifecycle shutdown closes classloaders.
