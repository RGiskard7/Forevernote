@echo off
REM Forevernote Launcher for Windows
REM This batch file launches Forevernote with proper JavaFX module-path configuration

setlocal enabledelayedexpansion

REM Get the directory where this script is located
set SCRIPT_DIR=%~dp0

REM Set JAR path
set JAR=%SCRIPT_DIR%Forevernote\target\forevernote-1.0.0-uber.jar

REM Check if JAR exists
if not exist "%JAR%" (
    echo Error: JAR not found at %JAR%
    echo Please run the build scripts first
    pause
    exit /b 1
)

REM Detect Maven repository location
set M2_REPO=%USERPROFILE%\.m2\repository

REM Build JavaFX module path from Maven repository
REM Look for specific javafx modules in version 21
set JAVAFX_MODULES=

REM Find the actual version directory (e.g., 21.0.1)
for /d %%v in ("%M2_REPO%\org\openjfx\javafx-base\21*") do (
    if exist "%%v" (
        set "JAVAFX_MODULES=%%v"
        goto :found_base
    )
)
:found_base

for /d %%v in ("%M2_REPO%\org\openjfx\javafx-controls\21*") do (
    if exist "%%v" (
        if not "!JAVAFX_MODULES!"=="" (
            set "JAVAFX_MODULES=!JAVAFX_MODULES!;%%v"
        ) else (
            set "JAVAFX_MODULES=%%v"
        )
        goto :found_controls
    )
)
:found_controls

for /d %%v in ("%M2_REPO%\org\openjfx\javafx-fxml\21*") do (
    if exist "%%v" (
        if not "!JAVAFX_MODULES!"=="" (
            set "JAVAFX_MODULES=!JAVAFX_MODULES!;%%v"
        ) else (
            set "JAVAFX_MODULES=%%v"
        )
        goto :found_fxml
    )
)
:found_fxml

for /d %%v in ("%M2_REPO%\org\openjfx\javafx-graphics\21*") do (
    if exist "%%v" (
        if not "!JAVAFX_MODULES!"=="" (
            set "JAVAFX_MODULES=!JAVAFX_MODULES!;%%v"
        ) else (
            set "JAVAFX_MODULES=%%v"
        )
        goto :found_graphics
    )
)
:found_graphics

for /d %%v in ("%M2_REPO%\org\openjfx\javafx-web\21*") do (
    if exist "%%v" (
        if not "!JAVAFX_MODULES!"=="" (
            set "JAVAFX_MODULES=!JAVAFX_MODULES!;%%v"
        ) else (
            set "JAVAFX_MODULES=%%v"
        )
        goto :found_web
    )
)
:found_web

REM Launch with module-path if JavaFX modules were found
if not "!JAVAFX_MODULES!"=="" (
    echo Launching Forevernote with JavaFX module-path...
    java --module-path "!JAVAFX_MODULES!" --add-modules javafx.base,javafx.controls,javafx.fxml,javafx.graphics,javafx.web -jar "%JAR%"
) else (
    echo JavaFX modules not found. Attempting standard JAR launch...
    java -jar "%JAR%"
)

exit /b !ERRORLEVEL!
