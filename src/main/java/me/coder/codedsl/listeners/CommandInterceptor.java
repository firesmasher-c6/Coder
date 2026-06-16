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
 * Intercepts commands to handle CodeDSL command registrations and cross-plugin /coder & /code redirects.
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
        
        // 1. Intercept /coder run or /code run paths and redirect them to loadScript
        if (shouldRedirectCoderRun(message)) {
            event.setCancelled(true);
            executeRedirect(event.getPlayer(), message);
            return;
        }
        
        // 2. Check if this is a standard CodeDSL script-registered command
        if (commandParser.isCodeDSLCommand(message)) {
            commandParser.processCommand(event.getPlayer(), message, processor);
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerCommand(ServerCommandEvent event) {
        String command = event.getCommand();
        
        // 1. Intercept /coder run or /code run console commands and redirect them to loadScript
        if (shouldRedirectCoderRun(command)) {
            event.setCancelled(true);
            executeRedirect(event.getSender(), command);
            return;
        }
        
        // 2. Check if this is a standard CodeDSL script-registered command
        if (commandParser.isCodeDSLCommand(command)) {
            commandParser.processCommand(event.getSender(), command, processor);
            // Fix: Cancel the event so the server doesn't try to parse the unknown command
            event.setCancelled(true); 
            event.setCommand(""); 
        }
    }

    /**
     * Helper to verify if the command pattern matches '/coder run ' or '/code run ' 
     * targeting a configured file extension.
     */
    private boolean shouldRedirectCoderRun(String rawCommand) {
        String lower = rawCommand.toLowerCase().trim();
        if (lower.startsWith("/")) {
            lower = lower.substring(1);
        }

        if (lower.startsWith("coder run ") || lower.startsWith("code run ")) {
            String[] splitArgs = lower.split("\\s+");
            if (splitArgs.length >= 3) {
                String fileTarget = splitArgs[2].toLowerCase();
                
                YamlConfiguration config = addon.getAddonConfig();
                if (config == null) return fileTarget.endsWith(".cd") || fileTarget.endsWith(".code");

                // Pull configuration definitions
                String mainExt = config.getString("file-extensions.main", ".cd").toLowerCase();
                String legacyExt = config.getString("file-extensions.legacy", ".code").toLowerCase();
                String oldExt = config.getString("file-extensions.old", ".cdsl").toLowerCase();
                String customExt = config.getString("file-extensions.custom", "").toLowerCase();

                // 1. Match core extensions
                if (fileTarget.endsWith(mainExt) || fileTarget.endsWith(legacyExt) || fileTarget.endsWith(oldExt)) {
                    return true;
                }

                // 2. Safely match custom extension slot if it passes guards
                if (!customExt.isEmpty() 
                    && !customExt.equals("your_prefered_custom_file_extension_here") 
                    && customExt.startsWith(".")) {
                    return fileTarget.endsWith(customExt);
                }
            }
        }
        return false;
    }

    /**
     * Isolates the file name element out of the command args string and runs the redirection logic.
     */
    private void executeRedirect(CommandSender sender, String originalCommand) {
        String cleanCommand = originalCommand.trim();
        if (cleanCommand.startsWith("/")) {
            cleanCommand = cleanCommand.substring(1);
        }

        String[] splitArgs = cleanCommand.split("\\s+");
        if (splitArgs.length >= 3) {
            String targetFileName = splitArgs[2];
            
            if (api != null) {
                api.sendSuccess(sender, "[CodeDSL] Intercepted run command alias -> Redirecting file loading path...");
            } else {
                sender.sendMessage("§7[CodeDSL] Intercepted run command alias -> Redirecting file loading path...");
            }
            
            scriptManager.loadScript(targetFileName, sender);
        }
    }
}