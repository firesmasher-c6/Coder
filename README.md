# CodeDSL v2.3.1
A Skript-like DSL for the Coder plugin ecosystem.

## Features
* **Custom Syntax**: Write `.cd` files with familiar `command /...` and `trigger:` syntax.
* **Dynamic Loading**: Hot-reload scripts using `/codedsl configreload.
* **Command Interception**: Redirects `/coder run <script>` to your DSL processor.
* **File Management**: Auto-generates `examples/` and `variables/` directories upon first boot.
* **Auto-Download/Update**: Auto Downloads the latest version of CodeDSL after `/codedsl update` and `/codedsl confirm`.

## Installation
1. Drop `CodeDSL.jar` into your `plugins/` folder.
2. Ensure the `Coder` core plugin is installed.
3. Restart server to generate default configurations.

## Configuration
Customize your script extensions in `config.yml`:
```yaml
file-extensions:
  main: ".cd"
  legacy: ".code"
  old: ".cdsl"
  custom: "YOUR_PREFERRED_FILE_EXTENSION_HERE"
scripts:
  auto-load: true
