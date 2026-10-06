# ==============================================================================
# DidYouDoIt - Windows Installer Generation Script (jpackage)
# Generates a native Windows MSI / EXE installer with bundled JRE
# ==============================================================================

param(
    [string]$PackageType = "msi",   # "msi" or "exe"
    [string]$AppVersion = "1.0.0"
)

$ErrorActionPreference = "Stop"

Write-Host "==> 1. Building DidYouDoIt desktop JAR..." -ForegroundColor Cyan
Set-Location "$PSScriptRoot/../../app"
.\mvnw.cmd clean package -DskipTests

$JarFile = (Get-ChildItem -Path "target" -Filter "DidYouDoIt-*.jar" | Select-Object -First 1).FullName
if (-not $JarFile) {
    Write-Error "Could not locate built JAR in target/ directory."
}

Write-Host "==> 2. Preparing output directory..." -ForegroundColor Cyan
$OutputDir = "$PSScriptRoot/output"
New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

$IconFile = "$PSScriptRoot/../../app/src/main/resources/icons/app-icon.ico"
if (-not (Test-Path $IconFile)) {
    # Fallback to PNG if ICO not present
    $IconFile = "$PSScriptRoot/../../app/src/main/resources/icons/app-icon.png"
}

Write-Host "==> 3. Running jpackage for Windows ($PackageType)..." -ForegroundColor Cyan
jpackage `
    --type $PackageType `
    --input "target" `
    --main-jar (Split-Path $JarFile -Leaf) `
    --main-class "com.aarav.didyoudoit.DidYouDoItApp" `
    --name "DidYouDoIt" `
    --app-version $AppVersion `
    --vendor "Aarav" `
    --copyright "Copyright (C) 2026 DidYouDoIt" `
    --description "Personal Accountability & Nagging Desktop App" `
    --icon $IconFile `
    --win-menu `
    --win-shortcut `
    --win-dir-chooser `
    --dest $OutputDir

Write-Host "==> SUCCESS: Windows package created in: $OutputDir" -ForegroundColor Green
