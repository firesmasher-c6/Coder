# Coder User Guide

This guide explains how to use the three scripting types supported by Coder and how to manage them via commands.

## 1. Custom Syntax (.script & .code)
Coder uses a custom parser for "Modern Skript."
* **`.script` files:** Contain your actual commands and triggers.
* **`.code` files:** Used specifically to store reusable variables.

### Writing a Script
Use the following keywords defined in Coder:
* `cmd`: Define a command.
* `trigger`: The entry point for your command.
* `print("")`: Broadcasts a message to the server.
* `perm`: Set required permissions (e.g., `perm: op`).
* `import`: Load variables from a `.code` file (e.g., `import hello.code`).

**Example (`kisert.script`):**
```text
import hello.code 

cmd /jello:
    perm: op
    trigger:
        print(cmd1) # Prints variable from hello.code
        print("Hello, {player}!")
