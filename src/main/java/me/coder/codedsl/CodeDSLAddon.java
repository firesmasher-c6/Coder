package me.coder.codedsl;

import me.coder.api.CoderAddon;
import me.coder.api.CoderAPI;
import me.coder.codedsl.listeners.CommandInterceptor;
import me.coder.codedsl.manager.ScriptManager;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class CodeDSLAddon implements CoderAddon {

    private ScriptManager scriptManager;
    private CoderAPI api;
    private File dataFolder;
    private YamlConfiguration addonConfig;
    
    private File variablesStorageFile;
    private File variablesObfFile;
    private YamlConfiguration variablesConfig;

    @Override
    public String getName() {
        return "CodeDSL";
    }

    @Override
    public String getVersion() {
        return "1.9.5";
    }

    @Override
    public String getAuthor() {
        return "Firesmasher";
    }

    @Override
    public String getDescription() {
        return "A Domain Specific Language (DSL) for writing Minecraft scripts with custom syntax";
    }

    @Override
    public void onEnable() {
        try {
            api = CoderAPI.getInstance();
            
            // CRITICAL FIX: Initialize dataFolder FIRST and verify it exists
            CodeDSLPlugin pluginInstance = CodeDSLPlugin.getInstance();
            if (pluginInstance == null) {
                throw new IllegalStateException("CodeDSLPlugin instance is not initialized");
            }
            
            File pluginDataFolder = pluginInstance.getDataFolder();
            if (pluginDataFolder == null) {
                throw new IllegalStateException("Plugin data folder is null");
            }
            
            dataFolder = new File(pluginDataFolder, "addons/CodeDSL");
            if (!dataFolder.exists()) {
                if (!dataFolder.mkdirs()) {
                    throw new IllegalStateException("Failed to create data folder: " + dataFolder.getAbsolutePath());
                }
            }
            
            setupConfig();

            if (addonConfig != null && !addonConfig.getBoolean("enabled", true)) {
                if (api != null) api.log("CodeDSL is disabled in config.yml. Skipping initialization.");
                return;
            }
            
            setupFolders();
            scriptManager = new ScriptManager(dataFolder, api);
            scriptManager.setAddonReference(this);
            loadAllScriptsOnStartup();
            
            if (Bukkit.getPluginManager().getPlugin("Coder") != null) {
                Bukkit.getPluginManager().registerEvents(
                    new CommandInterceptor(api, scriptManager.getCommandParser(), scriptManager, scriptManager.getProcessor(), this),
                    Bukkit.getPluginManager().getPlugin("Coder")
                );
            } else {
                if (api != null) api.logError("Could not register CommandInterceptor: Main plugin 'Coder' was not found!");
            }
            
            registerCommands();
            if (api != null) {
                api.log("CodeDSL v" + getVersion() + " enabled!");
                api.log("Data folder: " + dataFolder.getAbsolutePath());
            }
            
        } catch (Exception e) {
            if (api != null) {
                api.logError("Failed to enable CodeDSL: " + e.getMessage());
            } else {
                System.err.println("[CodeDSL] CRITICAL ERROR: " + e.getMessage());
            }
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        if (scriptManager != null) {
            scriptManager.unloadAllScripts();
        }
        if (api != null) {
            api.log("CodeDSL disabled.");
        }
    }

    private void setupFolders() {
        if (dataFolder == null) {
            throw new IllegalStateException("Data folder is not initialized");
        }
        
        try {
            File scriptsDir = new File(dataFolder, "scripts");
            if (!scriptsDir.exists()) {
                if (!scriptsDir.mkdirs()) {
                    if (api != null) api.logError("Failed to create scripts directory");
                }
            }
            
            File configDir = new File(dataFolder, "config");
            if (!configDir.exists()) {
                if (!configDir.mkdirs()) {
                    if (api != null) api.logError("Failed to create config directory");
                }
            }
            
            // Initialize variables storage files
            variablesStorageFile = new File(dataFolder, "variables.yml");
            variablesObfFile = new File(dataFolder, "variables.obf");
            
            // Create variables files if they don't exist
            if (!variablesStorageFile.exists()) {
                variablesStorageFile.createNewFile();
                variablesConfig = new YamlConfiguration();
                variablesConfig.save(variablesStorageFile);
            } else {
                variablesConfig = YamlConfiguration.loadConfiguration(variablesStorageFile);
            }
            
            if (!variablesObfFile.exists()) {
                variablesObfFile.createNewFile();
            }
            
            if (api != null) {
                api.log("Variables storage initialized: " + variablesStorageFile.getAbsolutePath());
            }
            
        } catch (Exception e) {
            if (api != null) {
                api.logError("Error setting up folders: " + e.getMessage());
            }
            e.printStackTrace();
        }
    }

    private void loadAllScriptsOnStartup() {
        if (addonConfig == null || !addonConfig.getBoolean("scripts.auto-load", false)) {
            if (api != null) api.log("[CodeDSL] Script auto-load is disabled in config.yml.");
            return;
        }

        File scriptsDir = new File(dataFolder, "scripts");
        if (!scriptsDir.exists() || !scriptsDir.isDirectory()) {
            return;
        }

        File[] files = scriptsDir.listFiles();
        if (files == null) return;

        String mainExt = addonConfig.getString("file-extensions.main", ".cd").toLowerCase();
        String legacyExt = addonConfig.getString("file-extensions.legacy", ".code").toLowerCase();
        String oldExt = addonConfig.getString("file-extensions.old", ".cdsl").toLowerCase();
        String customExt = addonConfig.getString("file-extensions.custom", "").toLowerCase();

        List<String> loadedScripts = new ArrayList<>();
        List<String> encounteredErrors = new ArrayList<>();

        for (File file : files) {
            if (file.isFile()) {
                String name = file.getName().toLowerCase();
                boolean matches = name.endsWith(mainExt) || name.endsWith(legacyExt) || name.endsWith(oldExt);
                
                if (!matches && !customExt.isEmpty() 
                        && !customExt.equals("your_prefered_custom_file_extension_here") 
                        && customExt.startsWith(".")) {
                    matches = name.endsWith(customExt);
                }

                if (matches) {
                    try {
                        scriptManager.loadScriptSilent(file.getName(), Bukkit.getConsoleSender());
                        loadedScripts.add(file.getName());
                    } catch (Exception e) {
                        encounteredErrors.add(file.getName() + " (" + e.getMessage() + ")");
                    }
                }
            }
        }
        
        ConsoleCommandSender console = Bukkit.getConsoleSender();
        String g = "§a";
        String r = "§c";
        
        console.sendMessage(g + "----------------< CodeDSL >----------------");
        console.sendMessage(g + "scripts loaded:");
        if (loadedScripts.isEmpty()) {
            console.sendMessage(g + "   N/A");
        } else {
            for (String script : loadedScripts) {
                console.sendMessage(g + "   ✓ " + script);
            }
        }
        console.sendMessage(g + "errors encountered:");
        if (encounteredErrors.isEmpty()) {
            console.sendMessage(g + "   N/A");
        } else {
            for (String error : encounteredErrors) {
                console.sendMessage(r + "   ✗ " + error);
            }
        }
        console.sendMessage(g + "-----------------------------------------");
    }

    private void registerCommands() {
        if (api != null) {
            api.log("CodeDSL addon is ready for command registration");
        }
    }

    private void setupConfig() throws Exception {
        if (dataFolder == null) {
            throw new IllegalStateException("Data folder is null. setupConfig() called before dataFolder initialization");
        }
        
        try {
            File configFile = new File(dataFolder, "config.yml");
            
            if (!configFile.exists()) {
                try (InputStream in = CodeDSLAddon.class.getResourceAsStream("/config.yml");
                     OutputStream out = new FileOutputStream(configFile)) {
                    
                    if (in != null) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = in.read(buffer)) > 0) {
                            out.write(buffer, 0, length);
                        }
                        if (api != null) api.log("Default config.yml created");
                    } else {
                        if (api != null) api.logError("Could not find default config.yml in resources");
                    }
                }
            }
            
            addonConfig = YamlConfiguration.loadConfiguration(configFile);
            
        } catch (Exception e) {
            if (api != null) {
                api.logError("Failed to setup configuration: " + e.getMessage());
            }
            throw new Exception("Configuration setup failed", e);
        }
    }

    public void reloadConfig() {
        if (dataFolder != null) {
            try {
                File configFile = new File(dataFolder, "config.yml");
                if (configFile.exists()) {
                    addonConfig = YamlConfiguration.loadConfiguration(configFile);
                    if (api != null) {
                        api.log("Configuration reloaded successfully");
                    }
                }
            } catch (Exception e) {
                if (api != null) {
                    api.logError("Failed to reload configuration: " + e.getMessage());
                }
            }
        }
    }

    public File getDataFolder() {
        return dataFolder;
    }

    public ScriptManager getScriptManager() {
        return scriptManager;
    }

    public CoderAPI getAPI() {
        return api;
    }

    public YamlConfiguration getAddonConfig() {
        return addonConfig;
    }

    public File getVariablesStorageFile() {
        return variablesStorageFile;
    }

    public File getVariablesObfFile() {
        return variablesObfFile;
    }

    public YamlConfiguration getVariablesConfig() {
        return variablesConfig;
    }

    public void saveVariablesConfig() {
        try {
            if (variablesConfig != null && variablesStorageFile != null) {
                variablesConfig.save(variablesStorageFile);
            }
        } catch (Exception e) {
            if (api != null) {
                api.logError("Failed to save variables config: " + e.getMessage());
            }
        }
    }
}