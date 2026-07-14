package me.coder;

import me.coder.api.CoderAPI;
import me.coder.commands.CoderCommand;
import me.coder.listener.PlayerJoinListener;
import me.coder.manager.AddonManager;
import me.coder.manager.EditorManager;
import me.coder.manager.ScriptManager;
import me.coder.manager.VersionManager;
import me.coder.manager.ConfigManager;
import me.coder.manager.BackupManager;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;

public class CoderPlugin extends JavaPlugin {
    
    private ScriptManager scriptManager;
    private VersionManager versionManager;
    private ConfigManager configManager;
    private BackupManager backupManager;
    private AddonManager addonManager;
    private JavaCompiler javaCompiler;
    private EditorManager editorManager;

    @Override
    public void onEnable() {
        setupFolders();
        saveDefaultConfig();

        this.configManager = new ConfigManager(this);
        this.scriptManager = new ScriptManager(this);
        this.versionManager = new VersionManager(this);
        this.backupManager = new BackupManager(this, configManager);
        this.addonManager = new AddonManager(this);
        
        File javaClassesFolder = new File(getDataFolder(), "JavaClasses");
        if (!javaClassesFolder.exists()) {
            javaClassesFolder.mkdirs();
        }
        this.javaCompiler = new JavaCompiler(this, javaClassesFolder);
        
        // Initialize Coder API
        initializeAPI();
        
        // Check if startup backup is enabled
        backupManager.checkStartupBackup();
        
        versionManager.start();
        
        // Load and verify all addons
        addonManager.loadAddons();
        
        this.editorManager = new EditorManager(this);

        CoderCommand cmdHandler = new CoderCommand(this, scriptManager, versionManager, configManager, javaCompiler, backupManager, editorManager);
        getCommand("coder").setExecutor(cmdHandler);
        getCommand("coder").setTabCompleter(cmdHandler);
        
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(versionManager), this);

        getLogger().info("Coder v" + getPluginMeta().getVersion() + " enabled.");
    }

    private void setupFolders() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        new File(getDataFolder(), "scripts").mkdirs();
        new File(getDataFolder(), "Logs/Error-Logs").mkdirs();
        new File(getDataFolder(), "backups").mkdirs();
    }

    private void initializeAPI() {
        getLogger().info("[Coder] Initializing Coder API...");
        CoderAPI.getInstance();
        getLogger().info("[Coder] ✓ Coder API initialized");
    }

    @Override
    public void onDisable() {
        if (editorManager != null) {
            editorManager.shutdown();
        }
        if (addonManager != null) {
            addonManager.disableAddons();
        }
        if (versionManager != null) {
            versionManager.stop();
        }
        if (backupManager != null) {
            backupManager.stopOnDisable();
        }
        if (javaCompiler != null) {
            javaCompiler.clearCache();
        }
        getLogger().info("Coder plugin disabled.");
    }

    public ScriptManager getScriptManager() {
        return scriptManager;
    }
    
    public VersionManager getVersionManager() {
        return versionManager;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public BackupManager getBackupManager() {
        return backupManager;
    }
    
    public AddonManager getAddonManager() {
        return addonManager;
    }
}