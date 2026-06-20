package me.coder.codedsl.listeners;

import me.coder.api.CoderAPI;
import me.coder.codedsl.CodeDSLAddon;
import me.coder.codedsl.CodeDSLProcessor;
import me.coder.codedsl.manager.ScriptManager;
import me.coder.codedsl.parser.CommandParser;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

/**
 * Intercepts commands to route:
 * 1. /coder run <file> or /code run <file> - Execute FULL script
 * 2. /nan (registered command) - Execute ONLY command block from script
 */
public class CommandInterceptor implements Listener {

    private final CoderAPI api;
    private final CommandParser commandParser;
    private final ScriptManager scriptManager;
    private final CodeDSLProcessor processor;
    private final CodeDSLAddon addon;

    public CommandInterceptor(CoderAPI api, CommandParser commandParser, ScriptManager scriptManager, CodeDSLProcessor processor, CodeDSLAddon addon) {
        this.api = api;
        this.commandParser = commandParser;
        this.scriptManager = scriptManager;
        this.processor = processor;
        this.addon = addon;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        
        // 1. Handle /coder run <file> or /code run <file> - Execute FULL script
        if (shouldRedirectCoderRun(message)) {
            event.setCancelled(true);
            executeCoderRun(event.getPlayer(), message);
            return;
        }
        
        // 2. Check if this is a REGISTERED CodeDSL command (execute only command block)
        String commandLabel = extractCommandLabel(message);
        if (scriptManager.hasCommandHandler(commandLabel)) {
            event.setCancelled(true);
            scriptManager.executeCommandHandler(commandLabel, event.getPlayer());
            return;
        }
        
        // 3. Otherwise check if this is a script file execution via CommandParser
        if (commandParser.isCodeDSLCommand(message)) {
            commandParser.processCommand(event.getPlayer(), message, processor);
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerCommand(ServerCommandEvent event) {
        String command = event.getCommand();
        
        // 1. Handle /coder run <file> or /code run <file> - Execute FULL script
        if (shouldRedirectCoderRun(command)) {
            event.setCancelled(true);
            executeCoderRun(event.getSender(), command);
            return;
        }
        
        // 2. Check if this is a REGISTERED CodeDSL command (execute only command block)
        String commandLabel = extractCommandLabel(command);
        if (scriptManager.hasCommandHandler(commandLabel)) {
            event.setCancelled(true);
            scriptManager.executeCommandHandler(commandLabel, event.getSender());
            return;
        }
        
        // 3. Otherwise check if this is a script file execution via CommandParser
        if (commandParser.isCodeDSLCommand(command)) {
            commandParser.processCommand(event.getSender(), command, processor);
            event.setCancelled(true); 
            event.setCommand(""); 
        }
    }
    
    /**
     * Extract the base command label from a full command string
     */
    private String extractCommandLabel(String rawCommand) {
        String clean = rawCommand.trim();
        if (clean.startsWith("/")) {
            clean = clean.substring(1);
        }
        String[] parts = clean.split("\\s+");
        return parts.length > 0 ? parts[0].toLowerCase() : "";
    }

    /**
     * Check if command matches /coder run or /code run pattern
     */
    private boolean shouldRedirectCoderRun(String rawCommand) {
        String lower = rawCommand.toLowerCase().trim();
        if (lower.startsWith("/")) {
            lower = lower.substring(1);
        }

        // Only intercept /coder run or /code run
        if (!(lower.startsWith("coder run ") || lower.startsWith("code run "))) {
            return false;
        }

        String[] splitArgs = lower.split("\\s+");
        if (splitArgs.length < 3) {
            return false;
        }

        String fileTarget = splitArgs[2].toLowerCase();
        
        YamlConfiguration config = addon.getAddonConfig();
        if (config == null) return fileTarget.endsWith(".cd") || fileTarget.endsWith(".code");

        String mainExt = config.getString("file-extensions.main", ".cd").toLowerCase();
        String legacyExt = config.getString("file-extensions.legacy", ".code").toLowerCase();
        String oldExt = config.getString("file-extensions.old", ".cdsl").toLowerCase();
        String customExt = config.getString("file-extensions.custom", "").toLowerCase();

        if (fileTarget.endsWith(mainExt) || fileTarget.endsWith(legacyExt) || fileTarget.endsWith(oldExt)) {
            return true;
        }

        if (!customExt.isEmpty() 
            && !customExt.equals("your_prefered_custom_file_extension_here") 
            && customExt.startsWith(".")) {
            return fileTarget.endsWith(customExt);
        }

        return false;
    }

    /**
     * Execute /coder run <file> or /code run <file> - runs FULL script file
     */
    private void executeCoderRun(CommandSender sender, String originalCommand) {
        String cleanCommand = originalCommand.trim();
        if (cleanCommand.startsWith("/")) {
            cleanCommand = cleanCommand.substring(1);
        }

        String[] splitArgs = cleanCommand.split("\\s+");
        if (splitArgs.length >= 3) {
            String targetFileName = splitArgs[2];
            
            if (api != null) {
                api.log("[CodeDSL] Running script file: " + targetFileName);
            }
            
            // Execute FULL script (NOT just registering commands)
            scriptManager.runScript(targetFileName, sender);
        }
    }
}