package me.coder.api;

import org.bukkit.command.CommandSender;
import java.util.List;

/**
 * Tab completer for /coder command
 * Provide custom tab completion suggestions
 */
public interface CoderTabCompleter {
    
    /**
     * Get tab completion suggestions
     */
    List<String> getCompletions(CommandSender sender, String[] args);
    
    /**
     * Get completer name
     */
    String getName();
    
    /**
     * Check if completer applies to this context
     */
    default boolean applies(CommandSender sender, String[] args) {
        return true;
    }
}
