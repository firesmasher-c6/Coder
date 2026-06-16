package me.coder.codedsl.commands;

import me.coder.api.CoderAPI;
import me.coder.codedsl.manager.ScriptManager;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.*;

/**
 * Handles CodeDSL commands: /codedsl, /cdsl, /code-dsl
 */
public class CodeDSLCommand implements CommandExecutor, TabCompleter {

    private final ScriptManager scriptManager;
    private final CoderAPI api;
    private final File dataFolder;

    public CodeDSLCommand(ScriptManager scriptManager, CoderAPI api, File dataFolder) {
        this.scriptManager = scriptManager;
        this.api = api;
        this.dataFolder = dataFolder;
    }

    /**
     * Programmatically registers the command into Bukkit's SimpleCommandMap 
     * without needing configuration entries inside paper-plugin.yml.
     */
    public static void register(Plugin plugin, ScriptManager scriptManager, CoderAPI api, File dataFolder) {
        try {
            var server = Bukkit.getServer();
            var commandMapField = server.getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            var map = (SimpleCommandMap) commandMapField.get(server);

            // Access PluginCommand constructor reflectively
            var constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            constructor.setAccessible(true);
            
            PluginCommand cmd = constructor.newInstance("codedsl", plugin);
            cmd.setAliases(List.of("cdsl", "code-dsl"));
            cmd.setDescription("Execute and manage CodeDSL scripts");
            cmd.setUsage("/codedsl <run|reload|load|unload|list> <filename>");

            CodeDSLCommand handler = new CodeDSLCommand(scriptManager, api, dataFolder);
            cmd.setExecutor(handler);
            cmd.setTabCompleter(handler);

            // Register directly into the live server command map fallback namespace
            map.register("codedsl", cmd);
            api.log("CodeDSL commands programmatically registered into Bukkit engine.");
        } catch (Exception e) {
            api.logError("Failed to reflectively register commands: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§6========== CodeDSL Help ==========");
            sender.sendMessage("§a/codedsl run <file> §7- Execute a CodeDSL script");
            sender.sendMessage("§a/codedsl reload <file> §7- Reload a script");
            sender.sendMessage("§a/codedsl load <file> §7- Load script to memory");
            sender.sendMessage("§a/codedsl unload <file> §7- Unload script from memory");
            sender.sendMessage("§a/codedsl list        §7- List all local script files");
            sender.sendMessage("§6==================================");
            return true;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "run":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /codedsl run <filename>");
                    return true;
                }
                scriptManager.runScript(args[1], sender);
                break;

            case "reload":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /codedsl reload <filename>");
                    return true;
                }
                scriptManager.reloadScript(args[1], sender);
                break;

            case "load":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /codedsl load <filename>");
                    return true;
                }
                scriptManager.loadScript(args[1], sender);
                break;

            case "unload":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /codedsl unload <filename>");
                    return true;
                }
                scriptManager.unloadScript(args[1], sender);
                break;

            case "list":
                listScripts(sender);
                break;

            default:
                sender.sendMessage("§cUnknown command. Use /codedsl");
        }

        return true;
    }

    /**
     * List all available scripts
     */
    private void listScripts(CommandSender sender) {
        File scriptsDir = new File(dataFolder, "scripts");
        if (!scriptsDir.exists()) {
            sender.sendMessage("§cNo scripts directory found");
            return;
        }

        File[] files = scriptsDir.listFiles((dir, name) -> name.endsWith(".cd") || name.endsWith(".code"));
        
        if (files == null || files.length == 0) {
            sender.sendMessage("§c[CodeDSL] No scripts found");
            return;
        }

        sender.sendMessage("§6========= CodeDSL Scripts =========");
        for (File file : files) {
            sender.sendMessage("§a• " + file.getName() + " §7(" + file.length() + " bytes)");
        }
        sender.sendMessage("§6====================================");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("run", "reload", "load", "unload", "list");
        }

        if (args.length == 2 && isValidAction(args[0])) {
            List<String> scripts = new ArrayList<>();
            File scriptsDir = new File(dataFolder, "scripts");

            if (scriptsDir.exists() && scriptsDir.isDirectory()) {
                File[] files = scriptsDir.listFiles((dir, name) -> name.endsWith(".cd") || name.endsWith(".code"));
                if (files != null) {
                    for (File file : files) {
                        scripts.add(file.getName());
                    }
                }
            }
            return scripts;
        }

        return new ArrayList<>();
    }

    private boolean isValidAction(String action) {
        return action.equalsIgnoreCase("run") || action.equalsIgnoreCase("reload") ||
               action.equalsIgnoreCase("load") || action.equalsIgnoreCase("unload");
    }
}