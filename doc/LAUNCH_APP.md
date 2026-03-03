# Launch Guide

## Recommended Launchers

```bash
./scripts/launch-forevernote.sh
```

```powershell
.\scripts\launch-forevernote.bat
# or
.\scripts\launch-forevernote.ps1
```

These scripts handle JavaFX runtime/module-path details automatically.

## Alternative Launch

```bash
mvn -f Forevernote/pom.xml exec:java -Dexec.mainClass="com.example.forevernote.Launcher"
```

## Runtime Directories

Created on first run if missing:

- `data/`
- `logs/`

## Common Issues

- If JavaFX runtime components appear missing, use launcher scripts.
- Confirm Java and Maven versions (`java -version`, `mvn -version`).
