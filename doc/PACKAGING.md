# Packaging Guide

## Build Uber JAR

```bash
mvn -f Forevernote/pom.xml clean package -DskipTests
```

Output:

- `Forevernote/target/forevernote-1.0.0-uber.jar`

## Platform Packaging Scripts

```bash
./scripts/package-linux.sh
./scripts/package-macos.sh
```

```powershell
.\scripts\package-windows.ps1
```

## Packaging Validation Checklist

1. Application launches using platform launcher.
2. Note CRUD works in SQLite and FileSystem mode.
3. Trash/restore workflows work for notes and folders.
4. Plugin manager opens and detects external plugins.
5. Theme switching works (built-in + external).
