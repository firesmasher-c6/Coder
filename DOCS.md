# Coder API Documentation

Reference for addon developers. Access the API via `CoderAPI.getInstance()` — typically stored as `api` in your addon.

---

## Getting the API

```java
CoderAPI api = CoderAPI.getInstance();
```

---

## Messaging

Send formatted messages to a `CommandSender` (player or console).

| Method | Description |
|---|---|
| `api.sendMessage(sender, message)` | Send a plain prefixed message |
| `api.sendSuccess(sender, message)` | Send a green ✓ success message |
| `api.sendError(sender, message)` | Send a red ✗ error message |
| `api.sendInfo(sender, message)` | Send a blue ℹ info message |
| `api.sendWarning(sender, message)` | Send a yellow ⚠ warning message |
| `api.sendDebug(sender, message)` | Send a gray `[DEBUG]` message |
| `api.sendRaw(sender, message)` | Send a raw message with no prefix |

### Broadcast

| Method | Description |
|---|---|
| `api.broadcastMessage(message)` | Broadcast a prefixed message to all players + console |
| `api.broadcast(message)` | Alias for `broadcastMessage` |
| `api.broadcastSuccess(message)` | Broadcast a green ✓ success message |
| `api.broadcastWarning(message)` | Broadcast a yellow ⚠ warning message |
| `api.broadcastError(message)` | Broadcast a red ✗ error message |

---

## Logging

Write messages to the server log.

| Method | Description |
|---|---|
| `api.log(message)` | Log an info message |
| `api.logWarning(message)` | Log a warning message |
| `api.logError(message)` | Log a severe/error message |
| `api.logDebug(message)` | Log a debug info message |

---

## Players

| Method | Description |
|---|---|
| `api.isPlayer(sender)` | Check if a `CommandSender` is a `Player` |
| `api.getPlayer(name)` | Get an online player by name |
| `api.getPlayerByUUID(uuid)` | Get an online player by UUID |
| `api.getOnlinePlayers()` | Get all online players as an array |
| `api.getOnlinePlayerCount()` | Get number of online players |
| `api.teleportPlayer(player, location)` | Teleport a player to a location |
| `api.damagePlayer(player, damage)` | Deal damage to a player |
| `api.healPlayer(player, amount)` | Heal a player by an amount (capped at max health) |
| `api.setPlayerHealth(player, health)` | Set a player's health directly |
| `api.getPlayerHealth(player)` | Get a player's current health |

---

## Worlds

| Method | Description |
|---|---|
| `api.getWorld(name)` | Get a world by name |
| `api.getWorlds()` | Get all loaded worlds as an array |
| `api.getWorldNames()` | Get all world names as a `String[]` |

---

## Command Execution

Run commands programmatically.

| Method | Description |
|---|---|
| `api.executeCommand(command)` | Execute a command as console |
| `api.executeCommandAsPlayer(player, command)` | Execute a command as a player |
| `api.executeCommandAsConsole(command)` | Alias for `executeCommand` |

---

## Server Info

| Method | Description |
|---|---|
| `api.getServerMotd()` | Get the server MOTD |
| `api.getBukkitVersion()` | Get the Bukkit version string |
| `api.getMinecraftVersion()` | Get the Minecraft version string |
| `api.getMaxPlayers()` | Get the max player slot count |
| `api.isServerRunning()` | Check if the server is running (not stopping) |
| `api.getServerTicks()` | Get current server tick count |

---

## Plugins

| Method | Description |
|---|---|
| `api.getPlugin(name)` | Get a plugin by name |
| `api.getPlugins()` | Get all loaded plugins |
| `api.isPluginEnabled(name)` | Check if a plugin is enabled |

---

## Registering Commands

Addons can register custom `/coder` subcommands.

| Method | Description |
|---|---|
| `api.registerCoderCommand(subcommand, handler)` | Register a `/coder <subcommand>` handler |
| `api.unregisterCoderCommand(subcommand)` | Unregister a subcommand |
| `api.getRegisteredCoderCommands()` | Get all registered subcommand names |
| `api.getCoderCommand(subcommand)` | Get the handler for a subcommand |

### CoderCommandHandler Interface

```java
public interface CoderCommandHandler {
    boolean execute(CommandSender sender, String[] args);
    String getCommandName();
    String getDescription();
    String getUsage();
    default String getPermission();                                       // default: "coder.admin"
    default List<String> getTabCompletions(CommandSender sender, String[] args);
}
```

**Example:**

```java
api.registerCoderCommand("hello", new CoderCommandHandler() {
    public boolean execute(CommandSender sender, String[] args) {
        api.sendSuccess(sender, "Hello from my addon!");
        return true;
    }
    public String getCommandName() { return "hello"; }
    public String getDescription() { return "Says hello"; }
    public String getUsage() { return "/coder hello"; }
});
```

---

## Execution Handlers

Override the built-in Java, Python, or Lua execution engines.

| Method | Description |
|---|---|
| `api.registerJavaHandler(handler)` | Override the Java execution handler |
| `api.registerPythonHandler(handler)` | Override the Python execution handler |
| `api.registerLuaHandler(handler)` | Override the Lua execution handler |
| `api.getJavaHandler()` | Get the current Java handler |
| `api.getPythonHandler()` | Get the current Python handler |
| `api.getLuaHandler()` | Get the current Lua handler |

### JavaExecutionHandler Interface

```java
public interface JavaExecutionHandler {
    boolean compileAndExecute(File javaFile, CommandSender executor);
    boolean compileAndLoad(File javaFile, CommandSender executor);
    boolean loadClass(String className, CommandSender executor);
    void unloadClass(String className, CommandSender executor);
    void listLoadedClasses(CommandSender executor);
    boolean isClassLoaded(String className);
    Class<?> getLoadedClass(String className);
    void clearCache();
    String getHandlerName();
}
```

### ScriptExecutionHandler Interface

Used for both Python and Lua handlers.

```java
public interface ScriptExecutionHandler {
    boolean execute(File scriptFile, CommandSender executor);
    boolean executeString(String scriptContent, CommandSender executor);
    String getLanguage();
    String getHandlerName();
    void initialize();
    void cleanup();
    boolean isAvailable();
    String getVersion();
}
```

---

## Script Pre/Postprocessors

Hook into the script pipeline to transform content before execution or handle results after.

| Method | Description |
|---|---|
| `api.registerPreprocessor(language, preprocessor)` | Register a preprocessor for a language (`"java"`, `"python"`, `"lua"`) |
| `api.registerPostprocessor(language, postprocessor)` | Register a postprocessor for a language |
| `api.getPreprocessor(language)` | Get the current preprocessor for a language |
| `api.getPostprocessor(language)` | Get the current postprocessor for a language |

### ScriptPreprocessor Interface

```java
public interface ScriptPreprocessor {
    String process(String scriptName, String content); // return null to cancel execution
    String getName();
    default boolean shouldProcess(String scriptName);
}
```

### ScriptPostprocessor Interface

```java
public interface ScriptPostprocessor {
    void processResult(String scriptName, CommandSender executor, boolean success, String output, Throwable error);
    String getName();
    default boolean shouldProcess(String scriptName);
}
```

---

## Event Listeners

Listen to Coder plugin events such as script execution and addon lifecycle events.

| Method | Description |
|---|---|
| `api.registerEventListener(listener)` | Register a `CoderEventListener` |
| `api.unregisterEventListener(listener)` | Unregister a listener |
| `api.getEventListeners()` | Get all registered event listeners |

### CoderEventListener Interface

```java
public interface CoderEventListener {
    String getName();
    default void onScriptStart(String scriptName, String language);
    default void onScriptEnd(String scriptName, String language, boolean success);
    default void onScriptError(String scriptName, String language, Throwable error);
    default void onAddonLoad(String addonName);
    default void onAddonUnload(String addonName);
    default void onCommandExecute(String subcommand, String[] args);
}
```

---

## Tab Completers

Provide custom tab completion for the `/coder` command.

| Method | Description |
|---|---|
| `api.registerTabCompleter(completer)` | Register a `CoderTabCompleter` |
| `api.getTabCompleters()` | Get all registered tab completers |

### CoderTabCompleter Interface

```java
public interface CoderTabCompleter {
    List<String> getCompletions(CommandSender sender, String[] args);
    String getName();
    default boolean applies(CommandSender sender, String[] args);
}
```

---

## CoderAddon Interface

All addons implement `CoderAddon`. The required methods are:

```java
String getName();
String getVersion();
String getAuthor();
void onEnable();
void onDisable();
```

Notable optional (default) methods your addon can override:

| Method | Description |
|---|---|
| `onReload()` | Called on reload (default: disable then enable) |
| `onAllAddonsLoaded()` | Called after all addons have loaded |
| `onBeforeDisable()` | Called just before `onDisable` |
| `onAddonLoaded(addonName)` | Called when another addon loads |
| `onAddonUnloaded(addonName)` | Called when another addon unloads |
| `onTick()` | Called each server tick |
| `onSave()` | Called when the addon should save data |
| `onLoad()` | Called when data should be loaded |
| `getCustomJavaHandler()` | Return a custom `JavaExecutionHandler` |
| `getCustomPythonHandler()` | Return a custom `ScriptExecutionHandler` for Python |
| `getCustomLuaHandler()` | Return a custom `ScriptExecutionHandler` for Lua |
| `getScriptPreprocessor(language)` | Return a `ScriptPreprocessor` for a language |
| `getScriptPostprocessor(language)` | Return a `ScriptPostprocessor` for a language |
| `getEventListener()` | Return a `CoderEventListener` |
| `getTabCompleter()` | Return a `CoderTabCompleter` |
| `registerCustomCommands(api)` | Register commands on enable |
| `unregisterCustomCommands(api)` | Unregister commands on disable |
| `getScriptGlobals()` | Provide global variables injected into scripts |
| `getAddonPermissions()` | Declare permissions used by this addon |
| `getConfigDefaults()` | Provide default config values |
| `getDependencies()` | Declare addon dependencies |
| `getPriority()` | Load priority (default: 50, higher = earlier) |
| `getAPIVersion()` | Target API version (default: 2) |
| `hasFeature(featureName)` | Declare optional feature support |
| `getFeature(featureName)` | Return a feature object by name |
| `getMetadata()` | Return a map of addon metadata |