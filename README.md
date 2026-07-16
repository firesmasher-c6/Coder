# Coder v2.3.2
**The Most Powerful Scripting Minecraft Plugin Allowing In Use Of Java, Lua, And Python.**

Coder allows you to create and execute **.py, .java, and .lua** files.

---

## 🚀 What's New in 2.3.2
* **Upgraded GWI Editor:**
    * **URL changed to `https://coder-gwieditor.firesmasher.workers.dev`**
    * **Visual Studio Code-like Web Interface.**
    * **Multi-Language highlighting support.**
    * **Real-time file sync, save a file and the plugin writes it.**
    * **Cloud-backup, everytime you save a file, its stored on the cloudflare for 15 minutes.**

* **Added Poll Actions:**
    * **Updated the v2.3.1 plugin to v2.3.2 to use the upgraded `/api/poll?=TOKEN` endpoint**
    * **Fixed keep disconnecting to worker problem**

* **Highly-configurable `config.yml`:**
    * **Adds new GENERAL SETTINGS to config.yml with `exit code`, and `config-version`**
    * **`exitCode: '2'` now exits gracefully with a detailed shutdown report.**
  
---

## 🛠 Features
* **Multi-Language Native Execution:** Execute **Java, Python and Lua** directly from your server.
* **Unified Workspace:** Keep all your automation files organized in one place.
* **Java Code Loading:** Load Java Code On To Memory To Keep It Active
* **Organized Cache Folder:** All runtime compiled `.class` files are now inside `Coder/JavaClass/Runtime/` and for the loaded `.class` files are placed on `Coder/JavaClass/Loaded/`.
* **User Execution Control:** Even if your code is `.java`, `.py`, or `.lua` you cannot escape the **UEC**. UEC protects your system.
* **Automated Backup System:** Automatically backup your scripts.
* **Manual Backup System:** Manually backup your scripts.
* **Graphical Web Interface Editor (GWI Editor):** Let's you create, edit, save, read, etc. In a remote web editor that expires within 30 minutes.**
  
## Good News
* **GWI Editor** is safe and secure, by using a **SHA256** Session Tokens, it is impossible for other people to guess your session token within 30 minutes.

## 📖 Quick Start
Place all your files in the following directory:

| Language | Path |
| :--- | :--- |
| **All Scripts** | `/plugins/Coder/scripts/` |
| **All Logs**    | `/plugins/Coder/Logs/`    |

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
* `/coder backup` - backup.
* `/coder auto-backup-start` - starts automatic backups
* `/coder auto-backup-stop` - stops automatic backup.
* `/coder expansion` - Subcommand for expansions.
* `/coder enable-activity-logging` - Enables activity logging.
* `/coder disable-activity-logging` - Disables activity logging.
* `/coder editor [options]` - Lets you use the new **Graphical Web Interface** to edit, create, deletes files.

## ✔️ INFOs
```text
/code editor start
[10:02:14 INFO]: [Coder] Editor started!
[10:02:14 INFO]: Open this link in your browser:
[10:02:14 INFO]: https://coder-gwieditor.firesmasher.workers.dev/editor?session=0a3dd6348de5cd31ff5d7c24999a28d1f11414aa4ea49377c61655763c84c250
[10:02:14 INFO]: When someone connects, you'll be asked to trust them.
[10:02:28 INFO]: [Coder] firesmasher wants to access the editor.
[10:02:28 INFO]: Run /coder editor trust firesmasher to allow or /coder editor doNotTrust firesmasher to reject.
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