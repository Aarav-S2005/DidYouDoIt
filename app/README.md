# DidYouDoIt? - Desktop Application

> A personal accountability and task-nagging desktop application built with JavaFX for Windows and Linux.

---

## 🛠 Tech Stack

- **Runtime**: Java 21+ / Java 25
- **UI Framework**: JavaFX 21 (Modular, pure Java code — no FXML, no Scene Builder)
- **Persistence**: SQLite 3 via JDBC (WAL mode)
- **Native OS Integration**: JNA (Win32 Registry & SystemTray on Windows, XDG Autostart & Freedesktop notifications on Linux)
- **Build System**: Apache Maven (`pom.xml` + Maven Wrapper)

---

## 🏛 Architecture

The desktop application follows strict architectural separation:

```
com.aarav.didyoudoit/
├── model/           # Immutable domain entities & enums (Task, AppSettings, EscalationLevel, etc.)
├── repository/      # Interface-driven data persistence (SqliteTaskRepository, etc.)
├── service/         # Business logic (NaggingDaemon, StreakService, PersonalityMessages, etc.)
├── viewmodel/       # Reactive presentation state (DashboardViewModel, SettingsViewModel, etc.)
├── view/            # JavaFX views assembled in pure Java (DashboardView, TaskDialog, etc.)
├── ui/
│   ├── components/  # Reusable widgets (TaskCardView, ToastNotificationCard, CategoryChip, etc.)
│   └── theme/       # Central design system (Theme.java, FontManager.java, theme.css)
└── util/            # Deterministic time abstraction (ClockService, SystemClockService)
```

---

## 🚀 Running Locally

### Prerequisites
- JDK 21 or higher installed (`java -version`)

### Commands

```powershell
# Run unit tests
./mvnw test

# Launch the desktop application
./mvnw javafx:run

# Package executable JAR
./mvnw clean package
```

---

## 🧪 Testing

The application includes 59 unit tests covering:
- Task CRUD, recurrence spawning, and soft deletion
- Streaks calculation and activity logging
- Background nagging daemon escalation cycles and interval enforcement
- Quiet hours and reminder suppression
- JSON backup export and restore
- Windows & Linux auto-start registration
