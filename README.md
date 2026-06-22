# Coder Plugin
**The Most Powerful Scripting Minecraft Plugin, lightweight modern plugin for Minecraft server automation.**

Coder allows you to create custom commands and logic without the bloat of traditional heavy scripting engines. By utilizing native Java/Python/Lua execution and a custom-built, simple syntax parser, Coder gives you full control over your server environment.

## 🚀 Features
* **Native Execution:** Execute real **Java** and **Lua/Python** code directly from your server.
* **Lightweight:** No heavy dependencies.
* **Load to Memory:** Load your scripts to your Server's Memory, ensuring that your scripts stay active.
* **Real Java Syntax Execution:** Who needs implementing the ScriptInterface just to get an error? Coder lets you execute real java syntaxes and methods, or alternatively use Bukkit imports.
* **VersionManager:** One tap Auto Update.
* **User Execution Control:** Blocks malicious and system terminal access.
  
<img width="2480" height="1748" alt="Coder" src="https://github.com/user-attachments/assets/ebe99fb8-4b5a-4c83-806c-1839931df1f2" />

## 📥 Installation

1. Drop the `Coder.jar` into your `/plugins` folder.
2. Start the server to generate the folder structure.
3. Place your scripts in the corresponding folders:
    * `/plugins/Coder/scripts/` (Scripts)

## 📖 Documentation
For a full guide on how to write scripts and use commands, see the [User Guide](https://codestuff.pages.dev/documentation).

## ⚠️ Security Warning
Because Coder allows native Python and Java execution, **only allow trusted administrators** to manage files in the `/scripts/` directory, as these have full access to system commands.

