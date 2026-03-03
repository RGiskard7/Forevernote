# Release and Rollback Playbook

## Release Steps

1. Run tests:

```bash
mvn -f Forevernote/pom.xml test
```

2. Run smoke/hardening checks.
3. Build plugins and themes.
4. Build production artifact.
5. Validate startup with launcher scripts.

## Rollback Triggers

- Data integrity issue in note/folder operations.
- Save/restore regressions.
- Plugin/theme loading breakage.
- Startup failure in target OS.

## Rollback Procedure

1. Revert to previous stable tag/commit.
2. Rebuild artifact and reinstall plugins/themes from known-good bundle.
3. Re-run smoke gate.
4. Publish incident note with root cause and fix-forward plan.
