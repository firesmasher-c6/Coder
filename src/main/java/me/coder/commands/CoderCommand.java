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