package me.coder.api;

// Interface for creating Coder Addons
public interface CoderAddon {
    
    // Get addon name
    String getName();
    
    // Get addon version
    String getVersion();
    
    // Get addon author
    String getAuthor();
    
    // Called when addon is enabled
    void onEnable();
    
    // Called when addon is disabled
    void onDisable();
    
    // Get addon description (optional)
    default String getDescription() {
        return "A Coder addon";
    }
}