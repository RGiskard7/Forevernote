# Build and Test Guide

## Requirements

- JDK 17
- Maven 3.9+

## Standard Build

```bash
./scripts/build_all.sh
```

```powershell
.\scripts\build_all.ps1
```

Output artifact:

- `Forevernote/target/forevernote-1.0.0-uber.jar`

## Maven Build

```bash
mvn -f Forevernote/pom.xml clean package
```

## Tests

```bash
mvn -f Forevernote/pom.xml test
```

## Hardening Gates

```bash
./scripts/smoke-phase-gate.sh
./scripts/hardening-storage-matrix.sh
```

```powershell
.\scripts\smoke-phase-gate.ps1
.\scripts\hardening-storage-matrix.ps1
```

## Plugin and Theme Build

```bash
./scripts/build-plugins.sh
./scripts/build-themes.sh
```

```powershell
.\scripts\build-plugins.ps1
.\scripts\build-themes.ps1
```

## Troubleshooting

- Use launch scripts for JavaFX module-path compatibility.
- Ensure `java` and `mvn` are available in `PATH`.
- JavaFX parent-POM warnings are expected and harmless.
