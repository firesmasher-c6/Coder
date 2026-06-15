# CoderJavaFixer 🛠️🛡️

`CoderJavaFixer` is a lightweight security addon for the Coder runtime plugin, designed to manage script compilation and prevent malicious system access.

## ✨ Key Features
* **Secure Compilation:** Safely intercepts `/coder` requests for live source compilation.
* **User Execution Control (UEC):** Scans scripts for unauthorized terminal handles (e.g., `Runtime.exec`, `ProcessBuilder`) before execution.
* **Bypass Logic:** Seamlessly trusts the server `CONSOLE` and defined administrator accounts.
* **Resource Efficiency:** Automates build-cache cleanup to keep your server directory clean.

## 🛡️ Commands
| Command | Description |
| :--- | :--- |
| `/coder run <file.java>` | Compiles and runs a script from the Coder scripts folder. |
| `/cjf compile <file.java>` | Compiles a script into the protected cache without running. |
| `/cjf execute-class <file>` | Runs a previously compiled class from the cache. |

---
