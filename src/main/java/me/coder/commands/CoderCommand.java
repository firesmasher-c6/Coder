package me.coder.commands;

import me.coder.CoderPlugin;
import me.coder.manager.ScriptManager;
import me.coder.manager.UserExecutionControl;
import org.bukkit.command.*;
import java.io.File;
import java.util.*;

public class CoderCommand implements CommandExecutor, TabCompleter {

    private final ScriptManager scriptManager;
    private final CoderPlugin plugin;

    public CoderCommand(CoderPlugin plugin, ScriptManager scriptManager) {
        this.plugin = plugin;
        this.scriptManager = scriptManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§cUsage: /coder <run|reload|load|unload|confirm|cancel> <filename>");
            return true;
        }

        String action = args[0].toLowerCase();

        // Handle confirmation commands
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

        // Handle normal script commands
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /coder <run|reload|load|unload> <filename>");
            return true;
        }

        String fileName = args[1];

        switch (action) {
            case "run":
                scriptManager.runScript(fileName, sender);
                break;
            case "reload":
                scriptManager.reloadScript(fileName, sender);
                break;
            case "load":
                scriptManager.loadScript(fileName, sender);
                break;
            case "unload":
                scriptManager.unloadScript(fileName, sender);
                break;
            default:
                sender.sendMessage("§cUnknown action: " + action);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("run", "reload", "load", "unload", "confirm", "cancel");
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