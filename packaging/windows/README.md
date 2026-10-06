# Windows Packaging

## Prerequisites
- JDK 21+ with `jpackage` on `PATH`
- WiX Toolset 3.11+ (required by `jpackage` to build `.msi` installers on Windows)

## Build Commands

```powershell
# Build standard MSI installer
.\package-windows.ps1 -PackageType msi

# Build EXE installer
.\package-windows.ps1 -PackageType exe
```

The output installer will be placed in `packaging/windows/output/`.
