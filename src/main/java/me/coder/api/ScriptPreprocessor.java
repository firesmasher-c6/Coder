package me.coder.api;

/**
 * Script preprocessor
 * Modify script content before execution
 */
public interface ScriptPreprocessor {
    
    /**
     * Process script before execution
     * Return null to cancel execution
     */
    String process(String scriptName, String content);
    
    /**
     * Get preprocessor name
     */
    String getName();
    
    /**
     * Check if should process
     */
    default boolean shouldProcess(String scriptName) {
        return true;
    }
}
