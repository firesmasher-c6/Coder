# Coder v2.4.3

> **The most powerful scripting engine for Minecraft. Run Java, Lua, and Python natively on your server with real-time web editing.**

![Compatibility](https://img.shields.io/badge/compatibility-Paper/Purpur-green)
![Version](https://img.shields.io/badge/version-2.4.3-green)
---

## 🚀 What's New in v2.4.3

*   **BackupManager fix:**
    *   **Fixed zipping problem**
        > Zipping below 2.4.2 caused zipping it self over and over causing a GIGABYTE ZIP.
    
*   **Coder Addons Lising:**
    *   **Run `/pl` or `/plugins` and the plugin will log coder addons. Along with the server plugins.**

*   **Added API Versoning:**
    *   **



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
*   **Virtual Console Interface (VCI):** Lets you securely run commands via `https://coder-gwieditor.firesmasher.workers.dev/mc-console?=YOUR_TOKEN_HERE`.
*   **Java 25 Support:** Download the java-25 version of Coder.

### 🛡 GWI Editor Security
The remote GWI Editor is highly secure. Session connections leverage **SHA-256 tokens** which are mathematically impossible to brute-force or guess. Additionally, every session token automatically expires after 30 minutes of inactivity.

### 🛡 Virtual Console Interface Security
The custom minecraft console panel blocks /op commands and is secured by `SHA-256` Server Password tokens, to disable this feature, you can go to `/plugins/Coder/.gwi/secure/serverPassword.env` and remove the value of `serverPassword=`.

---

## 📖 Quick Start

### Workspace Directories
Drop your scripts and check execution logs in the following directories:

| Purpose | Server Path |
| :--- | :--- |
| **Scripts Directory** | `/plugins/Coder/scripts/` |
| **Execution Logs** | `/plugins/Coder/Logs/` |
| **GWI Directory** | `/plugins/Coder/.gwi/` |

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

#### Virtual Console Interface
*   `/coder gen-pass` - Generates a unique SHA-256 server password to access `/mc-console/`.

#### System & Maintenance
*   `/coder backup` – Triggers an immediate manual backup of your script directory.
*   `/coder auto-backup-start` – Starts the automated scheduler for background backups.
*   `/coder auto-backup-stop` – Halts the background backup scheduler.
*   `/coder reload-config` – Hot-reloads the main `config.yml` file.
*   `/coder update` – Checks for updates and retrieves a download link.
*   `/coder update-jar` – Automatically downloads and replaces the plugin JAR file.
*   `/coder enable-activity-logging` – Enables activity logging.
*   `/coder disable-activity-logging` – Disables activity logging.

#### Coderc (Coder Compiler)
*   `/coderc run <filename.class>` - Runs a .class file.
*   `/coderc compile <filename.java>` - Compiles a .java into .class
*   **Aliases:**
    > */cojav, /cjavac, /cjava, /codec*
