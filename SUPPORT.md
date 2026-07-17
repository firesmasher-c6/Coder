# Coder User Guide

> **Welcome to the Coder User Guide. This document explains how the scripting engine executes different languages on your server, file requirements, and how to integrate the Coder API into your development environment.**

---

## 📂 Supported Languages & File Extensions

Coder supports three major programming languages. Scripts must use their respective standard file extensions to be recognized and executed by the plugin:

| Language | File Extension | Execution Engine |
| :--- | :--- | :--- |
| **Java** | `.java` | Native JVM Compiler |
| **Python** | `.py` | Jython Interpreter |
| **Lua** | `.lua` | LuaJ Interpreter |

---

## ⚙️ How Does Coder Work?

Since Minecraft runs on the **Java Virtual Machine (JVM)**, Coder utilizes specialized integration engines to bridge different scripting environments directly into the server's runtime memory.

### ☕ Native Java Execution
Unlike Python and Lua which run through interpreters, Java code is compiled natively. 
* **Hardware-Dependent:** The Java features available to your scripts depend entirely on the Java Development Kit (JDK) version running your Minecraft server. 
* **Example:** If your server is running on **Java 25**, your scripts can fully utilize Java 25 features, APIs, and syntax.

> ⚠️ **CRITICAL REQUIREMENT: JDK vs. JRE**
> You **must** run your Minecraft server using a **JDK (Java Development Kit)**, *not* a JRE (Java Runtime Environment). 
> Because Coder compiles `.java` source files on the fly directly on your server, it requires the Java Compiler (`javac`) tools—which are only packaged inside the JDK. Running on a JRE will cause Java script execution to fail.

### 🐍 Python Execution
Python scripts are executed using **Jython**, which compiles Python code directly into Java bytecode, allowing seamless integration with server resources and Bukkit APIs.

### 🌙 Lua Execution
Lua scripts are executed using **LuaJ**, a lightweight, high-performance Lua interpreter written in Java designed specifically for JVM environments.

---

## ⚠️ Language Compatibility & Limits

Because Coder relies on JVM-based interpreters for Python and Lua, there are strict version limitations you must keep in mind when writing scripts:

> ### 🚫 Python Limitations (Jython)
> * **Supported Version:** Python **2.7**
> * **Note:** You **cannot** run Python **3.x** code. Modern Python 3 syntaxes (such as `f-strings` or newer library modules) will result in syntax errors.

> ### 🚫 Lua Limitations (LuaJ)
> * **Supported Version:** Lua **5.3**
> * **Note:** You **cannot** run Lua **5.4 or newer** features. Keep your script syntax compliant with the Lua 5.3 specification.

---

## 📦 Developer API & Dependency Setup

To write scripts with full IDE autocomplete, or if you are developing a companion plugin, you can reference the Coder API (which includes utilities like `api.log` and custom event hooks).

> 📢 **Coming Soon:** The Coder API will soon be hosted directly on **Maven Central** for seamless remote importing.

Currently, you must reference the compiled `Coder-2.3.2.jar` as a local dependency in your build tool.

### 🔹 Maven (`pom.xml`)
Add the local jar file using a `system` scope dependency:

```xml
<dependency>
    <groupId>me.coder</groupId>
    <artifactId>coder</artifactId>
    <version>2.3.2</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/libs/Coder-2.3.2.jar</systemPath>
</dependency>
```

### 🔹 Gradle (`build.gradle`)
Place the JAR file in a libs directory at your project root and add:

```groovy
dependencies {
    compileOnly files('libs/Coder-2.3.2.jar')
}
```

### 🔹 Gradle Kotlin DSL (build.gradle.kts)
Place the JAR file in a libs directory at your project root and add:

```kotlin
dependencies {
    compileOnly(files("libs/Coder-2.3.2.jar"))
}
```
