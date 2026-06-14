# CoderJavaFixer 🛠️🛡️

`CoderJavaFixer` is a lightweight, high-performance security addon for Paper/Spigot Minecraft servers running the Coder runtime plugin ecosystem. It implements advanced compilation routing and **User Execution Control (UEC)** scanning matrices to ensure scripts executed by players do not contain malicious system handles.

## ✨ Features

* **Dynamic Command Routing:** Intercepts `/coder run` and `/coder execute` requests cleanly to handle live source compilation via Java's native compiler API.
* **Warning-Free Registration:** Programmatically maps fallback aliases (`/coderjavafixer`, `/cjf`) directly into the live server command table, eliminating deprecated API warnings.
* **User Execution Control (UEC):** Scans file contents before compilation to prevent unauthorized terminal access.
* **Execution Bypass:** Automatically trusts the server `CONSOLE` and designated administrator accounts while keeping regular players or staff restricted.

## 🛡️ Security Validation Profile

The built-in `UserExecutionControl` layer blocks unauthorized access to the underlying host system by blacklisting specific keywords and runtime components:
* `Runtime.getRuntime().exec`
* `ProcessBuilder`
* `java.lang.Process`
* Unix shells (`/bin/sh`, `/bin/bash`)
* Windows shells (`cmd.exe`, `powershell`)
* System utilities (`wmic`, `dmidecode`)

---

## 🚀 Installation & Setup

1. **Prerequisites:** Ensure your server environment runs **OpenJDK 21+** (or the version corresponding to your platform target).
2. Download or compile the `JavaFixerAddon.jar` and place it into your server's `/plugins/` directory.
3. Open `UserExecutionControl.java` and adjust your administrator account name:
   ```java
   if (sender.getName().equals("YourMinecraftName")) {
       return true;
   }