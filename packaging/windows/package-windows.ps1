# ==============================================================================
# DidYouDoIt - Windows Packaging & Distribution Script (jpackage)
# Generates a standalone Windows application with self-contained JRE.
# Supports 'app-image' (standalone folder / portable), 'msi', and 'exe'.
# Note: 'msi' and 'exe' installers require WiX Toolset 3.x installed on the system.
# ==============================================================================

param(
    [string]$PackageType = "app-image",   # "app-image", "msi", or "exe"
    [string]$AppVersion = "1.0.0"
)

$ErrorActionPreference = "Stop"

Write-Host "==> 1. Building DidYouDoIt desktop executable JAR..." -ForegroundColor Cyan
Push-Location "$PSScriptRoot/../../app"
try {
    .\mvnw.cmd clean package -DskipTests
} finally {
    Pop-Location
}

$JarFile = "$PSScriptRoot/../../app/target/DidYouDoIt-1.0-SNAPSHOT-all.jar"
if (-not (Test-Path $JarFile)) {
    Write-Error "Could not locate shaded JAR at $JarFile"
}

Write-Host "==> 2. Preparing packaging staging & output directories..." -ForegroundColor Cyan
$StagingDir = "$PSScriptRoot/staging"
$OutputDir = "$PSScriptRoot/output"

if (Test-Path $StagingDir) { Remove-Item -Recurse -Force $StagingDir }
New-Item -ItemType Directory -Force -Path "$StagingDir/jars" | Out-Null
New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

$AppDestDir = "$OutputDir/DidYouDoIt"
if (Test-Path $AppDestDir) {
    Remove-Item -Recurse -Force $AppDestDir
}

Copy-Item $JarFile "$StagingDir/jars/DidYouDoIt.jar"

$IconFile = "$PSScriptRoot/../../app/src/main/resources/icons/app-icon.ico"
if (-not (Test-Path $IconFile)) {
    $IconFile = "$PSScriptRoot/../../app/src/main/resources/icons/app-icon.png"
}

Write-Host "==> 3. Running jpackage for Windows (Format: $PackageType)..." -ForegroundColor Cyan

$jpackageArgs = @(
    "--type", $PackageType,
    "--input", "$StagingDir/jars",
    "--main-jar", "DidYouDoIt.jar",
    "--main-class", "com.aarav.didyoudoit.Main",
    "--name", "DidYouDoIt",
    "--app-version", $AppVersion,
    "--vendor", "Aarav",
    "--copyright", "Copyright (C) 2026 DidYouDoIt",
    "--description", "Personal Accountability & Nagging Desktop App",
    "--icon", $IconFile,
    "--dest", $OutputDir
)

if ($PackageType -ne "app-image") {
    $jpackageArgs += @(
        "--win-menu",
        "--win-shortcut",
        "--win-dir-chooser"
    )
}

& jpackage @jpackageArgs

if ($PackageType -eq "app-image") {
    Write-Host "==> 4. Creating portable zip archive..." -ForegroundColor Cyan
    $ZipPath = "$OutputDir/DidYouDoIt-Windows-Portable-v$AppVersion.zip"
    if (Test-Path $ZipPath) { Remove-Item -Force $ZipPath }
    Compress-Archive -Path "$OutputDir/DidYouDoIt/*" -DestinationPath $ZipPath
    Write-Host "==> Portable zip created: $ZipPath" -ForegroundColor Green
}

Write-Host "==> SUCCESS: Windows distribution ready in: $OutputDir" -ForegroundColor Green
