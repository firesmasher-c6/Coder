# Coder v2.3.2

> **The most powerful scripting engine for Minecraft. Run Java, Lua, and Python natively on your server with real-time web editing.**

---

## 🚀 What's New in v2.3.2

*   **Upgraded GWI Editor:**
    *   **New Domain:** Now hosted at `https://coder-gwieditor.firesmasher.workers.dev`.
    *   **VS Code-Like UI:** Familiar, high-performance web interface.
    *   **Multi-Language Syntax Highlighting:** Clear readability for `.java`, `.py`, and `.lua`.
    *   **Real-Time Sync:** Saving a file on the editor instantly writes it to your server.
    *   **Cloud Backup:** Automatically retains saved files on Cloudflare KV for 15 minutes as an emergency rollback.
*   **Enhanced Polling Mechanism:**
    *   Updated the internal API to use `/api/poll?=TOKEN` for ultra-stable editor connections.
    *   Resolved connection dropout issues between the server and Cloudflare Workers.
*   **Configurable `config.yml`:**
    *   Introduced `exitCode` and `config-version` settings under the `GENERAL SETTINGS` block.
    *   Setting `exitCode: '2'` now initiates a graceful server exit accompanied by a comprehensive shutdown report.

---

## 🛠 Features

*   **Native Multi-Language Support:** Write and execute scripts in **Java, Python, and Lua** directly on your Minecraft server.
*   **Unified Workspace:** Keep all your automation files organized in a single, dedicated folder.
*   **In-Memory Java Execution:** Load Java code directly into server memory to keep active listeners and background processes running.
*   **Clean Cache Directory:** Compiled `.class` files are cleanly separated:
    *   Temporary runtime files: `Coder/JavaClass/Runtime/`
    *   Loaded persistent files: `Coder/JavaClass/Loaded/`
*   **User Execution Control (UEC):** An active security layer that intercepts unauthorized scripts. Regardless of the language used, nothing runs without UEC verification.
*   **Dual Backup Systems:** Automated scheduled backups paired with instant manual snapshots.
*   **Graphical Web Interface (GWI):** Securely manage, edit, and delete files on your server using a remote web browser window.

### 🛡 GWI Editor Security
The remote GWI Editor is highly secure. Session connections leverage **SHA-256 tokens** which are mathematically impossible to brute-force or guess. Additionally, every session token automatically expires after 30 minutes of inactivity.

---

## 📖 Quick Start

### Workspace Directories
Drop your scripts and check execution logs in the following directories:

| Purpose | Server Path |
| :--- | :--- |
| **Scripts Directory** | `/plugins/Coder/scripts/` |
| **Execution Logs** | `/plugins/Coder/Logs/` |

---

### Command Reference

#### Script Management
*   `/coder run <filename>` – Executes a specified script.
*   `/coder load <filename>` – Loads and compiles a script directly into Server Memory.
*   `/coder unload <filename>` – Unloads an active script from Server Memory.
*   `/coder reload <filename>` – Reloads a specific script's configuration.

#### Security & UEC
*   `/coder confirm` – Manually permits a script flagged by the User Execution Control.
*   `/coder cancel` – Blocks and cancels a script caught by the UEC.

#### GWI Web Editor
*   `/coder editor start` – Spawns a secure SHA-256 link to access the GWI Web Editor.
*   `/coder editor trust <username>` – Grants access to a pending connection request.
*   `/coder editor do-not-trust <username>` – Denies access to a connection request.
*   `/coder editor stop` – Terminates the active GWI Web Editor session immediately.

#### System & Maintenance
*   `/coder backup` – Triggers an immediate manual backup of your script directory.
*   `/coder auto-backup-start` – Starts the automated scheduler for background backups.
*   `/coder auto-backup-stop` – Halts the background backup scheduler.
*   `/coder reload-config` – Hot-reloads the main `config.yml` file.
*   `/coder update` – Checks for updates and retrieves a download link.
*   `/coder update-jar` – Automatically downloads and replaces the plugin JAR file.
*   `/coder enable-activity-logging` – Enables activity logging.
*   `/coder disable-activity-logging` – Disables activity logging.

---

## 📋 Console Session Example

Here is a typical session workflow demonstrating how to initialize the web editor, authorize a user, and safely shut it down:

```text
/code editor start
[10:02:14 INFO]: [Coder] Editor started!
[10:02:14 INFO]: Open this link in your browser:
[10:02:14 INFO]: https://coder-gwieditor.firesmasher.workers.dev/editor?session=0a3dd6348de5cd31ff5d7c24999a28d1f11414aa4ea49377c61655763c84c250
[10:02:14 INFO]: When someone connects, you'll be asked to trust them.
[10:02:28 INFO]: [Coder] firesmasher wants to access the editor.
[10:02:28 INFO]: Run /coder editor trust firesmasher to allow or /coder editor do-not-trust firesmasher to reject.
code editor trust firesmasher
[10:02:30 INFO]: [Coder] Trusted firesmasher. Editor is now unlocked.
code editor trust firesmasher
[10:22:42 INFO]: [Coder] Trusted firesmasher. Editor is now unlocked.
code editor trust firesmasher
[10:41:07 INFO]: [Coder] Trusted firesmasher. Editor is now unlocked.
/code editor stop
[10:41:12 INFO]: [Coder] Editor session closed.
/code editor start
[10:41:18 INFO]: [Coder] Editor started!
[10:41:18 INFO]: Open this link in your browser:
[10:41:18 INFO]: https://coder-gwieditor.firesmasher.workers.dev/editor?session=c116beadb1afb910f409f7273415199712d95eb82f39cb4a648b9c722e47a4cc
[10:41:18 INFO]: When someone connects, you'll be asked to trust them.
[10:41:32 INFO]: [Coder] dir wants to access the editor.
[10:41:32 INFO]: Run /coder editor trust dir to allow or /coder editor doNotTrust dir to reject.
code editor trust dir
[10:41:34 INFO]: [Coder] Trusted dir. Editor is now unlocked.
```