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
     * Check if a specific language is enabled
     * @param language The language name (lua, python, java)
     * @return true if enabled
     */
    public boolean isLanguageEnabled(String language) {
        return config.getBoolean("plugin.languages." + language.toLowerCase(), true);
    }
    
    /**
     * Check if Python scripts are enabled
     */
    public boolean isPythonEnabled() {
        return isLanguageEnabled("python");
    }
    
    /**
     * Check if Lua scripts are enabled
     */
    public boolean isLuaEnabled() {
        return isLanguageEnabled("lua");
    }
    
    /**
     * Check if Java scripts are enabled
     */
    public boolean isJavaEnabled() {
        return isLanguageEnabled("java");
    }
    
    /**
     * Check if a specific command is enabled
     * @param commandName The command name (run, load, unload, update, update-jar, reload, confirm, cancel)
     * @return true if enabled
     */
    public boolean isCommandEnabled(String commandName) {
        return config.getBoolean("commands.coder." + commandName.toLowerCase(), true);
    }
    
    /**
     * Check if run command is enabled
     */
    public boolean isRunCommandEnabled() {
        return isCommandEnabled("run");
    }
    
    /**
     * Check if load command is enabled
     */
    public boolean isLoadCommandEnabled() {
        return isCommandEnabled("load");
    }
    
    /**
     * Check if unload command is enabled
     */
    public boolean isUnloadCommandEnabled() {
        return isCommandEnabled("unload");
    }
    
    /**
     * Check if reload command is enabled
     */
    public boolean isReloadCommandEnabled() {
        return isCommandEnabled("reload");
    }
    
    /**
     * Check if update command is enabled
     */
    public boolean isUpdateCommandEnabled() {
        return isCommandEnabled("update");
    }
    
    /**
     * Check if update-jar command is enabled
     */
    public boolean isUpdateJarCommandEnabled() {
        return isCommandEnabled("update-jar");
    }
    
    /**
     * Check if confirm command is enabled
     */
    public boolean isConfirmCommandEnabled() {
        return isCommandEnabled("confirm");
    }
    
    /**
     * Check if cancel command is enabled
     */
    public boolean isCancelCommandEnabled() {
        return isCommandEnabled("cancel");
    }
    
    /**
     * Check if error logging is enabled
     */
    public boolean isErrorLoggingEnabled() {
        return config.getBoolean("logs.errors", true);
    }
    
    /**
     * Check if compile error logging is enabled
     */
    public boolean isCompileErrorLoggingEnabled() {
        return config.getBoolean("logs.compile-errors", true);
    }
    
    // Backup Settings
    public String getBackupType() {
        return config.getString("plugin.backups.type", "zip");
    }
    
    public String getBackupSchedule() {
        return config.getString("plugin.backups.schedule.every", "1h");
    }
    
    public boolean shouldBackupOnStart() {
        return config.getBoolean("plugin.backups.schedule.on-start", false);
    }
    
    public boolean shouldCancelOnDisable() {
        return config.getBoolean("plugin.backups.schedule.cancel-on-disable", true);
    }

    // Action Logger Manager Settings
    /**
     * Check if activity logging is enabled
     */
    public boolean isActivityLoggingEnabled() {
        return config.getBoolean("actions-manager.enabled", true);
    }

    /**
     * Check if log compression is enabled
     */
    public boolean isLogCompressionEnabled() {
        return config.getBoolean("actions-manager.compress-logs", true);
    }

    /**
     * Check if enable-activity-logging command is enabled
     */
    public boolean isEnableActivityLoggingCommandEnabled() {
        return isCommandEnabled("enable-activity-logging");
    }

    /**
     * Check if disable-activity-logging command is enabled
     */
    public boolean isDisableActivityLoggingCommandEnabled() {
        return isCommandEnabled("disable-activity-logging");
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