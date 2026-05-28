package me.coder.commands;

import me.coder.CoderPlugin;
import me.coder.manager.ScriptManager;
import org.bukkit.command.*;
import java.io.File;
import java.util.*;

public class CoderCommand implements CommandExecutor, TabCompleter {

    private final ScriptManager scriptManager;
    private final CoderPlugin plugin; // Store plugin to access data folder

    public CoderCommand(CoderPlugin plugin, ScriptManager scriptManager) {
        this.plugin = plugin;
        this.scriptManager = scriptManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /coder <run|reload> <filename>");
            return true;
        }

        String action = args[0].toLowerCase();
        String fileName = args[1];

        if (action.equals("run")) {
            scriptManager.runScript(fileName);
            sender.sendMessage("§d[Coder] Executing: " + fileName);
        } else if (action.equals("reload")) {
            scriptManager.reloadScript(fileName);
            sender.sendMessage("§d[Coder] Reloaded: " + fileName);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        // Tab 1: run or reload
        if (args.length == 1) {
            return Arrays.asList("run", "reload");
        }
        
        // Tab 2: List files in /scripts/
        if (args.length == 2 && (args[0].equalsIgnoreCase("run") || args[0].equalsIgnoreCase("reload"))) {
            List<String> fileList = new ArrayList<>();
            File scriptsDir = new File(plugin.getDataFolder(), "scripts");
            
            if (scriptsDir.exists() && scriptsDir.isDirectory()) {
                File[] files = scriptsDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile()) {
                            fileList.add(file.getName());
                        }
                    }
                }
            }
            return fileList;
        }
        return new ArrayList<>();
    }
}