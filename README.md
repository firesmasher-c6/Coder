# CodeDSL v1.3.4

A Domain Specific Language (DSL) addon for the **Coder** Minecraft plugin that allows you to write scripts in an easy-to-learn custom syntax.

## Features

✨ **Easy-to-Learn Syntax** - Write scripts without Java knowledge  
⚡ **Fast Execution** - Optimized for Minecraft servers  
🔒 **Secure Variables** - Store secrets with obfuscated variables  
🔄 **Async Support** - Run tasks on separate threads  
📁 **File Operations** - Read/write files from scripts  
🎮 **Bukkit Integration** - Full access to Bukkit API  
📡 **Inter-Script Communication** - Scripts can talk to each other  
⏰ **Task Scheduling** - Delayed and repeating tasks  

## Installation

1. Place `CodeDSL-1.3.4.jar` in your `plugins/` folder

2. Server automatically creates:
```
plugins/Coder/CodeDSL/
├── scripts/          (Your .cd scripts)
├── examples/         (Example scripts)
├── variables/
│   ├── variables.storage      (Plain text variables)
│   └── variables.obf          (Obfuscated variables)
└── config.yml
```

## Quick Start

### 1. Create a Script

Create `plugins/Coder/CodeDSL/scripts/hello.cd`:

```code
broadcast "Hello, World!"
send "Welcome!" to player_name
console.log "Script executed!"
```

### 2. Run the Script

```
/codedsl run hello.cd
```

Or use Coder commands:

```
/coder run hello.cd
```

## Command Syntax

### Broadcast
Send message to all players:
```code
broadcast "This message goes to everyone!"
```

### Send Message
Send to specific player (outside triggers):
```code
send "Hello!" to DARRELMT
```

### Console Log
Log to server console:
```code
console.log "Server log message"
console.log {
    Multi-line
    Console log
    Multiple lines
}
```

### Variables
Store and use variables:
```code
var playerCount = 42
var serverName = "MyAwesomeServer"

broadcast "Server {serverName} has {playerCount} players!"
```

### Obfuscated Variables (Secrets)
Store passwords and API keys safely:
```code
obfuscatedVAR apiKey = "secret-api-key-12345"
obfuscatedVAR password = "super-secret-password"

console.log "Secrets stored securely!"
```

### File Operations

**Read file:**
```code
fileRead /path/to/file.txt
```

**Write file:**
```code
fileWrite /path/to/output.txt {
    This content will be written to the file
    Multiple lines supported
}
```

**Delete file:**
```code
fileDel /path/to/file.txt
```

### Execute Commands

Execute in console:
```code
execute command "broadcast Hello!" in console
```

### Delays and Waits

**Using ticks:**
```code
delay 20          # 20 ticks = 1 second
broadcast "After delay!"
```

**Using time units:**
```code
wait 1s           # 1 second
wait 2m           # 2 minutes
wait 5t           # 5 ticks
```

### Repeating Tasks

Run code every X time:
```code
every 5s {
    broadcast "Running every 5 seconds!"
}

every 1m {
    console.log "1 minute has passed!"
}
```

### If/Else Statements

```code
if someValue is "expected":
    broadcast "Value matched!"
else if someValue is "other":
    broadcast "Other value!"
else:
    broadcast "No match!"
```

### Import Libraries

**Async - Run tasks on separate threads:**
```code
import async

async:
    broadcast "This runs async!"
    wait 2s
    broadcast "After async delay!"
```

**Connection - Link multiple scripts:**
```code
import Connection

connectToFile "other-script.cd":
    if file is connected:
        broadcast "Connected to other script!"
        fileRead /plugins/Coder/CodeDSL/scripts/other-script.cd
```

**Bukkit - Full Bukkit API:**
```code
import bukkit

execute command "say Hello from Bukkit!" in console
```

## Command Definitions

Define custom Minecraft commands in your scripts:

```code
command /greet [<text>]:
    permission: op
    trigger:
        if arg-1 is "admin":
            broadcast "Admin greeted!"
            send "You are an admin!"
        else:
            broadcast "User greeted!"
```

## Full Commands Reference

### Plugin Commands
```
/codedsl run <filename>       - Execute a CodeDSL script
/codedsl reload <filename>    - Reload a script
/codedsl load <filename>      - Load script to memory
/codedsl unload <filename>    - Unload script from memory
/codedsl list                 - List all available scripts

/cdsl <same as above>         - Alias
/code-dsl <same as above>     - Alias
```

### Coder Integration
```
/coder run <filename>         - Run through Coder plugin
/coder reload <filename>      - Reload through Coder
/coder load <filename>        - Load through Coder
/coder unload <filename>      - Unload through Coder
```

## Variables Storage

**Regular variables** - stored in plain text:
```
plugins/Coder/CodeDSL/variables/variables.storage
```

**Obfuscated variables** - Base64 encoded for secrets:
```
plugins/Coder/CodeDSL/variables/variables.obf
```

## Configuration

Edit `plugins/Coder/CodeDSL/config.yml`:

```yaml
# Enable CodeDSL
enabled: true

# File extensions
file-extensions:
  main: ".cd"
  legacy: ".code"

# Allow reloading
reloading-enabled: true

# Script settings
scripts:
  auto-load: false
  timeout: 30

# Variable persistence
variables:
  persistence-enabled: true
  auto-save-interval: 60
```

## Examples

Check `plugins/Coder/CodeDSL/examples/EXAMPLES.cd` for 10 complete examples:
- Hello World
- Variables
- Command Definitions
- Async Execution
- File Operations
- Secrets
- Repeating Tasks
- Inter-Script Communication
- Console Logging
- Delayed Execution

## Permissions

```
codedsl.use      - Use CodeDSL commands
codedsl.command  - Execute custom commands
```

## Troubleshooting

**Script not found:**
- Ensure file is in `plugins/Coder/CodeDSL/scripts/`
- Check file extension is `.cd` or `.code`

**Variables not saving:**
- Ensure `variables.storage` exists in `variables/` folder
- Check file permissions

**Commands not working:**
- Ensure Coder plugin is installed and enabled
- Check player has `codedsl.use` permission

**Async not working:**
- Ensure `import async` is at the top of script
- Check server console for errors

## License

Made by **Firesmasher** for the Coder plugin community.
Licensed under **MIT**

## Credits

- Built as addon for **Coder v1.4.2**
- Uses **Paper/Bukkit API**

---

**Version:** 1.3.4  
**Last Updated:** 2026
**Status:** Active Development