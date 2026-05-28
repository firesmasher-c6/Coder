package me.coder.commands;

import me.coder.manager.ScriptManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.CommandSender;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CoderCommand implements CommandExecutor, TabCompleter {

    private final ScriptManager scriptManager;

    public CoderCommand(ScriptManager scriptManager) {
        this.scriptManager = scriptManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§cUsage: /coder <java|phyton|script|scripts> ...");
            return true;
        }

        String type = args[0].toLowerCase();

        // Handle Scripts Reload
        if (type.equals("scripts")) {
             if (args.length > 1 && args[1].equalsIgnoreCase("reload")) {
                sender.sendMessage("§a[Coder] Reloading all systems...");
                return true;
            }
        }
        
        // Handle Custom Language: /coder script <run|reload> <filename>
        else if (type.equals("script")) {
            if (args.length < 3) {
                sender.sendMessage("§cUsage: /coder script <run|reload> <filename>");
                return true;
            }
            String action = args[1].toLowerCase();
            String fileName = args[2];
            
            if (action.equals("run")) {
                scriptManager.runCustomScript(fileName);
                sender.sendMessage("§d[Coder] Executing: " + fileName);
            } else if (action.equals("reload")) {
                scriptManager.reloadCustomScript(fileName);
                sender.sendMessage("§d[Coder] Reloaded custom script: " + fileName);
            }
            return true;
        }

        // Handle Standard Java/Phyton
        else if (type.equals("java") || type.equals("phyton")) {
             // ... your existing logic ...
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("java", "phyton", "script", "scripts");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("script")) {
            return Arrays.asList("run", "reload");
        }
        return new ArrayList<>();
    }
}