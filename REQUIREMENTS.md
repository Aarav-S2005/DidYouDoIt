
## Tech Stack

Java + JavaFX + SQLite + JDBC + JNA + Maven + jpackage

## Functional Requirements

|ID|Requirement|
|---|---|
|FR-01|**Task Creation:** Users can create tasks with a title, description, due time, priority, and recurrence.|
|FR-02|**Task Management:** Users can edit, complete, postpone, delete, and restore tasks.|
|FR-03|**Daily Tasks:** The system automatically generates recurring tasks for each day according to their schedule.|
|FR-04|**Reminders:** The system sends Windows notifications when a task becomes due or approaches its deadline.|
|FR-05|**Persistent Nagging:** The system repeatedly reminds users about incomplete overdue tasks until they are completed, postponed, or explicitly dismissed.|
|FR-06|**Escalation:** Reminder frequency and message intensity can increase as a task remains incomplete.|
|FR-07|**Personalized Messages:** The system generates context-aware messages based on the task, lateness, and user's configured personality.|
|FR-08|**Notification Actions:** Notifications allow users to mark a task complete, snooze it, or open the application.|
|FR-09|**Snoozing:** Users can postpone a task for predefined or custom durations.|
|FR-10|**Priority:** Users can assign priorities to tasks, affecting reminder urgency and escalation.|
|FR-11|**Task Categories:** Users can organize tasks into categories such as study, fitness, work, and personal.|
|FR-12|**Daily Dashboard:** The application displays today's completed, pending, overdue, and upcoming tasks.|
|FR-13|**Progress Tracking:** The system records task completion history and displays daily/weekly progress.|
|FR-14|**Streaks:** The system tracks consecutive days of completing recurring tasks.|
|FR-15|**Personality Settings:** Users can select reminder styles such as gentle, strict, sarcastic, or aggressive.|
|FR-16|**Custom Messages:** Users can define custom reminder and escalation messages for specific tasks.|
|FR-17|**Quiet Hours:** Users can configure periods during which reminders and notifications are suppressed.|
|FR-18|**Background Operation:** The application continues monitoring tasks while its main window is closed.|
|FR-19|**System Tray:** Users can access the application, today's tasks, pause reminders, and quit through the Windows system tray.|
|FR-20|**Startup:** The application can automatically launch in the background when the user logs into Windows.|
|FR-21|**Search & Filtering:** Users can search and filter tasks by status, category, priority, and date.|
|FR-22|**Statistics:** The application provides statistics such as completion rate, overdue tasks, streaks, and productivity trends.|
|FR-23|**Data Persistence:** Tasks, settings, history, and statistics remain available after application restarts.|
|FR-24|**Import/Export:** Users can export their tasks and settings and restore them when required.|
|FR-25|**App Control:** Users can pause all reminders temporarily and resume them later.|

---

# Non-Functional Requirements

|ID|Requirement|
|---|---|
|NFR-01|**Performance:** The application should consume minimal CPU and memory while running in the background.|
|NFR-02|**Responsiveness:** User interactions and dashboard operations should respond within a reasonable time without noticeable freezing.|
|NFR-03|**Reliability:** Scheduled reminders should be triggered reliably even when the main JavaFX window is closed.|
|NFR-04|**Persistence:** Application data should not be lost because of normal application shutdowns or restarts.|
|NFR-05|**Startup:** The background service should start automatically and become ready shortly after Windows login.|
|NFR-06|**Availability:** The reminder engine should remain operational throughout the user's Windows session unless explicitly paused.|
|NFR-07|**Usability:** Creating, completing, snoozing, and managing tasks should require minimal user interaction.|
|NFR-08|**Accessibility:** Notifications and UI should remain readable and usable with different display sizes and scaling settings.|
|NFR-09|**Security:** Local task data and application settings should be protected from unauthorized modification where practical.|
|NFR-10|**Privacy:** User task data should remain local by default and should not be transmitted without explicit user consent.|
|NFR-11|**Maintainability:** Scheduling, notification, persistence, and UI components should be modular and independently maintainable.|
|NFR-12|**Extensibility:** The architecture should allow additional platforms, notification providers, and reminder personalities to be added later.|
|NFR-13|**Compatibility:** The application should support modern Windows 10/11 systems and common display configurations.|
|NFR-14|**Fault Tolerance:** Notification or UI failures should not crash the entire reminder engine.|
|NFR-15|**Resource Efficiency:** Background polling and scheduling should avoid unnecessary CPU wakeups and disk operations.|
|NFR-16|**Recoverability:** The application should recover gracefully from unexpected termination and continue pending schedules after restart.|
|NFR-17|**Offline Operation:** Core task management, scheduling, and reminders should work without an internet connection.|
|NFR-18|**Installation:** The application should be installable and uninstallable through a standard Windows installer.|

---

# Business Requirements

| ID    | Requirement                                                                                                                                                            |
| ----- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| BR-01 | **Core Value:** The application should help users actually complete planned tasks rather than merely record them.                                                      |
| BR-02 | **Accountability:** The product should differentiate itself through persistent, personalized accountability and escalation.                                            |
| BR-03 | **Low Friction:** Users should be able to configure their daily routine quickly without extensive setup.                                                               |
| BR-04 | **Personalization:** Users should be able to control how aggressively the application communicates with them.                                                          |
| BR-05 | **Retention:** Streaks, progress statistics, and personalized feedback should encourage users to return consistently.                                                  |
| BR-06 | **Trust:** The application should avoid manipulative behavior that users have explicitly disabled or that violates their configured quiet hours.                       |
| BR-07 | **Privacy First:** Personal schedules, routines, and task history should remain private by default.                                                                    |
| BR-08 | **Free Core Product:** Essential task creation, scheduling, notifications, and nagging should remain available without requiring payment.                              |
| BR-09 | **Potential Monetization:** Premium features could later include AI-generated accountability, advanced analytics, cloud synchronization, and additional personalities. |
| BR-10 | **Platform Strategy:** Windows should be the initial platform, while the architecture should leave room for future iOS/macOS support.                                  |
| BR-11 | **User Control:** Users must always have the ability to pause, snooze, disable, or completely quit the accountability system.                                          |
| BR-12 | **Differentiation:** The product should compete primarily on its **“personal accountability/bully” experience**, not on being another generic to-do list.              |