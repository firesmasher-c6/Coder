package me.coder.api;

/**
 * Interface for creating Coder Addons
 * 
 * Addons can extend Coder functionality with custom commands, events, and features.
 * 
 * Example Addon Implementation:
 * 
 * public class MyAddon implements CoderAddon {
 *     @Override
 *     public String getName() {
 *         return "MyAddon";
 *     }
 *     
 *     @Override
 *     public String getVersion() {
 *         return "1.0.0";
 *     }
 *     
 *     @Override
 *     public String getAuthor() {
 *         return "Your Name";
 *     }
 *     
 *     @Override
 *     public void onEnable() {
 *         CoderAPI.getInstance().log("MyAddon enabled!");
 *     }
 *     
 *     @Override
 *     public void onDisable() {
 *         CoderAPI.getInstance().log("MyAddon disabled!");
 *     }
 * }
 */
public interface CoderAddon {
    
    /**
     * Get the addon name
     * @return The addon name
     */
    String getName();
    
    /**
     * Get the addon version
     * @return The addon version (e.g., "1.0.0")
     */
    String getVersion();
    
    /**
     * Get the addon author
     * @return The addon author name
     */
    String getAuthor();
    
    /**
     * Called when the addon is enabled
     * Initialize your addon here
     */
    void onEnable();
    
    /**
     * Called when the addon is disabled
     * Clean up your addon here
     */
    void onDisable();
    
    /**
     * Get the addon description
     * @return A short description of what this addon does
     */
    default String getDescription() {
        return "A Coder addon";
    }
}