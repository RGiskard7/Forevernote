# Hardening Phase Checklist

Use this checklist at the end of each phase.

## Automated checks
- [ ] `mvn -f Forevernote/pom.xml clean test`
- [ ] `mvn -f Forevernote/pom.xml -DskipTests clean package`
- [ ] `./scripts/smoke-phase-gate.sh` (if running on macOS/Linux)
- [ ] `.\scripts\smoke-phase-gate.ps1` (if running on Windows PowerShell)
- [ ] `./scripts/hardening-storage-matrix.sh` or `.\scripts\hardening-storage-matrix.ps1`

## Manual smoke checks
- [ ] Create, edit and save a note
- [ ] Move note to trash and restore it
- [ ] Create folder and subfolder
- [ ] Add and remove tags in note
- [ ] Switch light/dark/system theme
- [ ] Open plugin manager

## Compatibility checks
- [ ] SQLite mode verified
- [ ] FileSystem mode verified
- [ ] Plugins enabled/disabled flow verified

## Release safety
- [ ] No critical runtime errors in logs
- [ ] No major visual regressions
- [ ] Changelog and docs updated for the phase
