# Scripts Reference

## Core Build/Run

- `build_all.sh` / `build_all.ps1`: clean build of app artifact.
- `run_all.sh` / `run_all.ps1`: run app with development-friendly flow.
- `launch-forevernote.sh` / `.bat` / `.ps1`: recommended production launchers.

## Quality Gates

- `smoke-phase-gate.sh` / `.ps1`: smoke checks.
- `hardening-storage-matrix.sh` / `.ps1`: backend parity checks.

## Plugins and Themes

- `build-plugins.sh` / `.ps1`: compile/install external plugins to `plugins/`.
- `build-themes.sh` / `.ps1`: install themes to runtime theme directory.

## Packaging

- `package-linux.sh`
- `package-macos.sh`
- `package-windows.ps1`

## Utilities

- `schema.txt`: schema notes/reference.
- `cleanup-installers.ps1`: cleanup helper for packaging output.
