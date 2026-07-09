package me.coder.commands;

import me.coder.expansion.ExpansionManager;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ExpansionCommandHandler implements TabCompleter {
    
    private final ExpansionManager expansionManager;
    
    public ExpansionCommandHandler(ExpansionManager expansionManager) {
        this.expansionManager = expansionManager;
    }
    
    public boolean handleCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            showExpansionHelp(sender);
            return true;
        }
        
        String subcommand = args[1].toLowerCase();
        
        if (subcommand.equals("list")) {
            handleListCommand(sender);
            return true;
        }
        
        if (subcommand.equals("disable")) {
            if (args.length < 3) {
                sender.sendMessage("§cUsage: /coder expansion disable <expansion_name>");
                return true;
            }
            handleDisableCommand(sender, args[2]);
            return true;
        }
        
        if (subcommand.equals("enable")) {
            if (args.length < 3) {
                sender.sendMessage("§cUsage: /coder expansion enable <expansion_name>");
                return true;
            }
            handleEnableCommand(sender, args[2]);
            return true;
        }
        
        if (subcommand.equals("load")) {
            if (args.length < 3) {
                sender.sendMessage("§cUsage: /coder expansion load <expansion_name>");
                return true;
            }
            handleLoadCommand(sender, args[2]);
            return true;
        }
        
        showExpansionHelp(sender);
        return true;
    }
    
    private void handleListCommand(CommandSender sender) {
        Set<String> loaded = expansionManager.getLoadedExpansionNames();
        Set<String> disabled = expansionManager.getDisabledExpansionNames();
        Set<String> available = expansionManager.getAvailableExpansionNames();
        
        sender.sendMessage("§f═══════════════════════════════════");
        sender.sendMessage("§f[Coder Expansions]");
        sender.sendMessage("§f═══════════════════════════════════");
        
        if (available.isEmpty()) {
            sender.sendMessage("§eNo expansions found.");
        } else {
            sender.sendMessage("§aLoaded Expansions: §f" + loaded.size());
            for (String name : loaded) {
                sender.sendMessage("  §a✓ §f" + name);
            }
            
            if (!disabled.isEmpty()) {
                sender.sendMessage("§cDisabled Expansions: §f" + disabled.size());
                for (String name : disabled) {
                    sender.sendMessage("  §c✗ §f" + name);
                }
            }
        }
        
        sender.sendMessage("§f═══════════════════════════════════");
    }
    
    private void handleDisableCommand(CommandSender sender, String expansionName) {
        if (!expansionManager.isExpansionLoaded(expansionName)) {
            sender.sendMessage("§cExpansion '" + expansionName + "' is not loaded.");
            return;
        }
        
        expansionManager.disableExpansion(expansionName);
        sender.sendMessage("§aDisabled expansion: §f" + expansionName);
    }
    
    private void handleEnableCommand(CommandSender sender, String expansionName) {
        if (expansionManager.isExpansionLoaded(expansionName)) {
            sender.sendMessage("§cExpansion '" + expansionName + "' is already enabled.");
            return;
        }
        
        expansionManager.enableExpansion(expansionName);
        sender.sendMessage("§aEnabled expansion: §f" + expansionName);
    }
    
    private void handleLoadCommand(CommandSender sender, String expansionName) {
        if (expansionManager.isExpansionLoaded(expansionName)) {
            sender.sendMessage("§cExpansion '" + expansionName + "' is already loaded.");
            return;
        }
        
        File jsZipFile = new File(expansionManager.getExpansionsDirectory(), expansionName + ".jszip");
        if (!jsZipFile.exists()) {
            sender.sendMessage("§cExpansion file '" + expansionName + ".jszip' not found.");
            return;
        }
        
        expansionManager.loadExpansion(jsZipFile);
        
        if (expansionManager.isExpansionLoaded(expansionName)) {
            sender.sendMessage("§aLoaded expansion: §f" + expansionName);
        } else {
            sender.sendMessage("§cFailed to load expansion: §f" + expansionName);
        }
    }
    
    private void showExpansionHelp(CommandSender sender) {
        sender.sendMessage("§f═══════════════════════════════════");
        sender.sendMessage("§f[Coder Expansion Commands]");
        sender.sendMessage("§f═══════════════════════════════════");
        sender.sendMessage("§a/coder expansion list §f- List all expansions");
        sender.sendMessage("§a/coder expansion enable <name> §f- Enable an expansion");
        sender.sendMessage("§a/coder expansion disable <name> §f- Disable an expansion");
        sender.sendMessage("§a/coder expansion load <name> §f- Load an inactive expansion");
        sender.sendMessage("§f═══════════════════════════════════");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command command, 
                                      String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 2) {
            completions.add("list");
            completions.add("enable");
            completions.add("disable");
            completions.add("load");
        } else if (args.length == 3) {
            String subcommand = args[1].toLowerCase();
            
            if (subcommand.equals("enable")) {
                Set<String> disabled = expansionManager.getDisabledExpansionNames();
                completions.addAll(disabled);
            } else if (subcommand.equals("disable")) {
                Set<String> loaded = expansionManager.getLoadedExpansionNames();
                completions.addAll(loaded);
            } else if (subcommand.equals("load")) {
                Set<String> inactive = expansionManager.getInactiveExpansionNames();
                completions.addAll(inactive);
            }
        }
        
        return completions;
    }
}