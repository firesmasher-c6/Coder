package dev.codestuff.coder;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import dev.codestuff.coder.api.CoderAPI;
import dev.codestuff.coder.commands.CoderCommand;
import dev.codestuff.coder.commands.CodercCommand;
import dev.codestuff.coder.listener.PlayerCommandListener;
import dev.codestuff.coder.listener.PlayerJoinListener;
import dev.codestuff.coder.manager.AddonManager;
import dev.codestuff.coder.manager.BackupManager;
import dev.codestuff.coder.manager.ConfigManager;
import dev.codestuff.coder.manager.EditorManager;
import dev.codestuff.coder.manager.ScriptManager;
import dev.codestuff.coder.manager.VersionManager;
import dev.codestuff.coder.JavaCompiler;

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
        
        this.editorManager = new EditorManager(this);

        CoderCommand cmdHandler = new CoderCommand(this, scriptManager, versionManager, configManager, javaCompiler, backupManager, editorManager);
        getCommand("coder").setExecutor(cmdHandler);
        getCommand("coder").setTabCompleter(cmdHandler);

        CodercCommand codercHandler = new CodercCommand(this, javaCompiler);
        getCommand("coderc").setExecutor(codercHandler);
        getCommand("coderc").setTabCompleter(codercHandler);

        getServer().getPluginManager().registerEvents(new PlayerJoinListener(versionManager), this);

        // Addon scan is deferred — triggered the first time someone runs /pl or
        // "plugins" in console, by which point all plugins are fully enabled.
        getServer().getPluginManager().registerEvents(new PlayerCommandListener(this, addonManager), this);

        this.enabledAt = System.currentTimeMillis();
        getLogger().info("Coder v" + getPluginMeta().getVersion() + " enabled.");

        getLogger().warning("**********************************************************************************************************");
        getLogger().warning("Coder API Warning.");
        getLogger().warning("API Path has changed to 'dev.codestuff.coder.api.CoderAPI', some addons may break.");
        getLogger().warning("Coder 2.4.2+ accepts 'me.coder.api.CoderAPI'. Addons using 'me.coder' will be called as Legacy Addons.");
        getLogger().warning("**********************************************************************************************************");

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
        // Cancel all remaining Bukkit async tasks (version checker, backup downloads, etc.)
        // Without this Bukkit logs a nag warning about improperly shut-down async tasks.
        getServer().getScheduler().cancelTasks(this);

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