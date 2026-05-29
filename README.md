# Coder Plugin
**A modern, lightweight "Skript-like" plugin for Minecraft server automation.**

Coder allows you to create custom commands and logic without the bloat of traditional heavy scripting engines. By utilizing native Java/Python execution and a custom-built, simple syntax parser, Coder gives you full control over your server environment.

## 🚀 Features
* **Modern Skript Syntax:** A clean, "Modern Skript" language designed for readability.
* **Native Execution:** Execute real **Java** and **Python** code directly from your server.
* **Lightweight:** No heavy dependencies.
* **Modular:** Separate your variables (`.code`) from your logic (`.script`).
* 
<img width="2480" height="1748" alt="Coder" src="https://github.com/user-attachments/assets/ebe99fb8-4b5a-4c83-806c-1839931df1f2" />

## 📥 Installation

1. Drop the `Coder.jar` into your `/plugins` folder.
2. Start the server to generate the folder structure.
3. Place your scripts in the corresponding folders:
    * `/plugins/Coder/scripts/` (Custom Scripts .script and .code)
    * `/plugins/Coder/Java/scripts/` (Java)
    * `/plugins/Coder/Python/scripts/` (Python)

## 📖 Documentation
For a full guide on how to write scripts and use commands, see the [User Guide](guide.md).

## ⚠️ Security Warning
Because Coder allows native Python and Java execution, **only allow trusted administrators** to manage files in the `Java/` and `Python/` directories, as these have full access to system commands.
