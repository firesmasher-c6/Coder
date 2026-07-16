# Coder Plugin

> **The most powerful, lightweight, and modern scripting engine for Minecraft server automation.**

Coder allows you to create custom commands and logic without the bloat of traditional heavy scripting engines. By utilizing native Java, Python, and Lua execution alongside a custom-built, simple syntax parser, Coder gives you full, uncompromised control over your server environment.

---

## 🚀 Key Features

*   **Native Multi-Language Execution:** Run authentic **Java**, **Python**, and **Lua** scripts natively directly on your server.
*   **Zero Bloat & Lightweight:** Designed from the ground up to be highly optimized with no heavy or unnecessary dependencies.
*   **In-Memory Execution:** Load scripts directly into your server's memory to keep active listeners, tasks, and background processes running continuously.
*   **Real Java Syntax Support:** Skip implementing rigid interfaces just to get compilation errors. Run authentic Java syntax, call custom methods, and tap directly into native Bukkit/Spigot imports.
*   **Active User Execution Control (UEC):** A robust security layer that intercepts unauthorized scripts and blocks malicious system terminal access.
*   **VersionManager:** Keep your system up to date effortlessly with one-tap auto-updates.
*   **Graphical Web Interface (GWI):** Edit, create, and manage your scripts securely in real-time with a VS Code-like web editor.

---

<img width="2480" height="1748" alt="Coder" src="https://github.com/user-attachments/assets/ebe99fb8-4b5a-4c83-806c-1839931df1f2" />

---

## 📥 Installation

1. Drop the `Coder.jar` into your server's `/plugins/` folder.
2. Start (or restart) the server to generate the directory structure.
3. Place your scripts in the designated workspace:
    *   `/plugins/Coder/scripts/` (Scripts)
    *   `/plugins/Coder/Logs/` (Logs)

---

## 📖 Documentation & Guides

For complete API details, scripting tutorials, and command walk-throughs, check out the official documentation:

👉 **[Read the User Guide](https://codestuff.pages.dev/documentation)**

---

## 🛡 Command Reference

### Script Management
*   `/coder run <filename>` – Executes a specified script.
*   `/coder load <filename>` – Loads and compiles a script directly into Server Memory.
*   `/coder unload <filename>` – Unloads an active script from Server Memory.
*   `/coder reload <filename>` – Reloads a specific script's configuration.

### Security & UEC
*   `/coder confirm` – Manually permits a script flagged by the User Execution Control.
*   `/coder cancel` – Blocks and cancels a script caught by the UEC.

### GWI Web Editor
*   `/coder editor start` – Spawns a secure SHA-256 link to access the GWI Web Editor.
*   `/coder editor trust <username>` – Grants access to a pending connection request.
*   `/coder editor do-not-trust <username>` – Denies access to a connection request.
*   `/coder editor stop` – Terminates the active GWI Web Editor session immediately.

---

## ⚠️ Security Warning

Because Coder allows native Python, Java, and Lua execution, **only allow trusted administrators** to manage files in the `/scripts/` directory. Scripts running through Coder have full, native access to system commands and the host environment.