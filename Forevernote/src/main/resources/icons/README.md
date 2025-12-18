# Application Icons

Place your application icons here in the following formats:

- **Windows**: `app-icon.ico` (multiple sizes: 16x16, 32x32, 48x48, 256x256)
- **macOS**: `app-icon.icns` (Apple icon format)
- **Linux**: `app-icon.png` (128x128 or 256x256 recommended)

## Icon Requirements

### Windows (.ico)
- Format: ICO file with multiple embedded sizes
- Recommended sizes: 16x16, 32x32, 48x48, 256x256
- Tools: Use online converters or tools like GIMP, ImageMagick

### macOS (.icns)
- Format: ICNS (Apple Icon Image format)
- Recommended sizes: 16x16, 32x32, 128x128, 256x256, 512x512, 1024x1024
- Tools: Use `iconutil` on macOS or online converters

### Linux (.png)
- Format: PNG with transparency
- Recommended size: 128x128 or 256x256
- Tools: Any image editor (GIMP, Photoshop, etc.)

## Configuration

Icon paths are configured in `src/main/resources/app.properties`:
- `app.icon.windows=src/main/resources/icons/app-icon.ico`
- `app.icon.macos=src/main/resources/icons/app-icon.icns`
- `app.icon.linux=src/main/resources/icons/app-icon.png`

If icons are not found, the packaging scripts will skip the `--icon` parameter (application will still work, just without custom icon).

