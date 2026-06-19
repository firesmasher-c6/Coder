package me.coder;

import me.coder.commands.CoderCommand;
import me.coder.listener.PlayerJoinListener;
import me.coder.manager.ScriptManager;
import me.coder.manager.VersionManager;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;

public class CoderPlugin extends JavaPlugin {
    
    private ScriptManager scriptManager;
    private VersionManager versionManager;

    @Override
    public void onEnable() {
        setupFolders();
        saveDefaultConfig();

        // Initialize managers
        this.scriptManager = new ScriptManager(this);
        this.versionManager = new VersionManager(this);
        
        // Start version manager (checks for updates and watches config.yml)
        versionManager.start();
        
        // Register command handler with version manager
        CoderCommand cmdHandler = new CoderCommand(this, scriptManager, versionManager);
        getCommand("coder").setExecutor(cmdHandler);
        getCommand("coder").setTabCompleter(cmdHandler);
        
        // Register event listeners
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
        // Stop version manager and cleanup
        if (versionManager != null) {
            versionManager.stop();
        }
        
        getLogger().info("Coder plugin disabled.");
    }

    public ScriptManager getScriptManager() {
        return scriptManager;
    }
    
    public VersionManager getVersionManager() {
        return versionManager;
    }
}