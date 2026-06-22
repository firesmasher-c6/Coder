package me.coder.manager;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;

public class ConfigManager {
    
    private final Plugin plugin;
    private final File configFile;
    private FileConfiguration config;
    private long lastModified;
    
    public ConfigManager(Plugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
        loadConfig();
    }
    
    /**
     * Load or reload the configuration from file
     */
    public void loadConfig() {
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        this.lastModified = configFile.lastModified();
    }
    
    /**
     * Check if config file has been modified and reload if needed
     */
    public void checkAndReload() {
        if (configFile.lastModified() > lastModified) {
            loadConfig();
            plugin.getLogger().info("[Coder] Config reloaded from disk");
        }
    }
    
    /**
     * Get the current config
     */
    public FileConfiguration getConfig() {
        return config;
    }
    
    /**
     * Check if plugin is enabled in config
     */
    public boolean isPluginEnabled() {
        return config.getBoolean("plugin.enabled", true);
    }
    
    /**
     * Check if Python scripts are enabled
     */
    public boolean isPythonEnabled() {
        return config.getBoolean("scripts.python-enabled", true);
    }
    
    /**
     * Check if Lua scripts are enabled
     */
    public boolean isLuaEnabled() {
        return config.getBoolean("scripts.lua-enabled", true);
    }
    
    /**
     * Check if Java scripts are enabled
     */
    public boolean isJavaEnabled() {
        return config.getBoolean("scripts.java-enabled", true);
    }
    
    /**
     * Check if Coder Addons are enabled
     */
    public boolean areAddonsEnabled() {
        return config.getBoolean("enabled", true);
    }
    
    /**
     * Get the blocked addon message
     */
    public String getBlockedAddonMessage() {
        return config.getString("blocked-addon-message", "[Coder] This Coder Addon Has Been Blocked. If This is a mistake please read your code.");
    }
    
    /**
     * Save config to file
     */
    public void saveConfig() {
        try {
            config.save(configFile);
            this.lastModified = configFile.lastModified();
        } catch (IOException e) {
            plugin.getLogger().severe("[Coder] Failed to save config: " + e.getMessage());
        }
    }
}