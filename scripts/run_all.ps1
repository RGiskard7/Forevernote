<#
Run script for Forevernote (Windows PowerShell)
Usage: .\scripts\run_all.ps1

This script launches Forevernote with JavaFX module-path configuration if needed.
#>
param()

Set-StrictMode -Version Latest

$root = Split-Path -Parent $PSScriptRoot
Push-Location (Join-Path $root 'Forevernote')

$jar = Join-Path (Get-Location) 'target\forevernote-1.0.0-uber.jar'
if (Test-Path $jar) {
    Write-Host "Launching Forevernote..." -ForegroundColor Green
    Write-Host "JAR: $jar" -ForegroundColor Cyan
    
    # Attempt to find JavaFX modules in Maven repository
    $m2Repo = Join-Path $env:USERPROFILE '.m2\repository'
    $javafxModules = @()
    
    if (Test-Path $m2Repo) {
        $javafxDirs = Get-ChildItem -Path "$m2Repo\org\openjfx" -Directory -ErrorAction SilentlyContinue | 
                      Where-Object { $_.Name -match 'javafx-' } |
                      Get-ChildItem -Directory | Where-Object { $_.Name -match '^21' }
        
        foreach ($dir in $javafxDirs) {
            $javafxModules += $dir.FullName
        }
    }
    
    if ($javafxModules.Count -gt 0) {
        $modulePath = $javafxModules -join ';'
        Write-Host "Using JavaFX module-path: $modulePath" -ForegroundColor Cyan
        & java --module-path $modulePath --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.media -jar $jar
    } else {
        Write-Host "JavaFX modules not found. Attempting standard JAR launch..." -ForegroundColor Yellow
        & java -jar $jar
    }
    
    if ($LASTEXITCODE -eq 1) {
        Write-Warning "JAR launch failed. Attempting via Maven exec:java..."
        & mvn -f (Join-Path (Get-Location) 'pom.xml') exec:java -Dexec.mainClass="com.example.forevernote.Main" 2>&1
    }
} else {
    Write-Host "Packaged JAR not found at $jar" -ForegroundColor Red
    Write-Host "Attempting to run via Maven..." -ForegroundColor Yellow
    & mvn -f (Join-Path (Join-Path $root 'Forevernote') 'pom.xml') exec:java -Dexec.mainClass="com.example.forevernote.Main" 2>&1
}

Pop-Location
