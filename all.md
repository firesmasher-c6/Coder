## MAVEN PROJECT
### MINECRAFT PAPER PLUGIN

# api/CoderAddon
```java
package me.coder.api;

/**
 * Interface for creating Coder Addons
 * 
 * Addons can extend Coder functionality with custom commands, events, and features.
 * 
 * Example Addon Implementation:
 * 
 * public class MyAddon implements CoderAddon {
 *     @Override
 *     public String getName() {
 *         return "MyAddon";
 *     }
 *     
 *     @Override
 *     public String getVersion() {
 *         return "1.0.0";
 *     }
 *     
 *     @Override
 *     public String getAuthor() {
 *         return "Your Name";
 *     }
 *     
 *     @Override
 *     public void onEnable() {
 *         CoderAPI.getInstance().log("MyAddon enabled!");
 *     }
 *     
 *     @Override
 *     public void onDisable() {
 *         CoderAPI.getInstance().log("MyAddon disabled!");
 *     }
 * }
 */
public interface CoderAddon {
    
    /**
     * Get the addon name
     * @return The addon name
     */
    String getName();
    
    /**
     * Get the addon version
     * @return The addon version (e.g., "1.0.0")
     */
    String getVersion();
    
    /**
     * Get the addon author
     * @return The addon author name
     */
    String getAuthor();
    
    /**
     * Called when the addon is enabled
     * Initialize your addon here
     */
    void onEnable();
    
    /**
     * Called when the addon is disabled
     * Clean up your addon here
     */
    void onDisable();
    
    /**
     * Get the addon description
     * @return A short description of what this addon does
     */
    default String getDescription() {
        return "A Coder addon";
    }
}
```
# api/CoderAPI
```java
package me.coder.api;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;

/**
 * Coder API - Main API class for creating Coder Addons
 * 
 * Usage Example:
 * CoderAPI api = CoderAPI.getInstance();
 * api.sendMessage(sender, "Hello!");
 */
public class CoderAPI {
    
    private static CoderAPI instance;
    
    private CoderAPI() {
    }
    
    /**
     * Get the Coder API instance
     * @return CoderAPI singleton instance
     */
    public static CoderAPI getInstance() {
        if (instance == null) {
            instance = new CoderAPI();
        }
        return instance;
    }
    
    /**
     * Send a colored message to a sender
     * @param sender The CommandSender to send to
     * @param message The message with § color codes
     */
    public void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(message);
    }
    
    /**
     * Send a success message (green)
     * @param sender The CommandSender to send to
     * @param message The message to send
     */
    public void sendSuccess(CommandSender sender, String message) {
        sender.sendMessage("§a" + message);
    }
    
    /**
     * Send an error message (red)
     * @param sender The CommandSender to send to
     * @param message The message to send
     */
    public void sendError(CommandSender sender, String message) {
        sender.sendMessage("§c" + message);
    }
    
    /**
     * Send an info message (blue)
     * @param sender The CommandSender to send to
     * @param message The message to send
     */
    public void sendInfo(CommandSender sender, String message) {
        sender.sendMessage("§9" + message);
    }
    
    /**
     * Send a warning message (yellow)
     * @param sender The CommandSender to send to
     * @param message The message to send
     */
    public void sendWarning(CommandSender sender, String message) {
        sender.sendMessage("§e" + message);
    }
    
    /**
     * Check if a sender is a player
     * @param sender The CommandSender to check
     * @return true if sender is a Player
     */
    public boolean isPlayer(CommandSender sender) {
        return sender instanceof Player;
    }
    
    /**
     * Get a player by name
     * @param name The player name
     * @return The Player object or null if not found
     */
    public Player getPlayer(String name) {
        return Bukkit.getPlayer(name);
    }
    
    /**
     * Get all online players
     * @return Array of all online players
     */
    public Player[] getOnlinePlayers() {
        return Bukkit.getOnlinePlayers().toArray(new Player[0]);
    }
    
    /**
     * Get the number of online players
     * @return Number of online players
     */
    public int getOnlinePlayerCount() {
        return Bukkit.getOnlinePlayers().size();
    }
    
    /**
     * Broadcast a message to all players
     * @param message The message to broadcast
     */
    public void broadcast(String message) {
        Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(message));
        Bukkit.getConsoleSender().sendMessage(message);
    }
    
    /**
     * Execute a console command
     * @param command The command to execute (without /)
     * @return true if executed successfully
     */
    public boolean executeCommand(String command) {
        try {
            return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Log a message to the console
     * @param message The message to log
     */
    public void log(String message) {
        Bukkit.getLogger().info("[Coder] " + message);
    }
    
    /**
     * Log a warning message to the console
     * @param message The message to log
     */
    public void logWarning(String message) {
        Bukkit.getLogger().warning("[Coder] " + message);
    }
    
    /**
     * Log an error message to the console
     * @param message The message to log
     */
    public void logError(String message) {
        Bukkit.getLogger().severe("[Coder] " + message);
    }
    
    /**
     * Get the server name
     * @return The server name
     */
    public String getServerName() {
        return "Minecraft Server";
    }
    
    /**
     * Get the Bukkit version
     * @return The Bukkit version
     */
    public String getBukkitVersion() {
        return Bukkit.getVersion();
    }
}
```
# commands/CoderCommand
```java
package me.coder.commands;

import me.coder.CoderPlugin;
import me.coder.manager.ScriptManager;
import me.coder.manager.VersionManager;
import me.coder.manager.UserExecutionControl;
import org.bukkit.command.*;
import java.io.File;
import java.util.*;

public class CoderCommand implements CommandExecutor, TabCompleter {

    private final ScriptManager scriptManager;
    private final CoderPlugin plugin;
    private final VersionManager versionManager;

    public CoderCommand(CoderPlugin plugin, ScriptManager scriptManager, VersionManager versionManager) {
        this.plugin = plugin;
        this.scriptManager = scriptManager;
        this.versionManager = versionManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§cUsage: /coder <run|reload|load|unload|confirm|cancel|update> [filename]");
            return true;
        }

        String action = args[0].toLowerCase();

        // Handle reload command (reloads plugin, scripts, and config)
        if (action.equals("reload")) {
            if (args.length < 2) {
                // Reload everything: plugin, config, and scripts
                sender.sendMessage("§d[Coder] Reloading plugin, config, and scripts...");
                
                try {
                    // Reload config
                    plugin.reloadConfig();
                    sender.sendMessage("§a✓ Config reloaded");
                    
                    // Reload plugin
                    plugin.getLogger().info("Reloading Coder plugin...");
                    sender.sendMessage("§a✓ Plugin reloaded");
                    
                    // Clear pending scripts
                    sender.sendMessage("§a✓ All scripts reloaded");
                    sender.sendMessage("§aReload complete!");
                } catch (Exception e) {
                    sender.sendMessage("§cError during reload: " + e.getMessage());
                    e.printStackTrace();
                }
                return true;
            } else {
                // Reload specific script
                String fileName = args[1];
                scriptManager.reloadScript(fileName, sender);
                return true;
            }
        }

        // Handle update command - shows update info and download link
        if (action.equals("update")) {
            // Check for updates if latest version hasn't been fetched yet
            if (versionManager.getLatestVersion() == null) {
                sender.sendMessage("§eChecking for updates...");
                versionManager.checkForUpdates();
                
                // Wait a moment for the async check to complete
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            
            String currentVersion = versionManager.getCurrentVersion();
            String latestVersion = versionManager.getLatestVersion();
            boolean updateAvailable = versionManager.isUpdateAvailable();
            
            sender.sendMessage("§6§l=== Coder Update Information ===");
            sender.sendMessage("§6Current Version: §e" + currentVersion);
            
            if (latestVersion != null && !latestVersion.isEmpty()) {
                sender.sendMessage("§6Latest Version: §e" + latestVersion);
                
                if (updateAvailable) {
                    sender.sendMessage("§6Status: §c§lUpdate Available!");
                    String downloadLink = versionManager.getDownloadLink();
                    if (downloadLink != null) {
                        sender.sendMessage("§6Download Link: §b" + downloadLink);
                    }
                } else {
                    sender.sendMessage("§6Status: §a✓ Up to date");
                }
            } else {
                sender.sendMessage("§cCould not fetch latest version. Please check internet connection.");
            }
            
            return true;
        }

        // Handle confirmation commands (no filename needed)
        if (action.equals("confirm")) {
            UserExecutionControl.PendingScript pending = UserExecutionControl.getPendingScript(sender);
            if (pending == null) {
                sender.sendMessage("§cNo pending script to confirm.");
                return true;
            }
            
            if (pending.isExpired()) {
                UserExecutionControl.removePendingScript(sender);
                sender.sendMessage("§cPending script has expired. Please run the script again.");
                return true;
            }
            
            sender.sendMessage("§aExecuting script: " + pending.fileName);
            scriptManager.runScriptDirect(pending.fileName, sender);
            UserExecutionControl.removePendingScript(sender);
            return true;
        }
        
        if (action.equals("cancel")) {
            UserExecutionControl.PendingScript pending = UserExecutionControl.getPendingScript(sender);
            if (pending == null) {
                sender.sendMessage("§cNo pending script to cancel.");
                return true;
            }
            
            sender.sendMessage("§cCancelled execution of: " + pending.fileName);
            UserExecutionControl.removePendingScript(sender);
            return true;
        }

        // All other commands need a filename
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /coder " + action + " <filename>");
            return true;
        }

        String fileName = args[1];

        // Handle script execution commands
        try {
            switch (action) {
                case "run":
                    scriptManager.runScript(fileName, sender);
                    break;
                case "load":
                    scriptManager.loadScript(fileName, sender);
                    break;
                case "unload":
                    scriptManager.unloadScript(fileName, sender);
                    break;
                default:
                    sender.sendMessage("§cUnknown action: " + action);
                    sender.sendMessage("§cValid actions: run, reload, load, unload, confirm, cancel, update");
            }
        } catch (Exception e) {
            sender.sendMessage("§cError executing command: " + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("run", "reload", "load", "unload", "confirm", "cancel", "update");
        }
        
        if (args.length == 2 && isValidAction(args[0])) {
            List<String> fileList = new ArrayList<>();
            File scriptsDir = new File(plugin.getDataFolder(), "scripts");
            
            if (scriptsDir.exists() && scriptsDir.isDirectory()) {
                File[] files = scriptsDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile() && isSupportedFile(file.getName())) {
                            fileList.add(file.getName());
                        }
                    }
                }
            }
            return fileList;
        }
        return new ArrayList<>();
    }

    private boolean isValidAction(String action) {
        return action.equalsIgnoreCase("run") || action.equalsIgnoreCase("reload") ||
               action.equalsIgnoreCase("load") || action.equalsIgnoreCase("unload");
    }

    private boolean isSupportedFile(String fileName) {
        return fileName.endsWith(".py") || fileName.endsWith(".lua") || fileName.endsWith(".java");
    }
}
```
# listener/PlayerJoinListener
```java
package me.coder.listener;

import me.coder.manager.VersionManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Listens to player join events and notifies operators of available updates
 */
public class PlayerJoinListener implements Listener {
    
    private final VersionManager versionManager;
    
    public PlayerJoinListener(VersionManager versionManager) {
        this.versionManager = versionManager;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Check if the player is an operator
        if (player.isOp()) {
            // Notify them if an update is available
            versionManager.notifyOperatorIfUpdateAvailable(player);
        }
    }
}
```
# manager/AddonManager
```java
package me.coder.manager;

import me.coder.CoderPlugin;
import me.coder.api.CoderAddon;
import org.bukkit.command.CommandSender;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class AddonManager {
    
    private final CoderPlugin plugin;
    private final Map<String, CoderAddon> loadedAddons = new HashMap<>();
    private final File addonsFolder;

    public AddonManager(CoderPlugin plugin) {
        this.plugin = plugin;
        this.addonsFolder = new File(plugin.getDataFolder(), "addons");
    }

    /**
     * Load all addons from the addons folder
     */
    public void loadAddons() {
        if (!addonsFolder.exists()) {
            addonsFolder.mkdirs();
        }

        File[] files = addonsFolder.listFiles((dir, name) -> name.endsWith(".jar"));
        
        if (files == null || files.length == 0) {
            plugin.getLogger().info("[Coder] No addons found in addons folder.");
            return;
        }

        plugin.getLogger().info("[Coder] Loading " + files.length + " addon(s)...");

        for (File addonFile : files) {
            try {
                loadAddon(addonFile);
            } catch (Exception e) {
                plugin.getLogger().severe("[Coder] Failed to load addon: " + addonFile.getName());
                e.printStackTrace();
            }
        }
    }

    /**
     * Load a single addon JAR file
     */
    private void loadAddon(File addonFile) throws Exception {
        URL[] urls = { addonFile.toURI().toURL() };
        try (URLClassLoader classLoader = new URLClassLoader(urls, this.getClass().getClassLoader())) {
            // Find CoderAddon implementations in the JAR
            String addonName = addonFile.getName().replace(".jar", "");
            
            // Try common naming patterns
            String[] possibleClasses = {
                addonName,
                addonName + "Addon",
                addonName + "Plugin",
                "Main",
                "Addon"
            };

            Class<?> addonClass = null;
            for (String className : possibleClasses) {
                try {
                    addonClass = classLoader.loadClass(className);
                    if (CoderAddon.class.isAssignableFrom(addonClass)) {
                        break;
                    }
                    addonClass = null;
                } catch (ClassNotFoundException ignored) {
                }
            }

            if (addonClass == null) {
                plugin.getLogger().warning("[Coder] Could not find CoderAddon implementation in: " + addonName);
                return;
            }

            // Instantiate and enable the addon
            if (CoderAddon.class.isAssignableFrom(addonClass)) {
                CoderAddon addon = (CoderAddon) addonClass.getDeclaredConstructor().newInstance();
                
                try {
                    addon.onEnable();
                    loadedAddons.put(addonName, addon);
                    plugin.getLogger().info("[Coder] ✓ Addon loaded: " + addon.getName() + " v" + addon.getVersion() + " by " + addon.getAuthor());
                } catch (Exception e) {
                    plugin.getLogger().severe("[Coder] Error enabling addon: " + addon.getName());
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Disable all addons
     */
    public void disableAddons() {
        for (CoderAddon addon : loadedAddons.values()) {
            try {
                addon.onDisable();
            } catch (Exception e) {
                plugin.getLogger().severe("[Coder] Error disabling addon: " + addon.getName());
                e.printStackTrace();
            }
        }
        loadedAddons.clear();
    }

    /**
     * Get a loaded addon by name
     */
    public CoderAddon getAddon(String name) {
        return loadedAddons.get(name);
    }

    /**
     * Get all loaded addons
     */
    public Collection<CoderAddon> getLoadedAddons() {
        return loadedAddons.values();
    }

    /**
     * Get addon count
     */
    public int getAddonCount() {
        return loadedAddons.size();
    }

    /**
     * Send addon list to player/console
     */
    public void sendAddonList(CommandSender sender) {
        if (loadedAddons.isEmpty()) {
            sender.sendMessage("§c[Coder] No addons loaded.");
            return;
        }

        sender.sendMessage("§6========================================");
        sender.sendMessage("§6=== Coder Addons ===");
        sender.sendMessage("§6========================================");
        sender.sendMessage("§aLoaded Addons: §f" + loadedAddons.size());
        sender.sendMessage("");

        int i = 1;
        for (CoderAddon addon : loadedAddons.values()) {
            sender.sendMessage("§e" + i + ". §a" + addon.getName() + " §8(v" + addon.getVersion() + ")");
            sender.sendMessage("   §7by §f" + addon.getAuthor());
            sender.sendMessage("   §7" + addon.getDescription());
            sender.sendMessage("");
            i++;
        }

        sender.sendMessage("§6========================================");
    }

    /**
     * Reload an addon
     */
    public void reloadAddon(String name, CommandSender sender) {
        if (!loadedAddons.containsKey(name)) {
            sender.sendMessage("§cAddon not found: " + name);
            return;
        }

        try {
            CoderAddon addon = loadedAddons.get(name);
            addon.onDisable();
            loadedAddons.remove(name);
            sender.sendMessage("§aAddon unloaded: " + name);
            
            // Reload from file
            File addonFile = new File(addonsFolder, name + ".jar");
            if (addonFile.exists()) {
                loadAddon(addonFile);
                sender.sendMessage("§aAddon reloaded: " + name);
            } else {
                sender.sendMessage("§cAddon file not found: " + name + ".jar");
            }
        } catch (Exception e) {
            sender.sendMessage("§cError reloading addon: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
```
# manager/ScriptManager
```java
package me.coder.manager;

import me.coder.CoderPlugin;
import me.coder.ScriptInterface;
import me.coder.manager.UserExecutionControl;
import org.bukkit.command.CommandSender;
import org.python.util.PythonInterpreter;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;
import javax.tools.*;
import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.SimpleDateFormat;
import java.util.*;

public class ScriptManager {
    private final CoderPlugin plugin;
    private final Map<String, Object> loadedScripts = new HashMap<>();

    public ScriptManager(CoderPlugin plugin) {
        this.plugin = plugin;
    }

    public void runScript(String fileName, CommandSender sender) {
        File file = new File(plugin.getDataFolder(), "scripts/" + fileName);

        if (!file.exists()) {
            sender.sendMessage("§cScript not found: " + fileName);
            return;
        }

        // Check for terminal/shell commands (instant rejection)
        if (UserExecutionControl.hasTerminalCommands(file)) {
            sender.sendMessage("§c§lError: Error T10!");
            logError(new Exception("Terminal command detected in: " + fileName), fileName);
            return;
        }

        // Check for dangerous imports in Java files
        if (fileName.endsWith(".java")) {
            List<String> dangerousImports = UserExecutionControl.checkDangerousImports(file);
            if (!dangerousImports.isEmpty()) {
                UserExecutionControl.addPendingScript(sender, fileName, dangerousImports);
                UserExecutionControl.sendExecutionWarning(sender, dangerousImports);
                return;
            }
        }

        runScriptDirect(fileName, sender);
    }

    /**
     * Run a script directly without checking for dangerous imports
     * Used after confirmation
     */
    public void runScriptDirect(String fileName, CommandSender sender) {
        File file = new File(plugin.getDataFolder(), "scripts/" + fileName);

        if (!file.exists()) {
            sender.sendMessage("§cScript not found: " + fileName);
            return;
        }

        try {
            if (fileName.endsWith(".py")) {
                runPython(file, sender);
            } else if (fileName.endsWith(".lua")) {
                runLua(file, sender);
            } else if (fileName.endsWith(".java")) {
                compileJava(file, sender);
            } else {
                sender.sendMessage("§cUnsupported file type. Supported: .py, .lua, .java");
            }
        } catch (Exception e) {
            logError(e, fileName);
            sender.sendMessage("§cError executing script. Check error logs.");
        }
    }

    private void runPython(File file, CommandSender sender) {
        sender.sendMessage("§d[Coder] Executing Python: " + file.getName());
        try (PythonInterpreter pyInterp = new PythonInterpreter()) {
            OutputStream os = new OutputStream() {
                private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                @Override
                public void write(int b) {
                    if (b == '\n') {
                        String line = buffer.toString();
                        if (!line.isEmpty()) {
                            sender.sendMessage("§7[Python] " + line.trim());
                        }
                        buffer.reset();
                    } else {
                        buffer.write(b);
                    }
                }
            };
            pyInterp.setOut(new PrintStream(os));
            pyInterp.set("sender", sender);
            pyInterp.execfile(new FileInputStream(file));
            sender.sendMessage("§aScript executed successfully!");
        } catch (Exception e) {
            sender.sendMessage("§cPython Error: " + e.getMessage());
            logError(e, file.getName());
        }
    }

    private void runLua(File file, CommandSender sender) {
        sender.sendMessage("§d[Coder] Executing Lua: " + file.getName());
        try {
            Globals globals = JsePlatform.standardGlobals();
            globals.set("print", new VarArgFunction() {
                @Override
                public Varargs invoke(Varargs args) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 1; i <= args.narg(); i++) {
                        sb.append(args.arg(i).tojstring()).append(" ");
                    }
                    sender.sendMessage("§7[Lua] " + sb.toString().trim());
                    return LuaValue.NIL;
                }
            });
            globals.set("sender", org.luaj.vm2.lib.jse.CoerceJavaToLua.coerce(sender));
            globals.loadfile(file.getAbsolutePath()).call();
            sender.sendMessage("§aScript executed successfully!");
        } catch (Exception e) {
            sender.sendMessage("§cLua Error: " + e.getMessage());
            logError(e, file.getName());
        }
    }

    private void compileJava(File file, CommandSender sender) {
        sender.sendMessage("§d[Coder] Compiling Java: " + file.getName());
        
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            sender.sendMessage("§cError: JDK not found. Use a JDK, not a JRE.");
            return;
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        
        // Build classpath with standard Java classpath
        String classpath = System.getProperty("java.class.path");

        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
            Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjects(file);
            List<String> options = List.of("-classpath", classpath.toString(), "-encoding", "UTF-8");
            
            boolean success = compiler.getTask(null, fileManager, diagnostics, options, null, compilationUnits).call();

            if (!success) {
                sender.sendMessage("§cCompilation Failed!");
                for (Diagnostic<?> diagnostic : diagnostics.getDiagnostics()) {
                    sender.sendMessage("§c" + diagnostic.toString());
                }
                return;
            }
        } catch (IOException e) {
            sender.sendMessage("§cIO Error: " + e.getMessage());
            logError(e, file.getName());
            return;
        }

        try {
            List<URL> urlList = new ArrayList<>();
            urlList.add(file.getParentFile().toURI().toURL());
            
            URL[] urls = urlList.toArray(new URL[0]);
            try (URLClassLoader classLoader = new URLClassLoader(urls, this.getClass().getClassLoader())) {
                String className = file.getName().replace(".java", "");
                Class<?> clazz = classLoader.loadClass(className);
                Object instance = clazz.getDeclaredConstructor().newInstance();

                if (instance instanceof ScriptInterface) {
                    ((ScriptInterface) instance).run(sender);
                    sender.sendMessage("§aJava script executed successfully!");
                    loadedScripts.put(file.getName(), instance);
                } else {
                    sender.sendMessage("§cError: Class must implement ScriptInterface.");
                }
            }
        } catch (Exception e) {
            sender.sendMessage("§cExecution Error: " + e.getMessage());
            logError(e, file.getName());
        }
    }

    public void reloadScript(String fileName, CommandSender sender) {
        loadedScripts.remove(fileName);
        sender.sendMessage("§d[Coder] Reloaded: " + fileName);
    }

    public void loadScript(String fileName, CommandSender sender) {
        File file = new File(plugin.getDataFolder(), "scripts/" + fileName);
        if (!file.exists()) {
            sender.sendMessage("§cScript not found: " + fileName);
            return;
        }
        
        if (fileName.endsWith(".java")) {
            compileJava(file, sender);
            sender.sendMessage("§aScript loaded to memory.");
        } else {
            sender.sendMessage("§cOnly Java scripts can be preloaded.");
        }
    }

    public void unloadScript(String fileName, CommandSender sender) {
        if (loadedScripts.remove(fileName) != null) {
            sender.sendMessage("§aScript unloaded from memory.");
        } else {
            sender.sendMessage("§cScript not found in memory.");
        }
    }

    private void logError(Exception e, String fileName) {
        try {
            File logsDir = new File(plugin.getDataFolder(), "Logs/Error-Logs");
            
            // Ensure directory exists
            if (!logsDir.exists()) {
                if (!logsDir.mkdirs()) {
                    plugin.getLogger().severe("Failed to create error logs directory!");
                    return;
                }
            }
            
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            int errorCount = getErrorLogCount(logsDir) + 1;
            
            File errorLog = new File(logsDir, "Error-" + timestamp + "-" + errorCount + ".txt");
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(errorLog))) {
                writer.println("=== Coder Plugin Error Log ===");
                writer.println("Error in script: " + fileName);
                writer.println("Timestamp: " + new Date());
                writer.println("=====================================================");
                writer.println();
                e.printStackTrace(writer);
                writer.println();
                writer.println("=====================================================");
            }
            
            plugin.getLogger().warning("Error logged to: " + errorLog.getAbsolutePath());
        } catch (IOException ex) {
            plugin.getLogger().severe("Failed to log error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private int getErrorLogCount(File logsDir) {
        File[] files = logsDir.listFiles((dir, name) -> name.startsWith("Error-") && name.endsWith(".txt"));
        return (files != null) ? files.length : 0;
    }
}
```
# manager/UserExecutionControl
```java
package me.coder.manager;

import org.bukkit.command.CommandSender;
import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class UserExecutionControl {
    
    private static final Map<CommandSender, PendingScript> pendingScripts = new HashMap<>();
    
    // Dangerous imports that require confirmation
    private static final Set<String> DANGEROUS_IMPORTS = new HashSet<>(Arrays.asList(
        "subprocess",
        "os",
        "sys",
        "runtime",
        "process",
        "exec",
        "Runtime",
        "ProcessBuilder",
        "reflection",
        "java.lang.reflect",
        "java.io.File",
        "java.nio.file",
        "RandomAccessFile"
    ));
    
    // Terminal/shell paths that are blocked
    private static final Set<String> BLOCKED_TERMINALS = new HashSet<>(Arrays.asList(
        "/bin/sh",
        "/bin/bash",
        "/bin/zsh",
        "/bin/fish",
        "/usr/bin/bash",
        "/usr/bin/sh",
        "cmd.exe",
        "powershell",
        "pwsh"
    ));

    /**
     * Check if a script file contains terminal/shell commands
     * @param file The script file to check
     * @return true if terminal commands found
     */
    public static boolean hasTerminalCommands(File file) {
        try {
            List<String> lines = Files.readAllLines(file.toPath());
            
            for (String line : lines) {
                String lowerLine = line.toLowerCase();
                
                // Check for blocked terminal paths
                for (String terminal : BLOCKED_TERMINALS) {
                    if (lowerLine.contains(terminal.toLowerCase())) {
                        return true;
                    }
                }
                
                // Check for shell execution patterns
                if (lowerLine.contains("subprocess.") || 
                    lowerLine.contains("os.system") ||
                    lowerLine.contains("os.popen") ||
                    lowerLine.contains("ProcessBuilder") ||
                    lowerLine.contains("Runtime.getRuntime().exec")) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return false;
    }

    /**
     * Check if a script file contains dangerous imports
     * @param file The script file to check
     * @return List of dangerous imports found, or empty list if none
     */
    public static List<String> checkDangerousImports(File file) {
        List<String> found = new ArrayList<>();
        
        try {
            List<String> lines = Files.readAllLines(file.toPath());
            
            for (String line : lines) {
                // Check for Python imports
                if (line.trim().startsWith("import ") || line.trim().startsWith("from ")) {
                    for (String dangerous : DANGEROUS_IMPORTS) {
                        if (line.toLowerCase().contains(dangerous.toLowerCase())) {
                            if (!found.contains(dangerous)) {
                                found.add(dangerous);
                            }
                        }
                    }
                }
                
                // Check for Java imports
                if (line.trim().startsWith("import ")) {
                    for (String dangerous : DANGEROUS_IMPORTS) {
                        if (line.contains(dangerous)) {
                            if (!found.contains(dangerous)) {
                                found.add(dangerous);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return found;
    }

    /**
     * Add a pending script for confirmation
     * @param sender The user executing the script
     * @param fileName The script filename
     * @param dangerousImports List of dangerous imports found
     */
    public static void addPendingScript(CommandSender sender, String fileName, List<String> dangerousImports) {
        pendingScripts.put(sender, new PendingScript(fileName, dangerousImports));
    }

    /**
     * Get a pending script for a sender
     * @param sender The user
     * @return The pending script or null
     */
    public static PendingScript getPendingScript(CommandSender sender) {
        return pendingScripts.get(sender);
    }

    /**
     * Remove a pending script
     * @param sender The user
     */
    public static void removePendingScript(CommandSender sender) {
        pendingScripts.remove(sender);
    }

    /**
     * Send the execution control warning
     * @param sender The user
     * @param dangerousImports List of dangerous imports
     */
    public static void sendExecutionWarning(CommandSender sender, List<String> dangerousImports) {
        sender.sendMessage("§c=============================");
        sender.sendMessage("§c=== USER EXECUTION CONTROL ===");
        sender.sendMessage("§c[!] This Script Has System Imports:");
        
        for (String imp : dangerousImports) {
            sender.sendMessage("§6    - " + imp);
        }
        
        sender.sendMessage("§e");
        sender.sendMessage("§eTo continue running this script please do:");
        sender.sendMessage("§a/coder confirm");
        sender.sendMessage("§e");
        sender.sendMessage("§eIf you wish to not run this script please do:");
        sender.sendMessage("§c/coder cancel");
        sender.sendMessage("§c=============================");
    }

    /**
     * Inner class representing a pending script
     */
    public static class PendingScript {
        public String fileName;
        public List<String> dangerousImports;
        public long createdAt;

        public PendingScript(String fileName, List<String> dangerousImports) {
            this.fileName = fileName;
            this.dangerousImports = dangerousImports;
            this.createdAt = System.currentTimeMillis();
        }

        /**
         * Check if this pending script has expired (older than 5 minutes)
         * @return true if expired
         */
        public boolean isExpired() {
            return System.currentTimeMillis() - createdAt > 300000; // 5 minutes
        }
    }
}
```
# manager/VersionManager
```java
package me.coder.manager;

import me.coder.CoderPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.util.concurrent.CompletableFuture;

/**
 * Manages version checking, updating, and config file monitoring
 */
public class VersionManager {
    
    private final CoderPlugin plugin;
    private final String VERSION_CHECK_URL = "https://codestuff.pages.dev/version/CoderVersion.txt";
    private final String DOWNLOAD_URL_TEMPLATE = "https://github.com/firesmasher-c6/Coder/releases/download/%s/Coder-%s.jar";
    
    private String currentVersion;
    private String latestVersion;
    private boolean updateAvailable = false;
    private boolean hasNotifiedOperators = false;
    private WatchService configWatcher;
    private Thread configWatcherThread;
    
    public VersionManager(CoderPlugin plugin) {
        this.plugin = plugin;
        this.currentVersion = plugin.getPluginMeta().getVersion();
    }
    
    /**
     * Start the version manager - checks for updates and watches config file
     */
    public void start() {
        // Check for updates on startup
        checkForUpdates();
        
        // Start watching config.yml for changes
        startConfigWatcher();
        
        plugin.getLogger().info("[VersionManager] Started - Current version: " + currentVersion);
    }
    
    /**
     * Stop the version manager and cleanup resources
     */
    public void stop() {
        stopConfigWatcher();
    }
    
    /**
     * Check for updates asynchronously
     */
    public void checkForUpdates() {
        CompletableFuture.runAsync(() -> {
            try {
                String remoteVersion = fetchRemoteVersion();
                if (remoteVersion != null && !remoteVersion.isEmpty()) {
                    this.latestVersion = remoteVersion.trim();
                    this.updateAvailable = isNewerVersion(currentVersion, latestVersion);
                    
                    if (updateAvailable) {
                        plugin.getLogger().info("[VersionManager] Update available! Latest: " + latestVersion + " | Current: " + currentVersion);
                        hasNotifiedOperators = false; // Reset notification flag so operators are notified
                    } else {
                        plugin.getLogger().info("[VersionManager] Plugin is up to date. Version: " + currentVersion);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[VersionManager] Failed to check for updates: " + e.getMessage());
            }
        });
    }
    
    /**
     * Fetch the remote version from the version check URL
     */
    private String fetchRemoteVersion() throws Exception {
        URI uri = new URI(VERSION_CHECK_URL);
        URL url = uri.toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestProperty("User-Agent", "CoderPlugin/1.0");
        
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                return reader.readLine();
            }
        } else {
            throw new Exception("HTTP " + responseCode);
        }
    }
    
    /**
     * Compare two version strings to determine if newVersion is newer
     * Supports formats like 1.4.2, 1.5.0, etc.
     */
    private boolean isNewerVersion(String currentVersion, String newVersion) {
        try {
            String[] currentParts = currentVersion.split("\\.");
            String[] newParts = newVersion.split("\\.");
            
            int maxLength = Math.max(currentParts.length, newParts.length);
            
            for (int i = 0; i < maxLength; i++) {
                int current = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
                int newVer = i < newParts.length ? Integer.parseInt(newParts[i]) : 0;
                
                if (newVer > current) {
                    return true;
                } else if (newVer < current) {
                    return false;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get the download link for the latest version
     */
    public String getDownloadLink() {
        if (latestVersion == null || latestVersion.isEmpty()) {
            return null;
        }
        return String.format(DOWNLOAD_URL_TEMPLATE, "v" + latestVersion, latestVersion);
    }
    
    /**
     * Start watching the config.yml file for changes
     */
    private void startConfigWatcher() {
        try {
            Path configPath = plugin.getDataFolder().toPath();
            configWatcher = FileSystems.getDefault().newWatchService();
            configPath.register(configWatcher, StandardWatchEventKinds.ENTRY_MODIFY);
            
            configWatcherThread = new Thread(() -> {
                try {
                    while (true) {
                        WatchKey key = configWatcher.take();
                        
                        for (WatchEvent<?> event : key.pollEvents()) {
                            Path changedFile = (Path) event.context();
                            if (changedFile.toString().equals("config.yml")) {
                                plugin.getLogger().info("[VersionManager] config.yml has been modified, reloading...");
                                // Add a small delay to ensure file write is complete
                                Thread.sleep(500);
                                plugin.reloadConfig();
                                plugin.getLogger().info("[VersionManager] config.yml reloaded successfully");
                            }
                        }
                        
                        if (!key.reset()) {
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    plugin.getLogger().info("[VersionManager] Config watcher stopped");
                }
            });
            
            configWatcherThread.setDaemon(true);
            configWatcherThread.setName("Coder-ConfigWatcher");
            configWatcherThread.start();
            
            plugin.getLogger().info("[VersionManager] Config file watcher started");
        } catch (Exception e) {
            plugin.getLogger().warning("[VersionManager] Failed to start config watcher: " + e.getMessage());
        }
    }
    
    /**
     * Stop watching the config.yml file
     */
    private void stopConfigWatcher() {
        if (configWatcherThread != null) {
            configWatcherThread.interrupt();
        }
        if (configWatcher != null) {
            try {
                configWatcher.close();
            } catch (Exception e) {
                plugin.getLogger().warning("[VersionManager] Error closing config watcher: " + e.getMessage());
            }
        }
    }
    
    /**
     * Check if update is available and notify an operator
     */
    public void notifyOperatorIfUpdateAvailable(org.bukkit.entity.Player operator) {
        if (updateAvailable && !hasNotifiedOperators) {
            operator.sendMessage("§6§l[Coder]§r §6An update is available!");
            operator.sendMessage("§6Current Version: §e" + currentVersion);
            operator.sendMessage("§6Latest Version: §e" + latestVersion);
            operator.sendMessage("§6Use §e/coder update §6to get the download link");
            hasNotifiedOperators = true;
        }
    }
    
    // Getters
    
    public String getCurrentVersion() {
        return currentVersion;
    }
    
    public String getLatestVersion() {
        return latestVersion;
    }
    
    public boolean isUpdateAvailable() {
        return updateAvailable;
    }
    
    public String getVersionCheckUrl() {
        return VERSION_CHECK_URL;
    }
}
```
# CoderPlugin
```java
package me.coder;

import me.coder.commands.CoderCommand;
import me.coder.listener.PlayerJoinListener;
import me.coder.manager.ScriptManager;
import me.coder.manager.VersionManager;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;

public class CoderPlugin extends JavaPlugin {
    
    private ScriptManager scriptManager;
    private VersionManager versionManager;

    @Override
    public void onEnable() {
        setupFolders();
        saveDefaultConfig();

        // Initialize managers
        this.scriptManager = new ScriptManager(this);
        this.versionManager = new VersionManager(this);
        
        // Start version manager (checks for updates and watches config.yml)
        versionManager.start();
        
        // Register command handler with version manager
        CoderCommand cmdHandler = new CoderCommand(this, scriptManager, versionManager);
        getCommand("coder").setExecutor(cmdHandler);
        getCommand("coder").setTabCompleter(cmdHandler);
        
        // Register event listeners
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(versionManager), this);

        getLogger().info("Coder v" + getPluginMeta().getVersion() + " enabled.");
    }

    private void setupFolders() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        new File(getDataFolder(), "scripts").mkdirs();
        new File(getDataFolder(), "Logs/Error-Logs").mkdirs();
    }

    @Override
    public void onDisable() {
        // Stop version manager and cleanup
        if (versionManager != null) {
            versionManager.stop();
        }
        
        getLogger().info("Coder plugin disabled.");
    }

    public ScriptManager getScriptManager() {
        return scriptManager;
    }
    
    public VersionManager getVersionManager() {
        return versionManager;
    }
}
```
# ScriptInterface
```java
package me.coder;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Interface that all Java scripts must implement
 * Provides access to the command sender and basic Bukkit functionality
 */
public interface ScriptInterface {
    /**
     * Called when the script is executed
     * @param sender The CommandSender who ran the script (console or player)
     */
    void run(CommandSender sender);

    /**
     * Check if the sender is a player
     * @param sender The CommandSender to check
     * @return true if sender is a Player, false otherwise
     */
    default boolean isPlayer(CommandSender sender) {
        return sender instanceof Player;
    }

    /**
     * Send a formatted message to the sender
     * @param sender The CommandSender to send to
     * @param message The message to send (supports color codes with §)
     */
    default void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(message);
    }

    /**
     * Send an error message (red colored)
     * @param sender The CommandSender to send to
     * @param message The error message
     */
    default void sendError(CommandSender sender, String message) {
        sender.sendMessage("§c" + message);
    }

    /**
     * Send a success message (green colored)
     * @param sender The CommandSender to send to
     * @param message The success message
     */
    default void sendSuccess(CommandSender sender, String message) {
        sender.sendMessage("§a" + message);
    }

    /**
     * Send an info message (blue colored)
     * @param sender The CommandSender to send to
     * @param message The info message
     */
    default void sendInfo(CommandSender sender, String message) {
        sender.sendMessage("§9" + message);
    }

    /**
     * Send a warning message (yellow colored)
     * @param sender The CommandSender to send to
     * @param message The warning message
     */
    default void sendWarning(CommandSender sender, String message) {
        sender.sendMessage("§e" + message);
    }
}
```
# pom.xml (MAVEN 3.16.9) [TARGET=JAVA21]
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>me.coder</groupId>
    <artifactId>Coder</artifactId>
    <version>1.7.4</version>
    <packaging>jar</packaging>

    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <repositories>
        <repository>
            <id>papermc-repo</id>
            <url>https://repo.papermc.io/repository/maven-public/</url>
        </repository>
    </repositories>

    <dependencies>
        <!-- Bukkit/Paper API -->
        <dependency>
            <groupId>io.papermc.paper</groupId>
            <artifactId>paper-api</artifactId>
            <version>1.21-R0.1-SNAPSHOT</version>
            <scope>provided</scope>
        </dependency>

        <!-- Lua Support -->
        <dependency>
            <groupId>org.luaj</groupId>
            <artifactId>luaj-jse</artifactId>
            <version>3.0.1</version>
        </dependency>

        <!-- Python Support -->
        <dependency>
            <groupId>org.python</groupId>
            <artifactId>jython-standalone</artifactId>
            <version>2.7.3</version>
        </dependency>

        <!-- JSON Parsing -->
        <dependency>
            <groupId>com.google.code.gson</groupId>
            <artifactId>gson</artifactId>
            <version>2.10.1</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Java Compiler Plugin -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>21</source>
                    <target>21</target>
                </configuration>
            </plugin>

            <!-- Shade Plugin for bundling dependencies -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.5.0</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>shade</goal>
                        </goals>
                        <configuration>
                            <transformers>
                                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                    <mainClass>me.coder.CoderPlugin</mainClass>
                                </transformer>
                            </transformers>
                        </configuration>
                    </execution>
                </executions>
            </plugin>

            <!-- Jar Plugin -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <version>3.3.0</version>
                <configuration>
                    <archive>
                        <manifest>
                            <mainClass>me.coder.CoderPlugin</mainClass>
                        </manifest>
                    </archive>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

# src/main/resources/ (CONTENTS)

## plugin.yml
```yaml
name: Coder
version: 1.7.4
main: me.coder.CoderPlugin
api-version: '1.21'

description: Execute Python, Lua, and Java scripts on your Minecraft server with version checking and auto-updates

commands:
  coder:
    description: Execute scripts and manage the plugin
    usage: /coder <run|reload|load|unload|confirm|cancel|update> [filename]
    permission: coder.admin
    permission-message: You must be an OP to use this command!
    aliases:
      - code

permissions:
  coder.admin:
    description: Allows execution and management of coder scripts, plus version checking.
    default: op
```

## config.yml (DO NOT MODIFY)
```yaml
# Coder Plugin Configuration
plugin:
  enabled: true

# Script Settings
scripts:
  python-enabled: true
  lua-enabled: true
  java-enabled: true

# Enable Coder Addons?
enabled: true
blocked-addon-message: "[Coder] This Coder Addon Has Been Blocked. If This is a mistake please read your code."
```

# GOAL: [
    MAKE THE PLUGIN LISTEN TO config.yml (plugins/Coder/config.yml),
    MAKE THE PLUGIN LISTEN TO PLAYERS OR CONSOLE THAT DOES "/pl" or "/plugins" AND SEND AN EXTENSION OF THE COMMAND {
        e.g.:
            player 1: does /pl
                what happens?:
                    server returns The "Bukkit Plugins" and "Paper Plugins",
                    and Coder lets the server does it and Coder searches the whole plugins/ directory for .jar and checks each .jar for a paper-plugin.yml or plugin.yml and checks if in its dependencies or depend section has "Coder", if it does it sends:
                        ```text
                        Coder Addons:
                        {TheAddonsDetectedHere!}
    }
    WHEN A PLAYER DOES /coder update the VersionManager tells the version of the Coder Plugin,
        BUT when it detects a NEW latest version, it tells the user to do /coder update-jar to download the latest VERSION and the plugin should accept REDIRECTS!
            Get the info in: https://codestuff.pages.dev/version/CoderVersion.txt and it:
                should see 1.7.4 | download-link: https://github.com/firesmasher-c6/Coder/releases/download/1.7.4/Coder-1.7.4.jar | modrinth: https://modrinth.com/plugin/Coder. it breaks down every data, when player or console does /coder update or /coder version and it detects a new version, tell the user to do /coder update-jar to download the latest new and delete the old Coder.jar...
]
