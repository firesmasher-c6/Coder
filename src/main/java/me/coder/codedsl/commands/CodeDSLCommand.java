package me.coder.codedsl.commands;

import me.coder.api.CoderAPI;
import me.coder.codedsl.CodeDSLPlugin;
import me.coder.codedsl.manager.ScriptManager;
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
                api.log("Loading script: " + filename + " (requested by " + sender.getName() + ")");
            }
            
            scriptManager.loadScript(filename, sender);
            sender.sendMessage("§a✓ Script loaded: " + filename);
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
        return handleRun(sender, args);
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
            List<String> subcommands = List.of("run", "load", "unload", "reload", "list", "configreload");
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
                               CoderAPI apiInstance, File folder) {
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