package me.coder.api;

import org.bukkit.command.CommandSender;

/**
 * Script postprocessor
 * Handle script execution results
 */
public interface ScriptPostprocessor {
    
    /**
     * Process result after script execution
     */
    void processResult(String scriptName, CommandSender executor, boolean success, String output, Throwable error);
    
    /**
     * Get postprocessor name
     */
    String getName();
    
    /**
     * Check if should process
     */
    default boolean shouldProcess(String scriptName) {
        return true;
    }
}
