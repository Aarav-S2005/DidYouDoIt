# DidYouDoIt? 🎯

> **Most to-do lists just sit there quietly while deadlines slip away. DidYouDoIt? makes sure you actually finish them.**

DidYouDoIt? is a personal accountability desktop app for **Windows and Linux**. Instead of acting like another passive digital notebook where tasks go to be forgotten, it acts like an honest, persistent companion sitting in your system tray—checking in on your schedule, keeping you focused, and escalating reminders until your work is done.

🌐 **Website & Live Demo:** [did-you-do-it-app.vercel.app](https://did-you-do-it-app.vercel.app/)

---

## 💡 The Problem with Traditional To-Do Apps

We have all done this:
1. Open a fancy to-do list.
2. Carefully organize 10 important goals for the week.
3. Close the window.
4. Completely forget about them while spending the afternoon browsing distractions.

Traditional productivity apps rely entirely on **your own motivation** to open them back up. When you get distracted or procrastinate, they stay silent.

**DidYouDoIt? flips this around.** You tell it what you need to do and when, and it takes responsibility for keeping you honest.

---

## ✨ Key Features

### 1. 🔔 Persistent Accountability Daemon
DidYouDoIt? doesn't disappear when you close its window. It lives quietly in your system tray or menu bar:
- Watches your due dates in the background with near-zero CPU and memory usage.
- Sends native desktop notifications the moment a task is due.
- If a task is ignored, reminders don't just disappear—they gradually escalate in urgency until you either complete the task or consciously snooze it.

### 2. 🎭 4 Distinct Nagging Personalities
Everyone responds to accountability differently. Pick the voice that actually gets you moving:
- **🌱 Gentle (Mindful & Supportive):** Empathetic nudges that encourage you with warmth and positivity.
- **📋 Strict (Military Discipline):** Clear, objective, no-nonsense reminders focused purely on execution and punctuality.
- **😏 Sarcastic (Witty Reality Checks):** Playful roasts and humorous call-outs to snap you out of doomscrolling.
- **🔥 Aggressive (High Stakes & High Energy):** Urgent, intense wake-up calls designed for when excuses need to stop immediately.

Each personality features **over 40–50 unique randomized variations per tier**, so your notifications always feel fresh and unpredictable.

### 3. 🌙 Respects Your Downtime (Quiet Hours)
Accountability doesn't mean burnout. Configure your custom daily Quiet Hours (e.g., `10:00 PM – 7:00 AM`). During these hours, all notifications and pop-ups pause completely so you can sleep or relax undisturbed.

### 4. 🔒 100% Private & Fully Offline
- **No accounts or sign-ups.**
- **No cloud servers or monthly subscriptions.**
- **Zero data collection or tracking.**
All tasks, routines, and completion histories are stored exclusively on your own computer in a local database. It works perfectly without an internet connection.

### 5. 📊 Real Progress & Consistency Streaks
Track how consistent you really are over time with clear completion streaks, overdue breakdowns, and category tags.

---

## 💻 Download & Installation

Visit the [Download Website](https://did-you-do-it-app.vercel.app/) or grab the latest release from the [GitHub Releases](https://github.com/Aarav-S2005/DidYouDoIt/releases) page:

### Windows 10 / 11
- Download `DidYouDoIt-Windows-Portable-v1.0.0.zip`.
- Extract the zip anywhere on your PC.
- Double-click `DidYouDoIt.exe` to run. (No Java installation required—everything is self-contained!)

### Linux
- **Debian / Ubuntu / Mint / Pop!_OS:**
  ```bash
  sudo apt install ./didyoudoit_1.0.0_amd64.deb
  ```
- **Fedora, Arch, openSUSE & Universal Tarball:**
  ```bash
  tar -xzvf didyoudoit-1.0.0-linux-x64.tar.gz
  cd didyoudoit
  ./bin/didyoudoit
  ```

---

## 🛠️ Built With

- **Desktop Application:** Modern JavaFX & local SQLite.
- **Landing Website:** React 19, TypeScript, Vite & Vanilla CSS.
- **Packaging:** Native JDK `jpackage` bundling self-contained runtimes for Windows and Linux.

---

## 📄 License

Free and open source under the MIT License.
