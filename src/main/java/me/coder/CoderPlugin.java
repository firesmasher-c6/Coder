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
        
        saveResource("example.txt", false);
        File exFile = new File(getDataFolder(), "example.txt");
        File destFile = new File(getDataFolder(), "scripts/example/example.txt");
        if (exFile.exists()) {
            destFile.getParentFile().mkdirs();
            exFile.renameTo(destFile);
        }

        this.scriptManager = new ScriptManager(this);
        CoderCommand cmdHandler = new CoderCommand(this, scriptManager);

        getCommand("coder").setExecutor(cmdHandler);
        getCommand("coder").setTabCompleter(cmdHandler);

        getLogger().info("Coder v1.3.0 enabled. Centralized scripts loaded.");
    }

    private void setupFolders() {
        if (!getDataFolder().exists()) getDataFolder().mkdirs();
        new File(getDataFolder(), "scripts").mkdirs();
        new File(getDataFolder(), "scripts/example").mkdirs();
    }
}