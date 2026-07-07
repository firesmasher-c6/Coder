package me.coder.commands;

import me.coder.CoderPlugin;
import me.coder.JavaCompiler;
import me.coder.manager.ScriptManager;
import me.coder.manager.VersionManager;
import me.coder.manager.UserExecutionControl;
import me.coder.manager.ConfigManager;
import me.coder.manager.BackupManager;
import org.bukkit.command.*;
import java.io.File;
import java.util.*;

public class CoderCommand implements CommandExecutor, TabCompleter {

    private final ScriptManager scriptManager;
    private final CoderPlugin plugin;
    private final VersionManager versionManager;
    private final ConfigManager configManager;
    private final JavaCompiler javaCompiler;
    private final BackupManager backupManager;

    public CoderCommand(CoderPlugin plugin, ScriptManager scriptManager, VersionManager versionManager, ConfigManager configManager, JavaCompiler javaCompiler, BackupManager backupManager) {
        this.plugin = plugin;
        this.scriptManager = scriptManager;
        this.versionManager = versionManager;
        this.configManager = configManager;
        this.javaCompiler = javaCompiler;
        this.backupManager = backupManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // Check if plugin is enabled
        if (!configManager.isPluginEnabled()) {
            sender.sendMessage("§c[Coder] Plugin is disabled in config.yml");
            return true;
        }

        if (args.length < 1) {
            showMainHelp(sender);
            return true;
        }

        String action = args[0].toLowerCase();

        // Handle backup command
        if (action.equals("backup")) {
            if (!configManager.isCommandEnabled("backup")) {
                sender.sendMessage("§c[Coder] The backup command is disabled in config.yml");
                return true;
            }
            backupManager.startBackup(sender);
            return true;
        }

        // Handle auto-backup-start command
        if (action.equals("auto-backup-start")) {
            if (!configManager.isCommandEnabled("auto-backup-start")) {
                sender.sendMessage("§c[Coder] The auto-backup-start command is disabled in config.yml");
                return true;
            }
            backupManager.startAutoBackup(sender);
            return true;
        }

        // Handle auto-backup-stop command
        if (action.equals("auto-backup-stop")) {
            if (!configManager.isCommandEnabled("auto-backup-stop")) {
                sender.sendMessage("§c[Coder] The auto-backup-stop command is disabled in config.yml");
                return true;
            }
            backupManager.stopAutoBackup(sender);
            return true;
        }

        // Handle reload command (reloads plugin, scripts, and config)
        if (action.equals("reload")) {
            if (!configManager.isReloadCommandEnabled()) {
                sender.sendMessage("§c[Coder] The reload command is disabled in config.yml");
                return true;
            }

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

        // Handle help command
        if (action.equals("help")) {
            if (args.length == 1) {
                showMainHelp(sender);
            } else {
                showCommandHelp(sender, args[1].toLowerCase());
            }
            return true;
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
            if (!configManager.isUpdateCommandEnabled()) {
                sender.sendMessage("§c[Coder] The update command is disabled in config.yml");
                return true;
            }
            versionManager.handleUpdateCommand(sender);
            return true;
        }

        // Handle update-jar command - downloads and installs new version
        if (action.equals("update-jar")) {
            if (!configManager.isUpdateJarCommandEnabled()) {
                sender.sendMessage("§c[Coder] The update-jar command is disabled in config.yml");
                return true;
            }
            
            if (!sender.hasPermission("coder.admin")) {
                sender.sendMessage("§cYou don't have permission to use this command!");
                return true;
            }
            versionManager.handleUpdateJarCommand(sender);
            return true;
        }

        // Handle confirmation commands (no filename needed)
        if (action.equals("confirm")) {
            if (!configManager.isConfirmCommandEnabled()) {
                sender.sendMessage("§c[Coder] The confirm command is disabled in config.yml");
                return true;
            }

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
            if (!configManager.isCancelCommandEnabled()) {
                sender.sendMessage("§c[Coder] The cancel command is disabled in config.yml");
                return true;
            }

            UserExecutionControl.PendingScript pending = UserExecutionControl.getPendingScript(sender);
            if (pending == null) {
                sender.sendMessage("§cNo pending script to cancel.");
                return true;
            }
            
            sender.sendMessage("§cCancelled execution of: " + pending.fileName);
            UserExecutionControl.removePendingScript(sender);
            return true;
        }

        // Handle enable-activity-logging command
        if (action.equals("enable-activity-logging")) {
            if (!configManager.isCommandEnabled("enable-activity-logging")) {
                sender.sendMessage("§c[Coder] The enable-activity-logging command is disabled in config.yml");
                return true;
            }
            
            plugin.getConfig().set("actions-manager.enabled", true);
            plugin.saveConfig();
            sender.sendMessage("§a[Coder] Activity logging has been §aenabled§a!");
            return true;
        }

        // Handle disable-activity-logging command
        if (action.equals("disable-activity-logging")) {
            if (!configManager.isCommandEnabled("disable-activity-logging")) {
                sender.sendMessage("§c[Coder] The disable-activity-logging command is disabled in config.yml");
                return true;
            }
            
            plugin.getConfig().set("actions-manager.enabled", false);
            plugin.saveConfig();
            sender.sendMessage("§a[Coder] Activity logging has been §cdisabled§a!");
            return true;
        }

        // All other commands need a filename
        if (args.length < 2) {
            showCommandHelp(sender, action);
            return true;
        }

        String fileName = args[1];

        // Handle script execution commands
        try {
            switch (action) {
                case "run":
                    if (!configManager.isRunCommandEnabled()) {
                        sender.sendMessage("§c[Coder] The run command is disabled in config.yml");
                        return true;
                    }

                    // Check if it's a Java file
                    if (fileName.endsWith(".java")) {
                        if (!configManager.isJavaEnabled()) {
                            sender.sendMessage("§c[Coder] Java scripts are disabled in config.yml");
                            return true;
                        }

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
                    } else if (fileName.endsWith(".py")) {
                        if (!configManager.isPythonEnabled()) {
                            sender.sendMessage("§c[Coder] Python scripts are disabled in config.yml");
                            return true;
                        }
                        scriptManager.runScript(fileName, sender);
                    } else if (fileName.endsWith(".lua")) {
                        if (!configManager.isLuaEnabled()) {
                            sender.sendMessage("§c[Coder] Lua scripts are disabled in config.yml");
                            return true;
                        }
                        scriptManager.runScript(fileName, sender);
                    } else {
                        // Check if .java version exists
                        File javaFile = new File(plugin.getDataFolder(), "scripts/" + fileName + ".java");
                        if (javaFile.exists()) {
                            if (!configManager.isJavaEnabled()) {
                                sender.sendMessage("§c[Coder] Java scripts are disabled in config.yml");
                                return true;
                            }
                            javaCompiler.compileAndExecute(javaFile, sender);
                        } else {
                            // Try python
                            File pyFile = new File(plugin.getDataFolder(), "scripts/" + fileName + ".py");
                            if (pyFile.exists()) {
                                if (!configManager.isPythonEnabled()) {
                                    sender.sendMessage("§c[Coder] Python scripts are disabled in config.yml");
                                    return true;
                                }
                                scriptManager.runScript(fileName + ".py", sender);
                            } else {
                                // Try lua
                                File luaFile = new File(plugin.getDataFolder(), "scripts/" + fileName + ".lua");
                                if (luaFile.exists()) {
                                    if (!configManager.isLuaEnabled()) {
                                        sender.sendMessage("§c[Coder] Lua scripts are disabled in config.yml");
                                        return true;
                                    }
                                    scriptManager.runScript(fileName + ".lua", sender);
                                } else {
                                    sender.sendMessage("§cScript not found: " + fileName);
                                }
                            }
                        }
                    }
                    return true;

                case "load":
                    if (!configManager.isLoadCommandEnabled()) {
                        sender.sendMessage("§c[Coder] The load command is disabled in config.yml");
                        return true;
                    }

                    if (!configManager.isJavaEnabled()) {
                        sender.sendMessage("§c[Coder] Java scripts are disabled in config.yml");
                        return true;
                    }

                    if (fileName.endsWith(".java")) {
                        File javaFile = new File(plugin.getDataFolder(), "scripts/" + fileName);
                        if (javaFile.exists()) {
                            javaCompiler.compileAndLoad(javaFile, sender);
                        } else {
                            javaFile = new File(plugin.getDataFolder(), "scripts/" + fileName.replace(".java", "") + ".java");
                            if (javaFile.exists()) {
                                javaCompiler.compileAndLoad(javaFile, sender);
                            } else {
                                sender.sendMessage("§cJava file not found: " + fileName);
                            }
                        }
                    } else {
                        File javaFile = new File(plugin.getDataFolder(), "scripts/" + fileName + ".java");
                        if (javaFile.exists()) {
                            javaCompiler.compileAndLoad(javaFile, sender);
                        } else {
                            sender.sendMessage("§cJava file not found: " + fileName);
                        }
                    }
                    return true;

                case "unload":
                    if (!configManager.isUnloadCommandEnabled()) {
                        sender.sendMessage("§c[Coder] The unload command is disabled in config.yml");
                        return true;
                    }

                    if (!configManager.isJavaEnabled()) {
                        sender.sendMessage("§c[Coder] Java scripts are disabled in config.yml");
                        return true;
                    }

                    String className = fileName.replace(".java", "");
                    javaCompiler.unloadClass(className, sender);
                    return true;

                default:
                    sender.sendMessage("§cUnknown command: " + action);
                    sender.sendMessage("§eRun §f/coder help §efor a list of commands");
                    return true;
            }
        } catch (Exception e) {
            sender.sendMessage("§cError executing command: " + e.getMessage());
            e.printStackTrace();
            return true;
        }
    }

    private void showMainHelp(CommandSender sender) {
        sender.sendMessage("§6╔═══════════════════════════════════════╗");
        sender.sendMessage("§6║§f      Coder Plugin Help (v2.3.1)     §6║");
        sender.sendMessage("§6╠═══════════════════════════════════════╣");
        sender.sendMessage("§a/coder run <file>       §7- Execute a script");
        
        if (configManager.isLoadCommandEnabled()) {
            sender.sendMessage("§a/coder load <file.java> §7- Load a Java class");
        }
        
        if (configManager.isUnloadCommandEnabled()) {
            sender.sendMessage("§a/coder unload <class>   §7- Unload a Java class");
        }
        
        if (configManager.isReloadCommandEnabled()) {
            sender.sendMessage("§a/coder reload [file]    §7- Reload config/scripts");
        }
        
        if (configManager.isUpdateCommandEnabled()) {
            sender.sendMessage("§a/coder update           §7- Check for updates");
        }
        
        if (configManager.isUpdateJarCommandEnabled()) {
            sender.sendMessage("§a/coder update-jar       §7- Download new version");
        }

        if (configManager.isCommandEnabled("backup")) {
            sender.sendMessage("§a/coder backup           §7- Create a backup");
        }
        
        if (configManager.isCommandEnabled("auto-backup-start")) {
            sender.sendMessage("§a/coder auto-backup-start §7- Start auto-backups");
        }
        
        if (configManager.isCommandEnabled("auto-backup-stop")) {
            sender.sendMessage("§a/coder auto-backup-stop §7- Stop auto-backups");
        }
        
        sender.sendMessage("§a/coder help [command]   §7- Show command help");
        sender.sendMessage("§6╚═══════════════════════════════════════╝");
    }

    private void showCommandHelp(CommandSender sender, String command) {
        switch (command) {
            case "run":
                sender.sendMessage("§6╔═══════════════════════════════════════╗");
                sender.sendMessage("§6║§f            RUN COMMAND              §6║");
                sender.sendMessage("§6╠═══════════════════════════════════════╣");
                sender.sendMessage("§aUsage: §f/coder run <filename>");
                sender.sendMessage("§7");
                sender.sendMessage("§eDescription:");
                sender.sendMessage("§7Execute a script (.py, .lua, or .java)");
                sender.sendMessage("§7");
                sender.sendMessage("§eSupported Languages:");
                
                if (configManager.isPythonEnabled()) {
                    sender.sendMessage("§7  • Python (.py)");
                }
                if (configManager.isLuaEnabled()) {
                    sender.sendMessage("§7  • Lua (.lua)");
                }
                if (configManager.isJavaEnabled()) {
                    sender.sendMessage("§7  • Java (.java) - compiles and runs");
                }
                
                sender.sendMessage("§7");
                sender.sendMessage("§eExamples:");
                sender.sendMessage("§7  §f/coder run script.py");
                sender.sendMessage("§7  §f/coder run script.lua");
                sender.sendMessage("§7  §f/coder run Script.java");
                sender.sendMessage("§6╚═══════════════════════════════════════╝");
                break;

            case "load":
                sender.sendMessage("§6╔═══════════════════════════════════════╗");
                sender.sendMessage("§6║§f           LOAD COMMAND              §6║");
                sender.sendMessage("§6╠═══════════════════════════════════════╣");
                sender.sendMessage("§aUsage: §f/coder load <filename.java>");
                sender.sendMessage("§7");
                sender.sendMessage("§eDescription:");
                sender.sendMessage("§7Compile a Java file and keep it in memory");
                sender.sendMessage("§7Compiled classes are stored in §f/JavaClasses/Loaded/");
                sender.sendMessage("§7");
                sender.sendMessage("§eNotes:");
                sender.sendMessage("§7  • Only works with §f.java §7files");
                sender.sendMessage("§7  • Class executes immediately after loading");
                sender.sendMessage("§7  • Use §f/coder unload §7to remove from memory");
                sender.sendMessage("§7");
                sender.sendMessage("§eExamples:");
                sender.sendMessage("§7  §f/coder load SystemInfo.java");
                sender.sendMessage("§7  §f/coder load MyScript.java");
                sender.sendMessage("§6╚═══════════════════════════════════════╝");
                break;

            case "unload":
                sender.sendMessage("§6╔═══════════════════════════════════════╗");
                sender.sendMessage("§6║§f          UNLOAD COMMAND             §6║");
                sender.sendMessage("§6╠═══════════════════════════════════════╣");
                sender.sendMessage("§aUsage: §f/coder unload <classname>");
                sender.sendMessage("§7");
                sender.sendMessage("§eDescription:");
                sender.sendMessage("§7Remove a loaded Java class from memory");
                sender.sendMessage("§7");
                sender.sendMessage("§eNotes:");
                sender.sendMessage("§7  • Only unloads previously loaded classes");
                sender.sendMessage("§7  • Class name is the filename without §f.java");
                sender.sendMessage("§7");
                sender.sendMessage("§eExamples:");
                sender.sendMessage("§7  §f/coder unload SystemInfo");
                sender.sendMessage("§7  §f/coder unload SystemInfo.java");
                sender.sendMessage("§6╚═══════════════════════════════════════╝");
                break;

            case "reload":
                sender.sendMessage("§6╔═══════════════════════════════════════╗");
                sender.sendMessage("§6║§f           RELOAD COMMAND             §6║");
                sender.sendMessage("§6╠═══════════════════════════════════════╣");
                sender.sendMessage("§aUsage: §f/coder reload [filename]");
                sender.sendMessage("§7");
                sender.sendMessage("§eDescription:");
                sender.sendMessage("§7Reload the plugin, config, or a specific script");
                sender.sendMessage("§7");
                sender.sendMessage("§eUsage modes:");
                sender.sendMessage("§7  §f/coder reload     §7→ Reload everything");
                sender.sendMessage("§7  §f/coder reload <f> §7→ Reload specific script");
                sender.sendMessage("§7");
                sender.sendMessage("§eExamples:");
                sender.sendMessage("§7  §f/coder reload");
                sender.sendMessage("§7  §f/coder reload script.py");
                sender.sendMessage("§6╚═══════════════════════════════════════╝");
                break;

            case "confirm":
                sender.sendMessage("§6╔═══════════════════════════════════════╗");
                sender.sendMessage("§6║§f          CONFIRM COMMAND             §6║");
                sender.sendMessage("§6╠═══════════════════════════════════════╣");
                sender.sendMessage("§aUsage: §f/coder confirm");
                sender.sendMessage("§7");
                sender.sendMessage("§eDescription:");
                sender.sendMessage("§7Confirm execution of a dangerous script");
                sender.sendMessage("§7Scripts with dangerous imports require confirmation");
                sender.sendMessage("§7");
                sender.sendMessage("§eWhen to use:");
                sender.sendMessage("§7  After running a script with system imports");
                sender.sendMessage("§7  Review the warning and confirm if safe");
                sender.sendMessage("§7");
                sender.sendMessage("§eExample workflow:");
                sender.sendMessage("§7  1. §f/coder run risky.java");
                sender.sendMessage("§7  2. §cReview dangerous imports warning");
                sender.sendMessage("§7  3. §f/coder confirm");
                sender.sendMessage("§6╚═══════════════════════════════════════╝");
                break;

            case "cancel":
                sender.sendMessage("§6╔═══════════════════════════════════════╗");
                sender.sendMessage("§6║§f           CANCEL COMMAND             §6║");
                sender.sendMessage("§6╠═══════════════════════════════════════╣");
                sender.sendMessage("§aUsage: §f/coder cancel");
                sender.sendMessage("§7");
                sender.sendMessage("§eDescription:");
                sender.sendMessage("§7Cancel a pending dangerous script execution");
                sender.sendMessage("§7");
                sender.sendMessage("§eWhen to use:");
                sender.sendMessage("§7  After running a script with system imports");
                sender.sendMessage("§7  If you decide not to execute it");
                sender.sendMessage("§7");
                sender.sendMessage("§eNote:");
                sender.sendMessage("§7  Pending scripts expire after 5 minutes");
                sender.sendMessage("§6╚═══════════════════════════════════════╝");
                break;

            case "backup":
                sender.sendMessage("§6╔═══════════════════════════════════════╗");
                sender.sendMessage("§6║§f           BACKUP COMMAND             §6║");
                sender.sendMessage("§6╠═══════════════════════════════════════╣");
                sender.sendMessage("§aUsage: §f/coder backup");
                sender.sendMessage("§7");
                sender.sendMessage("§eDescription:");
                sender.sendMessage("§7Create an immediate backup of Coder folder");
                sender.sendMessage("§7");
                sender.sendMessage("§eFormat: " + configManager.getBackupType());
                sender.sendMessage("§7Location: §fbackups/");
                sender.sendMessage("§7");
                sender.sendMessage("§eNote:");
                sender.sendMessage("§7  Backup runs asynchronously (doesn't freeze server)");
                sender.sendMessage("§6╚═══════════════════════════════════════╝");
                break;

            case "auto-backup-start":
                sender.sendMessage("§6╔═══════════════════════════════════════╗");
                sender.sendMessage("§6║§f      AUTO-BACKUP-START COMMAND       §6║");
                sender.sendMessage("§6╠═══════════════════════════════════════╣");
                sender.sendMessage("§aUsage: §f/coder auto-backup-start");
                sender.sendMessage("§7");
                sender.sendMessage("§eDescription:");
                sender.sendMessage("§7Start automatic backups on a schedule");
                sender.sendMessage("§7");
                sender.sendMessage("§eSchedule: §f" + configManager.getBackupSchedule());
                sender.sendMessage("§7Format: §f" + configManager.getBackupType());
                sender.sendMessage("§7");
                sender.sendMessage("§eNote:");
                sender.sendMessage("§7  Backups run in background (silent)");
                sender.sendMessage("§6╚═══════════════════════════════════════╝");
                break;

            case "auto-backup-stop":
                sender.sendMessage("§6╔═══════════════════════════════════════╗");
                sender.sendMessage("§6║§f       AUTO-BACKUP-STOP COMMAND       §6║");
                sender.sendMessage("§6╠═══════════════════════════════════════╣");
                sender.sendMessage("§aUsage: §f/coder auto-backup-stop");
                sender.sendMessage("§7");
                sender.sendMessage("§eDescription:");
                sender.sendMessage("§7Stop automatic backups");
                sender.sendMessage("§7");
                sender.sendMessage("§eNote:");
                sender.sendMessage("§7  Can be restarted anytime with auto-backup-start");
                sender.sendMessage("§6╚═══════════════════════════════════════╝");
                break;

            case "update":
                sender.sendMessage("§6╔═══════════════════════════════════════╗");
                sender.sendMessage("§6║§f           UPDATE COMMAND             §6║");
                sender.sendMessage("§6╠═══════════════════════════════════════╣");
                sender.sendMessage("§aUsage: §f/coder update");
                sender.sendMessage("§7");
                sender.sendMessage("§eDescription:");
                sender.sendMessage("§7Check if a plugin update is available");
                sender.sendMessage("§7Shows current vs latest version");
                sender.sendMessage("§6╚═══════════════════════════════════════╝");
                break;

            case "update-jar":
                sender.sendMessage("§6╔═══════════════════════════════════════╗");
                sender.sendMessage("§6║§f          UPDATE-JAR COMMAND          §6║");
                sender.sendMessage("§6╠═══════════════════════════════════════╣");
                sender.sendMessage("§aUsage: §f/coder update-jar");
                sender.sendMessage("§7");
                sender.sendMessage("§eDescription:");
                sender.sendMessage("§7Download and install the latest version");
                sender.sendMessage("§7Requires operator permissions");
                sender.sendMessage("§7");
                sender.sendMessage("§eNote:");
                sender.sendMessage("§7  Server must be restarted to load new version");
                sender.sendMessage("§6╚═══════════════════════════════════════╝");
                break;

            default:
                sender.sendMessage("§cUnknown command: " + command);
                sender.sendMessage("§eRun §f/coder help §efor a list of commands");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Only add commands that are enabled in config
            if (configManager.isRunCommandEnabled()) completions.add("run");
            if (configManager.isLoadCommandEnabled()) completions.add("load");
            if (configManager.isUnloadCommandEnabled()) completions.add("unload");
            if (configManager.isReloadCommandEnabled()) completions.add("reload");
            if (configManager.isConfirmCommandEnabled()) completions.add("confirm");
            if (configManager.isCancelCommandEnabled()) completions.add("cancel");
            if (configManager.isUpdateCommandEnabled()) completions.add("update");
            if (configManager.isUpdateJarCommandEnabled()) completions.add("update-jar");
            if (configManager.isCommandEnabled("backup")) completions.add("backup");
            if (configManager.isCommandEnabled("auto-backup-start")) completions.add("auto-backup-start");
            if (configManager.isCommandEnabled("auto-backup-stop")) completions.add("auto-backup-stop");
            
            completions.add("reload-config");
            completions.add("help");
            
            return completions;
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
        return (configManager.isRunCommandEnabled() && action.equalsIgnoreCase("run")) || 
               (configManager.isReloadCommandEnabled() && action.equalsIgnoreCase("reload")) || 
               (configManager.isLoadCommandEnabled() && action.equalsIgnoreCase("load")) || 
               (configManager.isUnloadCommandEnabled() && action.equalsIgnoreCase("unload")) ||
               action.equalsIgnoreCase("help");
    }

    private boolean isSupportedFile(String fileName) {
        boolean isPy = fileName.endsWith(".py") && configManager.isPythonEnabled();
        boolean isLua = fileName.endsWith(".lua") && configManager.isLuaEnabled();
        boolean isJava = (fileName.endsWith(".java") || fileName.endsWith(".class")) && configManager.isJavaEnabled();
        
        return isPy || isLua || isJava;
    }
}