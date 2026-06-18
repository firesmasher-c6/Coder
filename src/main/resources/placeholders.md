# CodeDSL Placeholders

Dynamic placeholders that display server and game information in your scripts.

## In-Game Placeholders

### Server Information
```
{placeholder(time)}         - Current in-game time
{placeholder(online)}       - Number of online players
{placeholder(online_max)}   - Server's maximum players
{placeholder(worldonline)}  - Online players in current world
{placeholder(server_uptime)} - Server's uptime
{placeholder(mspt)}         - Server's current MSPT (milliseconds per tick)
{placeholder(tps)}          - Server's current TPS (ticks per second)
{placeholder(plugins)}      - All loaded server plugins
```

### Variable Placeholders
```
{placeholder(var_<name>)}    - Display variable from variables.storage
{placeholder(obfvar_<name>)} - Display obfuscated variable from variables.obf
```

## Usage Rules

### Where Placeholders Can Be Used:
✅ **Allowed in:**
- `command` sections (in `trigger` or `run {}` blocks)
- `every` loops
- `wait` statements
- `delay` statements
- `broadcast` messages
- `console.log` output
- `send` messages

❌ **NOT allowed in:**
- Variable assignments (`var name = {placeholder(...)}` won't work)
- File operations
- Conditional statements

## Examples

### Example 1: Display Server TPS
```cd
command /tps:
    permission: op
    trigger:
        send "Server TPS: {placeholder(tps)}"
```

### Example 2: Display Online Players
```cd
command /players:
    trigger:
        broadcast "Players online: {placeholder(online)} / {placeholder(online_max)}"
```

### Example 3: Using with Variables
```cd
var playerCount = 10

command /info:
    trigger:
        send "My custom count: {var(playerCount)}"
        send "Actual online: {placeholder(online)}"
```

### Example 4: Every Loop with Placeholder
```cd
every 30s {
    broadcast "TPS: {placeholder(tps)} | Online: {placeholder(online)}"
}
```

### Example 5: Console Logging Server Info
```cd
console.log {
    Server Status:
    TPS: {placeholder(tps)}
    MSPT: {placeholder(mspt)}
    Players: {placeholder(online)} / {placeholder(online_max)}
    Uptime: {placeholder(server_uptime)}
}
```

### Example 6: Using Custom Variables
```cd
var admin_name = DARRELMT

command /admininfo:
    permission: op
    trigger:
        send "Admin: {var(admin_name)} | Current TPS: {placeholder(tps)}"
```

## Placeholder Details

### {placeholder(time)}
Returns the current in-game time in Minecraft format (0-24000 ticks).

### {placeholder(online)}
Returns the count of online players on the server.

### {placeholder(online_max)}
Returns the maximum allowed players on the server.

### {placeholder(worldonline)}
Returns the count of players in the world the command sender is in.

### {placeholder(server_uptime)}
Returns how long the server has been running (format: HH:MM:SS or days).

### {placeholder(mspt)}
Returns milliseconds per tick (lower is better, should be <50ms).

### {placeholder(tps)}
Returns server TPS (ticks per second). Green = 20 TPS, Yellow = 15-19 TPS, Red = <15 TPS.

### {placeholder(plugins)}
Returns comma-separated list of all loaded plugins.

### {placeholder(var_<name>)}
Displays a variable stored in `variables.storage`.
- Example: `{placeholder(var_myvar)}` displays the value of variable `myvar`

### {placeholder(obfvar_<name>)}
Displays an obfuscated variable decoded from `variables.obf`.
- Example: `{placeholder(obfvar_apikey)}` displays the decoded value of obfuscated variable `apikey`

## Advanced Examples

### Multi-line Display
```cd
console.log {
    === Server Status ===
    Time: {placeholder(time)}
    TPS: {placeholder(tps)}
    MSPT: {placeholder(mspt)}
    Online: {placeholder(online)}/{placeholder(online_max)}
    Uptime: {placeholder(server_uptime)}
    ====================
}
```

### Conditional with Placeholders
```cd
command /checkserver:
    permission: op
    trigger:
        send "Current server info:"
        send "TPS: {placeholder(tps)}"
        send "Online: {placeholder(online)}"
        send "World players: {placeholder(worldonline)}"
```

### Repeating Server Status
```cd
every 1m {
    broadcast "Server TPS: {placeholder(tps)} | Players: {placeholder(online)}/{placeholder(online_max)}"
}
```

## Troubleshooting

**Placeholder shows as `{placeholder(...)}` instead of value:**
- Ensure you're using it in an allowed section (trigger, broadcast, console.log, send, etc.)
- Check placeholder name spelling
- Make sure variable names match exactly (case-sensitive)

**Variable placeholder returns nothing:**
- Ensure the variable was created with `var name = value`
- Check the variable exists in `variables.storage`
- Verify the name in `{placeholder(var_name)}` matches exactly

---

**Version:** 1.3.4  
**Last Updated:** 2026