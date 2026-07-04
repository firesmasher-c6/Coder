package me.coder.api;

import org.bukkit.command.CommandSender;
import java.io.File;

/**
 * Custom Python/Lua execution handler
 * Override default script interpreter behavior
 */
public interface ScriptExecutionHandler {
    
    /**
     * Execute a script file
     */
    boolean execute(File scriptFile, CommandSender executor);
    
    /**
     * Execute script from string
     */
    boolean executeString(String scriptContent, CommandSender executor);
    
    /**
     * Get language (python, lua)
     */
    String getLanguage();
    
    /**
     * Get handler name
     */
    String getHandlerName();
    
    /**
     * Initialize interpreter
     */
    void initialize();
    
    /**
     * Cleanup resources
     */
    void cleanup();
    
    /**
     * Check if available
     */
    boolean isAvailable();
    
    /**
     * Get version
     */
    String getVersion();
}
