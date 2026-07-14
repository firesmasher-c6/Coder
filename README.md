# Coder v2.2.9
**The Most Powerful Scripting Minecraft Plugin Allowing In Use Of Java, Lua, And Python.**

Coder allows you to create and execute **.py, .java, and .lua** files.

---

## 🚀 What's New in 2.2.9
* **Graphical Web Interface Editor (GWI Editor):**
    * **Let's you create, edit, save, read, etc. In a remote web editor that expires within 30 minutes.**
  
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
coder editor start
[14:05:25 INFO]: [Coder] Editor started!
[14:05:25 INFO]: Open this link in your browser:
[14:05:25 INFO]: https://codestuff-coder.darreltampus39.workers.dev/editor?session=8fb7dec08cc5305e02999c1c6b25212cede1185a62a7016ac7d68e746104b3d1
[14:05:25 INFO]: When someone connects, you'll be asked to trust them.
[14:05:35 INFO]: [Coder] Maules wants to access the editor.
[14:05:35 INFO]: Run /coder editor trust Maules to allow or /coder editor doNotTrust Maules to reject.
coder editor trust Maules
[14:05:43 INFO]: [Coder] Trusted Maules. Editor is now unlocked.
```