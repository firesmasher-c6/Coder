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

    private long enabledAt;

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

        this.enabledAt = System.currentTimeMillis();
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
        getLogger().info("Initializing Coder API...");
        CoderAPI.getInstance();
        getLogger().info("✓ Coder API initialized");
    }

    @Override
    public void onDisable() {
        long disabledAt = System.currentTimeMillis();
        String exitCode = configManager != null ? configManager.getExitCode() : "0";

        // Exit code 1: force kill — skip graceful teardown, just log and bail
        if ("1".equals(exitCode)) {
            getLogger().severe("Force-kill exit (exitCode=1). Skipping graceful shutdown.");
            return;
        }

        // All other codes: graceful teardown
        if (editorManager != null) editorManager.shutdown();
        if (addonManager != null)  addonManager.disableAddons();
        if (versionManager != null) versionManager.stop();
        if (backupManager != null)  backupManager.stopOnDisable();
        if (javaCompiler != null)   javaCompiler.clearCache();

        // Exit code 2: detailed shutdown report
        if ("2".equals(exitCode)) {
            long uptimeMs   = disabledAt - enabledAt;
            long uptimeSecs = uptimeMs / 1000;
            long hours      = uptimeSecs / 3600;
            long minutes    = (uptimeSecs % 3600) / 60;
            long seconds    = uptimeSecs % 60;

            double tps  = getServer().getTPS()[0];
            double mspt = getServer().getAverageTickTime();

            getLogger().warning("═══════════ Shutdown Report ═══════════");
            getLogger().warning(" Exit Code   : 2 (detailed)");
            getLogger().warning(" Config Ver  : " + configManager.getConfigVersion());
            getLogger().warning(String.format(" Uptime      : %dh %dm %ds", hours, minutes, seconds));
            getLogger().warning(String.format(" TPS (1m avg): %.2f", tps));
            getLogger().warning(String.format(" MSPT (avg)  : %.2fms", mspt));
            getLogger().warning(" Enabled At  : " + new java.util.Date(enabledAt));
            getLogger().warning(" Disabled At : " + new java.util.Date(disabledAt));
            getLogger().warning("═══════════════════════════════════════");
        } else {
            getLogger().info("Coder plugin disabled.");
        }
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