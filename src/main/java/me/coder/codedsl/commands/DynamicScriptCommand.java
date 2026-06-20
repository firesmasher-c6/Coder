package me.coder.codedsl.commands;

import me.coder.api.CoderAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Custom runtime command wrapper for dynamically generated CodeDSL scripts.
 */
public class DynamicScriptCommand extends BukkitCommand {

    private final BiConsumer<CommandSender, String[]> executor;

    public DynamicScriptCommand(String name, String description, String usage, List<String> aliases, BiConsumer<CommandSender, String[]> executor) {
        super(name);
        this.setDescription(description);
        this.setUsage(usage);
        this.setAliases(aliases);
        this.executor = executor;
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        try {
            executor.accept(sender, args);
        } catch (Exception e) {
            sender.sendMessage("§cAn internal error occurred while executing this script command.");
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Programmatically registers a dynamic script syntax command straight into the server core.
     */
    public static void register(Plugin plugin, String commandName, List<String> aliases, String permission, BiConsumer<CommandSender, String[]> executor, CoderAPI api) {
        try {
            var server = Bukkit.getServer();
            var commandMapField = server.getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            var map = (SimpleCommandMap) commandMapField.get(server);

            if (commandName.startsWith("/")) {
                commandName = commandName.substring(1);
            }

            // Isolate base label if custom syntax contains arguments like [<text>] 
            if (commandName.contains(" ")) {
                commandName = commandName.split(" ")[0];
            }
            
            commandName = commandName.replace(":", "").trim();

            DynamicScriptCommand cmd = new DynamicScriptCommand(
                "codedsl:" + commandName,
                "CodeDSL Dynamic Command",
                "/codedsl:" + commandName,
                aliases != null ? aliases : new ArrayList<>(),
                executor
            );

            if (permission != null && !permission.isEmpty()) {
                cmd.setPermission(permission);
            }

            map.register("codedsl", cmd);

            // Force Paper engine to refresh command sheets for all online players immediately
            try {
                var syncMethod = server.getClass().getDeclaredMethod("syncCommands");
                syncMethod.setAccessible(true);
                syncMethod.invoke(server);
            } catch (Exception ignored) {}

            if (api != null) {
                api.log("Successfully registered script command: /codedsl:" + commandName);
            }
        } catch (Exception e) {
            if (api != null) {
                api.logError("Failed to reflectively inject script command /codedsl:" + commandName + ": " + e.getMessage());
            }
            e.printStackTrace();
        }
    }
}