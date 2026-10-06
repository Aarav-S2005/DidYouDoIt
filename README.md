# DidYouDoIt? - Personal Accountability & Habit-Nagging System

> The desktop accountability app that doesn't just record your tasks — it makes sure you actually do them.

---

## 📁 Repository Structure

```
DidYouDoIt/
│
├── app/                    # Java desktop application (JavaFX 21 + SQLite)
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   └── resources/
│   │   └── test/
│   ├── pom.xml
│   └── README.md
│
├── frontend/               # Download & landing website (React + Vite + TypeScript)
│   ├── src/
│   ├── public/
│   ├── package.json
│   └── ...
│
├── packaging/              # Native OS packaging scripts (jpackage)
│   ├── windows/            # MSI / EXE installer scripts & WiX config
│   │   ├── package-windows.ps1
│   │   └── README.md
│   └── linux/              # .deb, .rpm, and universal tarball scripts
│       ├── package-deb.sh
│       ├── package-rpm.sh
│       ├── package-tarball.sh
│       └── README.md
│
├── .github/
│   └── workflows/          # GitHub Actions automated build pipelines
│       ├── build-windows.yml
│       └── build-linux.yml
│
├── REQUIREMENTS.md         # Single source of truth for all requirements
├── AGENTS.md               # Architecture governance & paired workflow rules
└── README.md
```

---

## ⚡ Quick Start

### 1. Run the Desktop App (`app/`)
```bash
cd app
./mvnw test          # Run 59 automated unit tests
./mvnw javafx:run    # Launch application in development mode
```

### 2. Run the Download Landing Website (`frontend/`)
```bash
cd frontend
pnpm install
pnpm dev             # Launch Vite dev server
```

### 3. Generate Installers (`packaging/`)
- **Windows**: `cd packaging/windows && .\package-windows.ps1 -PackageType msi`
- **Linux**: `cd packaging/linux && ./package-deb.sh` (or `./package-tarball.sh`)

---

## 💻 Cross-Platform Distribution Matrix

| OS / Distribution | Supported Formats | Engine Integration |
|---|---|---|
| **Windows 10 / 11** | `.msi`, `.exe` | Windows System Tray, Registry Run Auto-start, Action Center Toasts |
| **Debian / Ubuntu / Mint** | `.deb` | XDG Autostart (`.desktop`), Freedesktop `notify-send`, AppIndicator |
| **Fedora / RHEL / openSUSE** | `.rpm` | XDG Autostart (`.desktop`), Freedesktop `notify-send`, AppIndicator |
| **Arch / Manjaro / Any Linux** | Portable `.tar.gz` | Zero-dependency bundled JRE, double-click execution |

---

## 🔒 Governance & Architectural Invariants

1. **Pure JavaFX**: Zero FXML, zero Scene Builder. Pure Java code styled with a central Theme system.
2. **Offline-First & Privacy-Focused**: Local SQLite with WAL mode; zero cloud telemetry or unexpected background network egress.
3. **Escalation Engine**: Dynamic priority-scaled nagging across 4 distinct personality styles (Gentle, Strict, Sarcastic, Aggressive) with over 160 randomized copy variants.
