package me.coder.api;

import org.bukkit.command.CommandSender;
import java.io.File;

/**
 * Custom Java execution handler
 * Override default Java script compilation and execution
 */
public interface JavaExecutionHandler {
    
    /**
     * Compile and execute Java file
     */
    boolean compileAndExecute(File javaFile, CommandSender executor);
    
    /**
     * Compile and load Java file
     */
    boolean compileAndLoad(File javaFile, CommandSender executor);
    
    /**
     * Load a class by name
     */
    boolean loadClass(String className, CommandSender executor);
    
    /**
     * Unload a class
     */
    void unloadClass(String className, CommandSender executor);
    
    /**
     * List loaded classes
     */
    void listLoadedClasses(CommandSender executor);
    
    /**
     * Check if class is loaded
     */
    boolean isClassLoaded(String className);
    
    /**
     * Get loaded class
     */
    Class<?> getLoadedClass(String className);
    
    /**
     * Clear cache
     */
    void clearCache();
    
    /**
     * Get handler name
     */
    String getHandlerName();
}
