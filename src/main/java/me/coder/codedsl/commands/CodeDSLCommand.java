package me.coder.codedsl.commands;

import me.coder.api.CoderAPI;
import me.coder.codedsl.CodeDSLPlugin;
import me.coder.codedsl.manager.ScriptManager;
import me.coder.codedsl.manager.VersionManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CodeDSLCommand implements CommandExecutor, TabCompleter {
    
    private static ScriptManager scriptManager;
    private static CoderAPI api;
    private static File dataFolder;
    private static VersionManager versionManager;
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, 
                           @NotNull String label, @NotNull String[] args) {
        
        // Safety check: ensure static fields are initialized
        if (!isInitialized()) {
            sender.sendMessage("§cCodeDSL is not properly initialized. Please restart the server.");
            return false;
        }
        
        if (args.length == 0) {
            sender.sendMessage("§b--- CodeDSL Commands ---");
            sender.sendMessage("§a/codedsl run <filename> §7- Run a script");
            sender.sendMessage("§a/codedsl load <filename> §7- Load a script");
            sender.sendMessage("§a/codedsl unload <filename> §7- Unload a script");
            sender.sendMessage("§a/codedsl reload <filename> §7- Reload a script");
            sender.sendMessage("§a/codedsl list §7- List loaded scripts");
            sender.sendMessage("§a/codedsl configreload §7- Reload config");
            sender.sendMessage("§a/codedsl version §7- Check version and updates");
            sender.sendMessage("§a/codedsl update §7- Get update information");
            sender.sendMessage("§a/codedsl confirm §7- Confirm and download update");
            return true;
        }
        
        String subcommand = args[0].toLowerCase();
        
        try {
            switch (subcommand) {
                case "run":
                    return handleRun(sender, args);
                case "load":
                    return handleLoad(sender, args);
                case "unload":
                    return handleUnload(sender, args);
                case "list":
                    return handleList(sender);
                case "reload":
                    return handleReload(sender, args);
                case "configreload":
                    return handleConfigReload(sender);
                case "version":
                    return handleVersion(sender);
                case "update":
                    return handleUpdate(sender);
                case "confirm":
                    return handleConfirm(sender);
                default:
                    sender.sendMessage("§cUnknown subcommand: " + subcommand);
                    sender.sendMessage("§7Use /codedsl for help");
                    return false;
            }
        } catch (Exception e) {
            sender.sendMessage("§c✗ Command execution error: " + e.getMessage());
            if (api != null) {
                api.logError("CodeDSL command error: " + e.getMessage());
            }
            return false;
        }
    }
    
    /**
     * Check if all required static fields are initialized
     */
    private static boolean isInitialized() {
        return scriptManager != null && api != null && dataFolder != null;
    }
    
    /**
     * Handle /codedsl run <filename>
     */
    private boolean handleRun(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /codedsl run <filename>");
            return false;
        }
        
        try {
            String filename = args[1];
            
            if (!isValidFileName(filename)) {
                sender.sendMessage("§cInvalid filename: " + filename);
                return false;
            }
            
            if (api != null) {
                api.log("Running script: " + filename + " (requested by " + sender.getName() + ")");
            }
            
            // Run the script (not load)
            scriptManager.runScript(filename, sender);
            return true;
            
        } catch (Exception e) {
            sender.sendMessage("§c✗ Error running script: " + e.getMessage());
            if (api != null) {
                api.logError("Script execution failed for " + args[1] + ": " + e.getMessage());
            }
            return false;
        }
    }
    
    /**
     * Handle /codedsl load <filename>
     */
    private boolean handleLoad(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /codedsl load <filename>");
            return false;
        }
        
        try {
            String filename = args[1];
            
            if (!isValidFileName(filename)) {
                sender.sendMessage("§cInvalid filename: " + filename);
                return false;
            }
            
            if (api != null) {
                api.log("Loading script: " + filename + " (requested by " + sender.getName() + ")");
            }
            
            // Load the script (register commands)
            scriptManager.loadScript(filename, sender);
            sender.sendMessage("§a✓ Script loaded: " + filename);
            return true;
            
        } catch (Exception e) {
            sender.sendMessage("§c✗ Error loading script: " + e.getMessage());
            if (api != null) {
                api.logError("Script load failed for " + args[1] + ": " + e.getMessage());
            }
            return false;
        }
    }
    
    /**
     * Handle /codedsl unload <filename>
     */
    private boolean handleUnload(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /codedsl unload <filename>");
            return false;
        }
        
        try {
            String filename = args[1];
            
            if (!isValidFileName(filename)) {
                sender.sendMessage("§cInvalid filename: " + filename);
                return false;
            }
            
            scriptManager.unloadScript(filename, sender);
            sender.sendMessage("§a✓ Script unloaded: " + filename);
            
            if (api != null) {
                api.log("Script unloaded: " + filename + " (by " + sender.getName() + ")");
            }
            
            return true;
            
        } catch (Exception e) {
            sender.sendMessage("§c✗ Error unloading script: " + e.getMessage());
            if (api != null) {
                api.logError("Script unload failed for " + args[1] + ": " + e.getMessage());
            }
            return false;
        }
    }
    
    /**
     * Handle /codedsl list
     */
    private boolean handleList(CommandSender sender) {
        try {
            if (scriptManager == null) {
                sender.sendMessage("§cScriptManager not initialized");
                return false;
            }
            
            var scripts = scriptManager.getLoadedScripts();
            
            if (scripts == null || scripts.isEmpty()) {
                sender.sendMessage("§bNo scripts loaded.");
                return true;
            }
            
            sender.sendMessage("§b--- Loaded Scripts ---");
            for (String script : scripts) {
                sender.sendMessage("§a• " + script);
            }
            
            if (api != null) {
                api.log("Script list requested by " + sender.getName());
            }
            
            return true;
            
        } catch (Exception e) {
            sender.sendMessage("§c✗ Error listing scripts: " + e.getMessage());
            if (api != null) {
                api.logError("Failed to list scripts: " + e.getMessage());
            }
            return false;
        }
    }
    
    /**
     * Handle /codedsl reload <filename>
     */
    private boolean handleReload(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /codedsl reload <filename>");
            return false;
        }
        
        try {
            String filename = args[1];
            
            if (!isValidFileName(filename)) {
                sender.sendMessage("§cInvalid filename: " + filename);
                return false;
            }
            
            sender.sendMessage("§eReloading " + filename + "...");
            
            if (api != null) {
                api.log("Reloading script: " + filename + " (by " + sender.getName() + ")");
            }
            
            scriptManager.unloadScript(filename, sender);
            scriptManager.loadScript(filename, sender);
            
            sender.sendMessage("§a✓ Script reloaded: " + filename);
            return true;
            
        } catch (Exception e) {
            sender.sendMessage("§c✗ Error reloading script: " + e.getMessage());
            if (api != null) {
                api.logError("Script reload failed for " + args[1] + ": " + e.getMessage());
            }
            return false;
        }
    }
    
    /**
     * Handle /codedsl configreload
     */
    private boolean handleConfigReload(CommandSender sender) {
        try {
            CodeDSLPlugin plugin = CodeDSLPlugin.getInstance();
            
            if (plugin == null) {
                sender.sendMessage("§c✗ CodeDSLPlugin not initialized");
                return false;
            }
            
            var addonInstance = plugin.getAddonInstance();
            if (addonInstance == null) {
                sender.sendMessage("§c✗ CodeDSLAddon not initialized");
                return false;
            }
            
            addonInstance.reloadConfig();
            sender.sendMessage("§a✓ Configuration reloaded");
            
            if (api != null) {
                api.log("Config reloaded by " + sender.getName());
            }
            
            return true;
            
        } catch (Exception e) {
            sender.sendMessage("§c✗ Error: " + e.getMessage());
            if (api != null) {
                api.logError("Config reload failed: " + e.getMessage());
            }
            return false;
        }
    }
    
    /**
     * Handle /codedsl version
     * NEW: Display version information
     */
    private boolean handleVersion(CommandSender sender) {
        try {
            if (versionManager == null) {
                sender.sendMessage("§cVersionManager not initialized");
                return false;
            }
            
            sender.sendMessage("§a━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            sender.sendMessage("§a CodeDSL Version Information");
            sender.sendMessage("§a━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            sender.sendMessage("§f Current Version: §7" + versionManager.getCurrentVersion());
            
            if (versionManager.getLatestVersion() != null) {
                sender.sendMessage("§f Latest Version:  §a" + versionManager.getLatestVersion());
                
                if (versionManager.isUpdateAvailable()) {
                    sender.sendMessage("§c Update Available!");
                    sender.sendMessage("§f Download: §b" + versionManager.getDownloadLink());
                } else {
                    sender.sendMessage("§a You are up to date!");
                }
            } else {
                sender.sendMessage("§7Latest version info not available (check network connection)");
            }
            
            sender.sendMessage("§a━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            
            if (api != null) {
                api.log("Version info requested by " + sender.getName());
            }
            
            return true;
            
        } catch (Exception e) {
            sender.sendMessage("§c✗ Error retrieving version info: " + e.getMessage());
            if (api != null) {
                api.logError("Failed to retrieve version info: " + e.getMessage());
            }
            return false;
        }
    }
    
    /**
     * Handle /codedsl update
     * NEW: Display update information and download link
     */
    private boolean handleUpdate(CommandSender sender) {
        try {
            if (versionManager == null) {
                sender.sendMessage("§cVersionManager not initialized");
                return false;
            }
            
            if (!versionManager.isUpdateAvailable()) {
                sender.sendMessage("§a✓ You are running the latest version!");
                sender.sendMessage("§f Current: §7" + versionManager.getCurrentVersion());
                return true;
            }
            
            // Use VersionManager to handle the update prompt
            if (sender instanceof org.bukkit.entity.Player) {
                versionManager.handleUpdateCommand((org.bukkit.entity.Player) sender);
            } else {
                // For console, show basic info and set pending update
                sender.sendMessage("§a━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                sender.sendMessage("§a CodeDSL Update Available");
                sender.sendMessage("§a━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                sender.sendMessage("§f Current Version: §7" + versionManager.getCurrentVersion());
                sender.sendMessage("§f Latest Version:  §a" + versionManager.getLatestVersion());
                sender.sendMessage("");
                sender.sendMessage("§f Download Link:");
                sender.sendMessage("§b " + versionManager.getDownloadLink());
                sender.sendMessage("");
                sender.sendMessage("§eRun §f/codedsl confirm §eto download and update!");
                sender.sendMessage("§a━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                
                // Set pending update for console
                versionManager.handleUpdateCommandConsole();
            }
            
            if (api != null) {
                api.log("Update info requested by " + sender.getName());
            }
            
            return true;
            
        } catch (Exception e) {
            sender.sendMessage("§c✗ Error: " + e.getMessage());
            if (api != null) {
                api.logError("Update check failed: " + e.getMessage());
            }
            return false;
        }
    }
    
    /**
     * Handle /codedsl confirm
     * NEW: Confirm and download the pending update
     */
    private boolean handleConfirm(CommandSender sender) {
        try {
            if (versionManager == null) {
                sender.sendMessage("§cVersionManager not initialized");
                return false;
            }
            
            if (versionManager.getPendingUpdateVersion() == null) {
                sender.sendMessage("§cNo pending update. Run §f/codedsl update §cfirst.");
                return false;
            }
            
            // Handle both player and console
            if (sender instanceof org.bukkit.entity.Player) {
                org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;
                versionManager.handleConfirmUpdate(player);
            } else {
                // Console can also confirm
                versionManager.handleConfirmUpdateConsole();
            }
            
            if (api != null) {
                api.log("Update download confirmed by " + sender.getName());
            }
            
            return true;
            
        } catch (Exception e) {
            sender.sendMessage("§c✗ Error: " + e.getMessage());
            if (api != null) {
                api.logError("Update confirmation failed: " + e.getMessage());
            }
            return false;
        }
    }
    
    /**
     * Validate filename to prevent directory traversal attacks
     */
    private boolean isValidFileName(String filename) {
        if (filename == null || filename.isEmpty()) {
            return false;
        }
        
        // Reject path traversal attempts
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return false;
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, 
                                     @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!isInitialized()) {
            return completions;
        }
        
        if (args.length == 1) {
            // Complete subcommands
            String partial = args[0].toLowerCase();
            List<String> subcommands = List.of("run", "load", "unload", "reload", "list", "configreload", "version", "update", "confirm");
            for (String sub : subcommands) {
                if (sub.startsWith(partial)) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2) {
            // Complete filenames for subcommands that need them
            String subcommand = args[0].toLowerCase();
            if (List.of("run", "load", "unload", "reload").contains(subcommand)) {
                try {
                    if (scriptManager != null) {
                        var scripts = scriptManager.getLoadedScripts();
                        if (scripts != null) {
                            String partial = args[1].toLowerCase();
                            for (String script : scripts) {
                                if (script.toLowerCase().startsWith(partial)) {
                                    completions.add(script);
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }
        
        return completions;
    }

    /**
     * Initialize static fields for command execution
     */
    public static void register(org.bukkit.plugin.Plugin plugin, ScriptManager scriptMgr, 
                               CoderAPI apiInstance, File folder, VersionManager versionMgr) {
        try {
            // Validate inputs
            if (scriptMgr == null || apiInstance == null || folder == null) {
                if (apiInstance != null) {
                    apiInstance.logError("Cannot initialize CodeDSL commands: Missing required parameters");
                }
                return;
            }
            
            // Set static fields for command executor use
            scriptManager = scriptMgr;
            api = apiInstance;
            dataFolder = folder;
            versionManager = versionMgr;
            
            if (apiInstance != null) {
                apiInstance.log("CodeDSL commands initialized and ready");
            }
            
        } catch (Exception e) {
            if (apiInstance != null) {
                apiInstance.logError("Failed to initialize CodeDSL commands: " + e.getMessage());
            }
        }
    }
}