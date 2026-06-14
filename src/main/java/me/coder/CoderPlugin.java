package me.coder;

import me.coder.commands.CoderCommand;
import me.coder.manager.ScriptManager;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;

public class CoderPlugin extends JavaPlugin {
    
    private ScriptManager scriptManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        setupFolders();

        this.scriptManager = new ScriptManager(this);
        
        CoderCommand cmdHandler = new CoderCommand(this, scriptManager);

        getCommand("coder").setExecutor(cmdHandler);
        getCommand("coder").setTabCompleter(cmdHandler);

        getLogger().info("Coder v1.3.6 enabled.");
    }

    private void setupFolders() {
        if (!getDataFolder().exists()) getDataFolder().mkdirs();
        new File(getDataFolder(), "scripts").mkdirs();
        new File(getDataFolder(), "Logs/Error-Logs").mkdirs();
    }

    @Override
    public void onDisable() {
        getLogger().info("Coder plugin disabled.");
    }
}