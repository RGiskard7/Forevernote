# Hardening Baseline

- Baseline version: 1.0.0
- Baseline date: 2026-03-03

## Baseline Gate

1. `mvn -f Forevernote/pom.xml test` passes.
2. Smoke flow works in SQLite and FileSystem.
3. Plugin manager opens and external plugins are detected.
4. Theme switching works for built-in and external themes.
5. No critical regressions in save/delete/restore workflows.
