package me.coder.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class BukkitInteractionHandler {
    private final JavaPlugin plugin;
    private final Map<String, Class<?>> loadedClasses;
    private URLClassLoader classLoader;

    public BukkitInteractionHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        this.loadedClasses = new HashMap<>();
        
        initializeClassLoader();
    }

    /**
     * Initialize the URLClassLoader for loading .class files
     */
    private void initializeClassLoader() {
        try {
            File classesDirectory = new File(plugin.getDataFolder(), "JavaClasses");
            URL[] urls = {classesDirectory.toURI().toURL()};
            classLoader = new URLClassLoader(urls, getClass().getClassLoader());
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize class loader: " + e.getMessage());
        }
    }

    /**
     * Load a Java class file and make it available for Bukkit interaction
     */
    public boolean loadClassFile(File classFile) {
        if (!classFile.exists() || !classFile.getName().endsWith(".class")) {
            return false;
        }

        try {
            String className = classFile.getName().replace(".class", "");
            Class<?> clazz = classLoader.loadClass(className);
            
            loadedClasses.put(className, clazz);
            plugin.getLogger().info("Loaded class for Bukkit interaction: " + className);
            
            return true;
        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("Failed to load class: " + e.getMessage());
            return false;
        }
    }

    /**
     * Execute a method from a loaded class that interacts with Bukkit
     */
    public Object executeClassMethod(String className, String methodName, Object... args) {
        Class<?> clazz = loadedClasses.get(className);
        
        if (clazz == null) {
            plugin.getLogger().warning("Class not loaded: " + className);
            return null;
        }

        try {
            // Find the method with matching name and argument types
            Method[] methods = clazz.getDeclaredMethods();
            Method targetMethod = null;
            
            for (Method method : methods) {
                if (method.getName().equals(methodName)) {
                    targetMethod = method;
                    break;
                }
            }
            
            if (targetMethod == null) {
                plugin.getLogger().warning("Method not found: " + methodName + " in class " + className);
                return null;
            }
            
            targetMethod.setAccessible(true);
            
            // Create instance and execute
            Object instance = clazz.getDeclaredConstructor().newInstance();
            Object result = targetMethod.invoke(instance, args);
            
            plugin.getLogger().info("Executed method: " + className + "." + methodName);
            
            return result;
            
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to execute class method: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Execute a static method from a loaded class
     */
    public Object executeStaticMethod(String className, String methodName, Object... args) {
        Class<?> clazz = loadedClasses.get(className);
        
        if (clazz == null) {
            plugin.getLogger().warning("Class not loaded: " + className);
            return null;
        }

        try {
            Method[] methods = clazz.getDeclaredMethods();
            Method targetMethod = null;
            
            for (Method method : methods) {
                if (method.getName().equals(methodName)) {
                    targetMethod = method;
                    break;
                }
            }
            
            if (targetMethod == null) {
                plugin.getLogger().warning("Static method not found: " + methodName);
                return null;
            }
            
            targetMethod.setAccessible(true);
            Object result = targetMethod.invoke(null, args);
            
            plugin.getLogger().info("Executed static method: " + className + "." + methodName);
            
            return result;
            
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to execute static method: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Broadcast a message to all players via Bukkit
     */
    public void broadcastMessage(String message) {
        Bukkit.getServer().broadcastMessage(message);
    }

    /**
     * Send a message to a specific player
     */
    public void sendPlayerMessage(String playerName, String message) {
        Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            player.sendMessage(message);
        }
    }

    /**
     * Execute a Bukkit command
     */
    public void executeCommand(String command) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    /**
     * Get a loaded class
     */
    public Class<?> getLoadedClass(String className) {
        return loadedClasses.get(className);
    }

    /**
     * Get all loaded classes
     */
    public Map<String, Class<?>> getAllLoadedClasses() {
        return new HashMap<>(loadedClasses);
    }

    /**
     * Unload a class
     */
    public boolean unloadClass(String className) {
        if (loadedClasses.containsKey(className)) {
            loadedClasses.remove(className);
            plugin.getLogger().info("Unloaded class: " + className);
            return true;
        }
        return false;
    }

    /**
     * Check if a class is loaded
     */
    public boolean isClassLoaded(String className) {
        return loadedClasses.containsKey(className);
    }

    /**
     * Get the number of loaded classes
     */
    public int getLoadedClassCount() {
        return loadedClasses.size();
    }

    /**
     * Cleanup and close the class loader
     */
    public void cleanup() {
        try {
            loadedClasses.clear();
            if (classLoader != null) {
                classLoader.close();
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error during cleanup: " + e.getMessage());
        }
    }
}