# Plugin Source Workspace

This directory contains source projects for sample external plugins.

## Build

From repository root:

```bash
./scripts/build-plugins.sh
```

```powershell
.\scripts\build-plugins.ps1
```

Built plugin jars are copied into `plugins/` for runtime discovery.

## Guidelines

- Keep plugin APIs compatible with current `plugin/` contracts.
- Do not hardcode plugin logic into core app.
- Validate plugin lifecycle (load/enable/disable/shutdown).
