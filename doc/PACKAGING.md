# 📦 Packaging Guide - Native Installers

This guide explains how to create native installers for Forevernote on Windows, macOS, and Linux using `jpackage`.

## 📋 Prerequisites

1. **JDK 17 or higher** (not just JRE)
   - `jpackage` is included in JDK 17+
   - Download from: https://adoptium.net/
   - Verify with: `jpackage --version`

2. **Maven** installed and configured

3. **Platform-specific requirements:**
   - **Windows**: WiX Toolset (optional, for MSI signing)
   - **macOS**: Xcode Command Line Tools (for code signing, optional)
   - **Linux**: `fakeroot` and `dpkg` (for DEB) or `rpmbuild` (for RPM)

## 🪟 Windows (EXE/MSI Installer)

### Generate Windows Installer

```powershell
.\scripts\package-windows.ps1
```

This will create:
- **EXE installer** (default): `Forevernote/target/installers/Forevernote-1.0.0.exe`
- **MSI installer** (if WiX is installed): `Forevernote/target/installers/Forevernote-1.0.0.msi`

The script automatically detects if WiX Toolset is available and creates MSI if possible, otherwise creates EXE (which works on all Windows without additional tools).

### Features

- ✅ Native Windows installer (EXE or MSI)
- ✅ Includes Java runtime
- ✅ Start menu shortcut
- ✅ Desktop shortcut
- ✅ Uninstaller in Control Panel (both formats)

### Installation

Users can double-click the installer file and follow the installation wizard.

### MSI vs EXE

- **EXE**: Works on all Windows 10/11 without additional tools (default)
- **MSI**: Requires WiX Toolset installed (optional, for MSI format)

To create MSI installers, install WiX Toolset from https://wixtoolset.org and add it to your PATH.

## 🍎 macOS (DMG Installer)

### Generate DMG Installer

```bash
./scripts/package-macos.sh
```

This will create: `Forevernote/target/installers/Forevernote-1.0.0.dmg`

### Features

- ✅ Native macOS disk image
- ✅ Includes Java runtime
- ✅ Drag-and-drop installation
- ✅ Application bundle (.app)
- ✅ Code signing support (optional)

### Installation

Users can:
1. Open the DMG file
2. Drag Forevernote to Applications folder
3. Launch from Applications

### Code Signing (Optional)

To sign the application for distribution:

```bash
jpackage ... --mac-sign --mac-signing-key-user-name "Developer ID Application: Your Name"
```

## 🐧 Linux (DEB/RPM Installer)

### Generate Linux Installer

```bash
./scripts/package-linux.sh
```

This will create:
- **Debian/Ubuntu**: `Forevernote/target/installers/forevernote_1.0.0-1_amd64.deb`
- **RedHat/Fedora**: `Forevernote/target/installers/forevernote-1.0.0-1.x86_64.rpm`

### Features

- ✅ Native package manager integration
- ✅ Includes Java runtime
- ✅ Desktop shortcut
- ✅ Application menu entry
- ✅ Easy uninstallation

### Installation

**Debian/Ubuntu:**
```bash
sudo dpkg -i forevernote_1.0.0-1_amd64.deb
```

**RedHat/Fedora:**
```bash
sudo rpm -i forevernote-1.0.0-1.x86_64.rpm
```

## 🔧 Advanced Configuration

### Customize Installer

Edit the package scripts to customize:
- Application name
- Vendor information
- Icons
- Installation directory
- Java runtime options

### Example: Custom Icon

```bash
jpackage ... --icon path/to/icon.ico  # Windows
jpackage ... --icon path/to/icon.icns # macOS
jpackage ... --icon path/to/icon.png  # Linux
```

### Example: Custom Java Options

```bash
jpackage ... --java-options "-Xmx2G -Dfile.encoding=UTF-8"
```

## 📊 Installer Sizes

Typical installer sizes:
- **Windows MSI**: ~80-100 MB (includes Java)
- **macOS DMG**: ~80-100 MB (includes Java)
- **Linux DEB/RPM**: ~80-100 MB (includes Java)

## 🚀 Distribution

### Windows

1. Generate MSI installer
2. Test on clean Windows system
3. Upload to distribution platform
4. Users download and install

### macOS

1. Generate DMG installer
2. Code sign (recommended for distribution)
3. Notarize with Apple (required for Gatekeeper)
4. Upload to distribution platform

### Linux

1. Generate DEB/RPM installer
2. Test on target distribution
3. Upload to distribution platform or PPA/repository

## 🔍 Troubleshooting

### Error: "jpackage not found"

Install JDK (not JRE) from https://adoptium.net/

### Error: "WiX Toolset not found" (Windows)

WiX is optional. The script will work without it, but MSI signing won't be available.

### Error: "fakeroot not found" (Linux)

Install with:
```bash
sudo apt-get install fakeroot  # Debian/Ubuntu
sudo yum install fakeroot     # RedHat/Fedora
```

### Error: "Code signing failed" (macOS)

Code signing is optional. Remove `--mac-sign` from the script if you don't have a developer certificate.

---

**Note**: Native installers include Java, so users don't need to install Java separately. This makes distribution much easier!

