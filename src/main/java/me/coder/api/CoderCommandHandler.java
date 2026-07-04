package me.coder.api;

import org.bukkit.command.CommandSender;
import java.util.List;

/**
 * Handler for custom /coder subcommands
 * Allows addons to register custom /coder commands
 */
public interface CoderCommandHandler {
    
    /**
     * Execute the command
     */
    boolean execute(CommandSender sender, String[] args);
    
    /**
     * Get command name
     */
    String getCommandName();
    
    /**
     * Get command description
     */
    String getDescription();
    
    /**
     * Get command usage
     */
    String getUsage();
    
    /**
     * Get permission required
     */
    default String getPermission() {
        return "coder.admin";
    }
    
    /**
     * Get tab completions
     */
    default List<String> getTabCompletions(CommandSender sender, String[] args) {
        return new java.util.ArrayList<>();
    }
}