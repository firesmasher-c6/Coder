package me.coder.api;

/**
 * Event listener for Coder events
 * Listen to script execution and plugin events
 */
public interface CoderEventListener {
    
    /**
     * Called when script starts executing
     */
    default void onScriptStart(String scriptName, String language) {
    }
    
    /**
     * Called when script finishes executing
     */
    default void onScriptEnd(String scriptName, String language, boolean success) {
    }
    
    /**
     * Called on script error
     */
    default void onScriptError(String scriptName, String language, Throwable error) {
    }
    
    /**
     * Called when addon loads
     */
    default void onAddonLoad(String addonName) {
    }
    
    /**
     * Called when addon unloads
     */
    default void onAddonUnload(String addonName) {
    }
    
    /**
     * Called when /coder command is executed
     */
    default void onCommandExecute(String subcommand, String[] args) {
    }
    
    /**
     * Get listener name
     */
    String getName();
}
