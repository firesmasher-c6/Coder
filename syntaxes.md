# CodeDSL Syntaxes Guide v1.8.2

Complete reference for all CodeDSL syntax rules and usage.

## Variable Management

### Store a Plain Variable
```cd
var name = value
var playerName = DARRELMT
var count = 42
var message = Hello World
```

**Rules:**
- Variable name must be alphanumeric and start with letter
- Value can be anything (string, number, etc.)
- Variables are stored in `variables.storage`

**Usage in strings:**
```cd
broadcast "{var(playerName)}"                 # Outputs: DARRELMT
send "{var(message)}" to {var(playerName)}    # Sends message to player
console.log "{var(count)}"                     # Logs: 42
```

### Store Obfuscated Variable (Secrets)
```cd
obfuscatedVAR apiKey = secret-api-key-12345
obfuscatedVAR password = super-secret
obfuscatedVAR token = bearer-token-xyz
```

**Rules:**
- Same naming rules as plain variables
- Values are Base64 encoded for protection
- Stored in `variables.obf`
- Perfect for API keys, passwords, tokens

**Usage in strings:**
```cd
broadcast "{obfvar(apiKey)}"                  # Auto-decodes when used
send "{obfvar(password)}" to admin            # Sends decoded value
console.log "{obfvar(token)}"                 # Logs decoded token
```

### Read Variable from Storage
```cd
varRead "playerName":
    console.log "Player: {var(playerName)}"
```

### Delete Variable
```cd
deleteVar "playerName"          # Delete plain variable
deleteObfVar "apiKey"           # Delete obfuscated variable
```

---

## Message Broadcasting

### Broadcast to All Players
```cd
broadcast "Hello, World!"
broadcast "Server message here"
broadcast "{var(serverName)} is running!"
```

### Send Message to Specific Player
```cd
send "Hello!" to DARRELMT
send "You have {var(coins)} coins!" to player
send "{obfvar(welcomeMessage)}" to admin
```

**Note:** Can only be used in trigger sections for offline send

---

## Console Logging

### Single Line Log
```cd
console.log "Server started!"
console.log "{var(message)}"
console.log "Player count: {var(count)}"
```

### Multi-Line Log Block
```cd
console.log {
    Server is running
    Version: 1.21
    Players online: 5
}
```

---

## Conditional Logic

### If Statement
```cd
if var "playerName" = DARRELMT:
    broadcast "Admin detected!"
    console.log "Admin login"
else if var "playerName" = moderator:
    broadcast "Moderator online"
else:
    broadcast "Regular player"
```

**Rules:**
- Check format: `if var "name" = value:`
- Supports `else if` and `else`
- Must end with colon `:`
- Exact string matching (case-sensitive)

---

## File Operations

### Read File
```cd
fileRead /path/to/file.txt
fileRead plugins/Coder/CodeDSL/scripts/data.txt
```

**Output:** Prints each line to console

### Write File
```cd
fileWrite /path/to/output.txt {
    Line 1 of content
    Line 2 of content
    # Comments with #
}
```

**Rules:**
- Path can be absolute or relative
- Creates directories automatically
- Overwrites existing files
- Supports multi-line content

### Delete File
```cd
fileDel /path/to/file.txt
```

---

## Command Execution

### Execute in Console
```cd
execute command "broadcast Hello!" in console
execute command "say Server message" in console
```

**Rules:**
- Can be used anywhere in script
- Executes with console permissions
- Command is inside quotes

### Execute as Player
```cd
command /test:
    trigger:
        execute command "say I am executing" in player
```

**Rules:**
- Only in command trigger sections
- Executes with player's permissions

---

## Delays and Timing

### Wait (Using Time Units)
```cd
wait 1s     # Wait 1 second
wait 5m     # Wait 5 minutes  
wait 10t    # Wait 10 ticks
```

**Supported units:**
- `s` = seconds
- `m` = minutes
- `t` = ticks (1 tick = 0.05 seconds)

**Note:** Can only be used in trigger/run sections

### Delay (Using Ticks)
```cd
delay 20            # Delay 20 ticks (1 second)
delay 100           # Delay 100 ticks (5 seconds)
broadcast "Later!"
```

---

## Repeating Tasks

### Every (Interval Loop)
```cd
every 5s {
    broadcast "This repeats every 5 seconds!"
}

every 1m {
    console.log "1 minute passed"
}

every 10t {
    # Execute every 10 ticks
}
```

**Rules:**
- Can be outside command sections
- Runs indefinitely
- Syntax: `every <number><unit> { content }`

---

## Command Definitions

### Define Custom Commands
```cd
command /greet [<text>]:
    permission: op
    trigger:
        if arg-1 is "admin":
            broadcast "Admin greeting!"
            send "Welcome, admin!" to player
        else:
            broadcast "User greeting!"
```

**Rules:**
- Command name must start with `/`
- Optional arguments: `[<text>]` or `[<number>]`
- Set permission requirement
- Trigger: code that runs when command executed
- `arg-1` = first argument (case-sensitive)

---

## Imports and Libraries

### Import Async
```cd
import async

async:
    broadcast "Running async!"
    wait 2s
    broadcast "After async delay"
```

**Rules:**
- Runs on separate thread
- Can use `wait` inside
- Won't block main server thread

### Import Connection
```cd
import Connection

connectToFile "other-script.cd":
    if file is connected:
        fileRead plugins/Coder/CodeDSL/scripts/other-script.cd
```

**Rules:**
- Only works with scripts in `scripts/` folder
- Allows inter-script communication
- Check `if file is connected:` before using

### Import Bukkit
```cd
import bukkit

execute command "op DARRELMT" in console
```

**Rules:**
- Access full Bukkit API
- Use for complex operations

---

## Code Blocks: run {} vs trigger

### run {} Block (Standalone)
```cd
run {
    broadcast "This runs immediately!"
    wait 1s
    broadcast "After 1 second"
}
```

### trigger (In Commands Only)
```cd
command /test:
    trigger:
        broadcast "Command triggered!"
        send "Hello!" to player
```

---

## Comments

```cd
# This is a comment
# Comments start with #

broadcast "Hello"  # Inline comments also work

# Comments are ignored during execution
```

---

## Variable Placeholder Syntax

### In Strings
```cd
{var(name)}        # Plain variable placeholder
{obfvar(name)}     # Obfuscated variable placeholder
```

### Full Examples
```cd
var serverName = MyServer
var playerCount = 42

broadcast "Welcome to {var(serverName)}!"
console.log "Players online: {var(playerCount)}"
```

---

## Complete Example Script

```cd
# MyScript.cd - Full example

# Store variables
var welcomeMessage = Welcome to our server!
var adminName = DARRELMT
obfuscatedVAR secretKey = my-secret-api-key

# Broadcast welcome
broadcast "{var(welcomeMessage)}"

# Log to console
console.log "Server started successfully"

# Define admin command
command /admin [<text>]:
    permission: admin
    trigger:
        if arg-1 is "status":
            send "Server is running!" to player
        else:
            send "Unknown admin command" to player

# Repeating task
every 5m {
    broadcast "Server check: {var(playerCount)} online"
}

# Read configuration file
fileRead plugins/Coder/CodeDSL/scripts/config.txt
```

---

## Error Handling

**Common Errors:**

| Error | Cause | Fix |
|-------|-------|-----|
| Variable not found | Variable doesn't exist | Use `varRead` first or create with `var` |
| File not found | Path is wrong | Check file path exists |
| Player not found | Player offline | Add online player check |
| Invalid syntax | Wrong syntax format | Check syntax guide |
| Permission denied | Player lacks permission | Set correct permission |

---

## Best Practices

1. **Use comments** - Explain complex logic
2. **Organize variables** - Group related variables together
3. **Use obfuscated variables** for secrets - Never store passwords plainly
4. **Test async blocks** - Test threading carefully
5. **Handle errors** - Check file/player exists before use
6. **Use meaningful names** - Variable names should be descriptive

---

**Version:** 1.8.2
**Last Updated:** 2026