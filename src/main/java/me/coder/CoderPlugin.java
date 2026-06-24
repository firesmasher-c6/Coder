package me.coder;

import me.coder.commands.CoderCommand;
import me.coder.listener.PlayerJoinListener;
import me.coder.manager.ScriptManager;
import me.coder.manager.VersionManager;
import me.coder.manager.ConfigManager;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;

public class CoderPlugin extends JavaPlugin {
    
    private ScriptManager scriptManager;
    private VersionManager versionManager;
    private ConfigManager configManager;
    private JavaCompiler javaCompiler;

    @Override
    public void onEnable() {
        setupFolders();
        saveDefaultConfig();

        this.configManager = new ConfigManager(this);
        this.scriptManager = new ScriptManager(this);
        this.versionManager = new VersionManager(this);
        
        File javaClassesFolder = new File(getDataFolder(), "JavaClasses");
        if (!javaClassesFolder.exists()) {
            javaClassesFolder.mkdirs();
        }
        this.javaCompiler = new JavaCompiler(this, javaClassesFolder);
        
        versionManager.start();
        
        CoderCommand cmdHandler = new CoderCommand(this, scriptManager, versionManager, configManager, javaCompiler);
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
    }

    @Override
    public void onDisable() {
        if (versionManager != null) {
            versionManager.stop();
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
}