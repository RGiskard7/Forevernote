# Hardening Phase Checklist

Use this checklist before closing any hardening phase.

1. Tests pass (`mvn -f Forevernote/pom.xml test`).
2. Smoke manual executed:
   - note create/edit/save
   - folder create/subfolder/delete/restore
   - tag assignment and deletion
   - command palette and quick switcher
3. Verify SQLite and FileSystem parity.
4. Validate plugin and theme loading.
5. Update changelog and relevant docs.
