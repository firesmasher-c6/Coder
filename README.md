# Coder v2.2.1
**The Most Powerful Scripting Minecraft Plugin Allowing In Use Of Java, Lua, And Python.**

Coder allows you to create and execute **.py, .java, and .lua** files.

---

## 🚀 What's New in 2.2.1
* **Automated Addon Security Check:**
    * **For safety, Coder now requires a VERIFIED.vf file on your plugin to make sure you are not hijacking coder to run blocked scripts.**
* **Enhanced API:**
  * **Upgraded Coder API for more methods.**
* **Addon Security. All addons that dont have "VERIFIED.vf" file is immediately rejected but, addon will still work but marked as "Rejected".
  
---

## 🛠 Features
* **Multi-Language Native Execution:** Execute **Java, Python and Lua** directly from your server.
* **Unified Workspace:** Keep all your automation files organized in one place.
* **Java Code Loading:** Load Java Code On To Memory To Keep It Active
* **Organized Cache Folder:** All runtime compiled `.class` files are now inside `Coder/JavaClass/Runtime/` and for the loaded `.class` files are placed on `Coder/JavaClass/Loaded/`.
* **User Execution Control:** Even if your code is `.java`, `.py`, or `.lua` you cannot escape the **UEC**. UEC protects your system.
* **Automated Backup System:** Automatically backup your scripts.
* **Manual Backup System:** Manually backup your scripts.

## Caution
* **Auto Backup** is new and may cause **TPS DROPS** when spammed. To make sure it wont happen please dont spam or backup it when there are backups already inside ``Coder/backups/``. Stacking Backups May Cause It To Be Larger (**GiB**).

## 📖 Quick Start
Place all your files in the following directory:

| Language | Path |
| :--- | :--- |
| **All Scripts** | `/plugins/Coder/scripts/` |

### Command Usage
* `/coder run <filename>` - Executes the specified script.
* `/coder reload <filename>` - Reloads the specified script configuration.
 * `/coder load <filename>` - Loads the specified script to the Server Memory.
 * `/coder unload <filename>` - Unloads a loaded script from the Server Memory.
 * `/coder cancel` - cancels UEC detected script.
 * `/coder confirm` - run a UEC detected script.
 * `/coder update` - fetches the latest version, download link from the official website.
* `/coder update-jar` - Downloads the latest version of the plugin.
* `/coder reload-config` - reloads the main configuration file.

## ⚙️ config.yml
```yaml
# =====================================================
# = Coder Plugin Configuration File
# = Made By FireSmasher.
# =====================================================
# = Web: https://codestuff.pages.dev
# = Github: https://github.com/firesmasher-c6/Coder
# = Modrinth: https://modrinth.com/plugin/coder
# =====================================================

# Main Plugin Settings
plugin:
  # Enable or disable the entire plugin
  enabled: true

  # Supported Languages - Set to true to enable, false to disable
  languages:
    # Enable Python script execution (.py files)
    python: true
    # Enable Lua script execution (.lua files)
    lua: true
    # Enable Java script compilation and execution (.java files)
    java: true

  # Backup Settings
  backups:
    # Backup format (ZIP - works on all systems)
    type: "zip"
    # Backup scheduling
    schedule:
      # Create backup when plugin starts
      on-start: false
      # Cancel auto-backup when plugin disables
      cancel-on-disable: true
      # Backup interval (format: '1m', '30m', '1h', '2h', etc.)
      # Allowed: 1-60 with m/h/d suffix (minutes/hours/days)
      every: "1h"

# Command Settings
# Enable or disable specific commands
# Defaults to true if not specified
commands:
  coder:
    # /coder run <file> - Execute Python, Lua, or Java scripts
    run: true
    # /coder load <file.java> - Load and execute a Java class into memory
    load: true
    # /coder unload <classname> - Unload a Java class from memory
    unload: true
    # /coder reload [file] - Reload plugin, config, or specific script
    reload: true
    # /coder update - Check for plugin updates
    update: true
    # /coder update-jar - Download and install latest plugin version
    update-jar: true
    # /coder confirm - Confirm execution of a script with dangerous imports
    confirm: true
    # /coder cancel - Cancel execution of a pending dangerous script
    cancel: true
    # /coder backup - Create a backup of the Coder folder
    backup: true
    # /coder auto-backup-start - Start automatic backups
    auto-backup-start: true
    # /coder auto-backup-stop - Stop automatic backups
    auto-backup-stop: true

# Logging Settings
logs:
  # Log general script execution errors to Logs/Error-Logs/
  errors: true
  # Log Java compilation errors to Logs/JavaCompile-Errors/
  compile-errors: true
```
