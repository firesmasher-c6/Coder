package me.coder;

import me.coder.commands.CoderCommand;
import me.coder.manager.ScriptManager;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;

public class CoderPlugin extends JavaPlugin {
    
    private ScriptManager scriptManager;

    @Override
    public void onEnable() {
        // Automatically create/load the config.yml
        saveDefaultConfig();

        // Setup the directories
        setupFolders();
        saveResource("example.txt", false);

        // Setup the script manager and command handler
        this.scriptManager = new ScriptManager(this);
        CoderCommand cmdHandler = new CoderCommand(scriptManager);

        // Register the command and tab completion
        getCommand("coder").setExecutor(cmdHandler);
        getCommand("coder").setTabCompleter(cmdHandler);

        getLogger().info("Coder v1.0.0 enabled. Configuration and structure loaded.");
    }

    private void setupFolders() {
        if (!getDataFolder().exists()) getDataFolder().mkdirs();

        // Standard script directories
        new File(getDataFolder(), "Java/scripts").mkdirs();
        new File(getDataFolder(), "Python/scripts").mkdirs();
        
        // NEW: Custom language script directory
        new File(getDataFolder(), "scripts").mkdirs();
    }
}