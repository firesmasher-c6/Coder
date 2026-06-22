package me.coder.commands;

import me.coder.CoderPlugin;
import me.coder.manager.ScriptManager;
import me.coder.manager.VersionManager;
import me.coder.manager.UserExecutionControl;
import me.coder.manager.ConfigManager;
import me.coder.javafixer.JavaCompiler;
import org.bukkit.command.*;
import java.io.File;
import java.util.*;

public class CoderCommand implements CommandExecutor, TabCompleter {

    private final ScriptManager scriptManager;
    private final CoderPlugin plugin;
    private final VersionManager versionManager;
    private final ConfigManager configManager;
    private final JavaCompiler javaCompiler;

    public CoderCommand(CoderPlugin plugin, ScriptManager scriptManager, VersionManager versionManager, ConfigManager configManager, JavaCompiler javaCompiler) {
        this.plugin = plugin;
        this.scriptManager = scriptManager;
        this.versionManager = versionManager;
        this.configManager = configManager;
        this.javaCompiler = javaCompiler;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§cUsage: /coder <run|reload|load|unload|confirm|cancel|update|update-jar|reload-config> [filename]");
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
                    configManager.loadConfig();
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

        // Handle reload-config command
        if (action.equals("reload-config")) {
            if (!sender.hasPermission("coder.admin")) {
                sender.sendMessage("§cYou don't have permission to use this command!");
                return true;
            }
            configManager.loadConfig();
            sender.sendMessage("§a✓ Config reloaded!");
            return true;
        }

        // Handle update command - shows update info
        if (action.equals("update")) {
            versionManager.handleUpdateCommand(sender);
            return true;
        }

        // Handle update-jar command - downloads and installs new version
        if (action.equals("update-jar")) {
            if (!sender.hasPermission("coder.admin")) {
                sender.sendMessage("§cYou don't have permission to use this command!");
                return true;
            }
            versionManager.handleUpdateJarCommand(sender);
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
                    // Check if it's a Java file
                    if (fileName.endsWith(".java")) {
                        File javaFile = new File(plugin.getDataFolder(), "scripts/" + fileName);
                        if (javaFile.exists()) {
                            javaCompiler.compileAndExecute(javaFile, sender);
                        } else {
                            // Try without extension
                            javaFile = new File(plugin.getDataFolder(), "scripts/" + fileName.replace(".java", "") + ".java");
                            if (javaFile.exists()) {
                                javaCompiler.compileAndExecute(javaFile, sender);
                            } else {
                                sender.sendMessage("§cJava file not found: " + fileName);
                            }
                        }
                    } else {
                        // Check if .java version exists
                        File javaFile = new File(plugin.getDataFolder(), "scripts/" + fileName + ".java");
                        if (javaFile.exists()) {
                            javaCompiler.compileAndExecute(javaFile, sender);
                        } else {
                            // Handle as regular script (Python/Lua)
                            scriptManager.runScript(fileName, sender);
                        }
                    }
                    break;
                case "load":
                    scriptManager.loadScript(fileName, sender);
                    break;
                case "unload":
                    scriptManager.unloadScript(fileName, sender);
                    break;
                default:
                    sender.sendMessage("§cUnknown action: " + action);
                    sender.sendMessage("§cValid actions: run, reload, load, unload, confirm, cancel, update, update-jar, reload-config");
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
            return Arrays.asList("run", "reload", "load", "unload", "confirm", "cancel", "update", "update-jar", "reload-config");
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
        return action.equalsIgnoreCase("run") || 
               action.equalsIgnoreCase("reload") || 
               action.equalsIgnoreCase("load") || 
               action.equalsIgnoreCase("unload");
    }

    private boolean isSupportedFile(String fileName) {
        return fileName.endsWith(".py") || 
               fileName.endsWith(".lua") || 
               fileName.endsWith(".java") || 
               fileName.endsWith(".class");
    }
}