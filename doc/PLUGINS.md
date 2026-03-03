# Plugin System

## Model

Plugins are fully external JARs loaded at startup from the `plugins/` directory.

Core app does not hardcode specific plugin implementations.

## Lifecycle

1. Discover plugin JARs.
2. Load classes with dedicated classloaders.
3. Register plugin metadata.
4. Initialize and enable.
5. Disable/unload/shutdown on request or app close.

## Build Sample Plugins

```bash
./scripts/build-plugins.sh
```

```powershell
.\scripts\build-plugins.ps1
```

This compiles sample plugin sources from `plugins-source/` and copies resulting JARs into `plugins/`.

## Operational Notes

- Plugin failures should degrade gracefully (warning logs, no app crash).
- Keep plugin API backward compatible when possible.
- Always close plugin classloaders during shutdown to prevent leaks.
