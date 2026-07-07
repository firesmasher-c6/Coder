package me.coder.manager;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class ScriptMemoryLoader {
    private final JavaPlugin plugin;
    private final File loadedScriptsDir;
    private final Map<String, LoadedScript> activeScripts;
    private BukkitTask memoryMaintenanceTask;

    public ScriptMemoryLoader(JavaPlugin plugin) {
        this.plugin = plugin;
        this.loadedScriptsDir = new File(plugin.getDataFolder(), "JavaClasses/Loaded");
        this.activeScripts = new HashMap<>();
        
        ensureDirectory();
        startMemoryMaintenance();
    }

    private void ensureDirectory() {
        if (!loadedScriptsDir.exists()) {
            loadedScriptsDir.mkdirs();
        }
    }

    /**
     * Start the memory maintenance task that keeps scripts active
     */
    private void startMemoryMaintenance() {
        memoryMaintenanceTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(
            plugin,
            this::maintainMemory,
            20L, // Initial delay - 1 second
            200L // Repeat every 10 seconds
        );
    }

    /**
     * Maintain active scripts in memory
     */
    private void maintainMemory() {
        try {
            File[] scripts = loadedScriptsDir.listFiles((dir, name) -> name.endsWith(".class"));
            
            if (scripts == null) {
                return;
            }

            for (File scriptFile : scripts) {
                String scriptName = scriptFile.getName();
                
                // Load new scripts
                if (!activeScripts.containsKey(scriptName)) {
                    loadScriptToMemory(scriptFile);
                }
                
                // Update existing scripts if modified
                LoadedScript loadedScript = activeScripts.get(scriptName);
                if (loadedScript != null && loadedScript.isModified(scriptFile)) {
                    reloadScriptToMemory(scriptFile);
                }
            }

            // Remove scripts that no longer exist
            activeScripts.entrySet().removeIf(entry -> 
                !new File(loadedScriptsDir, entry.getKey()).exists()
            );

        } catch (Exception e) {
            plugin.getLogger().severe("Error maintaining script memory: " + e.getMessage());
        }
    }

    /**
     * Load a script to active memory
     */
    private void loadScriptToMemory(File scriptFile) {
        try {
            byte[] scriptData = Files.readAllBytes(scriptFile.toPath());
            LoadedScript loadedScript = new LoadedScript(
                scriptFile.getName(),
                scriptData,
                scriptFile.lastModified()
            );
            
            activeScripts.put(scriptFile.getName(), loadedScript);
            plugin.getLogger().info("Loaded script to memory: " + scriptFile.getName());
            
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to load script to memory: " + e.getMessage());
        }
    }

    /**
     * Reload a script that has been modified
     */
    private void reloadScriptToMemory(File scriptFile) {
        try {
            byte[] scriptData = Files.readAllBytes(scriptFile.toPath());
            LoadedScript loadedScript = activeScripts.get(scriptFile.getName());
            
            if (loadedScript != null) {
                loadedScript.updateData(scriptData, scriptFile.lastModified());
                plugin.getLogger().info("Reloaded script in memory: " + scriptFile.getName());
            }
            
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to reload script: " + e.getMessage());
        }
    }

    /**
     * Get a loaded script from memory
     */
    public LoadedScript getLoadedScript(String scriptName) {
        return activeScripts.get(scriptName);
    }

    /**
     * Get all active scripts
     */
    public Map<String, LoadedScript> getActiveScripts() {
        return new HashMap<>(activeScripts);
    }

    /**
     * Check if a script is loaded in memory
     */
    public boolean isScriptLoaded(String scriptName) {
        return activeScripts.containsKey(scriptName);
    }

    /**
     * Get memory usage info
     */
    public String getMemoryStats() {
        long totalSize = activeScripts.values().stream()
            .mapToLong(LoadedScript::getSize)
            .sum();
        
        return String.format("Active Scripts: %d | Memory: %d bytes", 
            activeScripts.size(), totalSize);
    }

    /**
     * Stop the memory maintenance task
     */
    public void stop() {
        if (memoryMaintenanceTask != null) {
            memoryMaintenanceTask.cancel();
        }
        activeScripts.clear();
    }

    /**
     * Inner class representing a loaded script in memory
     */
    public static class LoadedScript {
        private String name;
        private byte[] data;
        private long lastModified;
        private long loadedTime;

        public LoadedScript(String name, byte[] data, long lastModified) {
            this.name = name;
            this.data = data;
            this.lastModified = lastModified;
            this.loadedTime = System.currentTimeMillis();
        }

        public void updateData(byte[] newData, long newLastModified) {
            this.data = newData;
            this.lastModified = newLastModified;
            this.loadedTime = System.currentTimeMillis();
        }

        public String getName() {
            return name;
        }

        public byte[] getData() {
            return data;
        }

        public long getLastModified() {
            return lastModified;
        }

        public long getSize() {
            return data.length;
        }

        public long getLoadedTime() {
            return loadedTime;
        }

        /**
         * Check if the file on disk has been modified
         */
        public boolean isModified(File file) {
            return file.lastModified() != this.lastModified;
        }
    }
}