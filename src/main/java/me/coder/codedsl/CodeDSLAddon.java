package me.coder.codedsl;

import me.coder.api.CoderAddon;
import me.coder.api.CoderAPI;
import me.coder.codedsl.listeners.CommandInterceptor;
import me.coder.codedsl.manager.ScriptManager;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class CodeDSLAddon implements CoderAddon {

    private ScriptManager scriptManager;
    private CoderAPI api;
    private File dataFolder;         // plugins/Coder/CodeDSL/
    private File configFolder;       // plugins/CodeDSL/
    private YamlConfiguration addonConfig;
    
    private File variablesStorageFile;
    private Properties variablesProperties;  // FIX 3: Changed from YamlConfiguration to Properties

    public CodeDSLAddon() {}

    public CodeDSLAddon(File customDataFolder, File corePluginFolder) {
        this.dataFolder = customDataFolder;
        this.configFolder = corePluginFolder;
    }

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
            
            if (dataFolder == null || configFolder == null) {
                CodeDSLPlugin pluginInstance = CodeDSLPlugin.getInstance();
                if (pluginInstance != null) {
                    if (configFolder == null) configFolder = pluginInstance.getDataFolder();
                    if (dataFolder == null) dataFolder = new File(pluginInstance.getDataFolder().getParentFile(), "Coder/CodeDSL");
                }
            }

            if (dataFolder == null || configFolder == null) {
                throw new IllegalStateException("Required ecosystem folder paths could not be fully resolved!");
            }
            
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
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
        saveVariablesConfig();  // FIX 3: Save variables using Properties.store()
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
            // 1. Setup primary folder structures
            File scriptsDir = new File(dataFolder, "scripts");
            if (!scriptsDir.exists()) scriptsDir.mkdirs();
            
            File examplesDir = new File(dataFolder, "examples");
            if (!examplesDir.exists()) examplesDir.mkdirs();

            File variablesDir = new File(dataFolder, "variables");
            if (!variablesDir.exists()) variablesDir.mkdirs();
            
            // 2. Safely extract placeholders document asset
            File placeholdersFile = new File(dataFolder, "placeholders.md");
            if (!placeholdersFile.exists()) {
                saveResourceToFolder("/placeholders.md", placeholdersFile);
            }

            // 3. Dynamically copy all elements inside the jar's resources "examples/" folder
            extractJarExamplesFolder(examplesDir);

            // 4. FIX 3: Setup core variables storage using Properties (NOT YAML)
            variablesStorageFile = new File(variablesDir, "variables.storage");
            variablesProperties = new Properties();
            
            if (variablesStorageFile.exists()) {
                try (FileInputStream fis = new FileInputStream(variablesStorageFile)) {
                    variablesProperties.load(fis);
                    if (api != null) {
                        api.log("Variables storage loaded: " + variablesProperties.size() + " variables");
                    }
                } catch (IOException e) {
                    if (api != null) {
                        api.logError("Failed to load variables.storage: " + e.getMessage());
                    }
                    variablesProperties = new Properties();
                }
            } else {
                try {
                    variablesStorageFile.createNewFile();
                    if (api != null) {
                        api.log("Created new variables.storage file");
                    }
                } catch (IOException e) {
                    if (api != null) {
                        api.logError("Failed to create variables.storage: " + e.getMessage());
                    }
                }
            }
            
        } catch (Exception e) {
            if (api != null) {
                api.logError("Error setting up folders: " + e.getMessage());
            }
            e.printStackTrace();
        }
    }

    private void extractJarExamplesFolder(File targetExamplesDir) {
        try {
            String jarPath = CodeDSLAddon.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            String decodedJarPath = URLDecoder.decode(jarPath, StandardCharsets.UTF_8.name());
            File jarFileObj = new File(decodedJarPath);

            if (jarFileObj.exists() && jarFileObj.isFile()) {
                try (JarFile jar = new JarFile(jarFileObj)) {
                    Enumeration<JarEntry> entries = jar.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        String name = entry.getName();

                        if (name.startsWith("examples/") && !entry.isDirectory()) {
                            String fileName = name.substring("examples/".length());
                            if (fileName.isEmpty()) continue;

                            File targetFile = new File(targetExamplesDir, fileName);
                            if (!targetFile.exists()) {
                                saveResourceToFolder("/" + name, targetFile);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (api != null) {
                api.logError("Failed to dynamically discover embedded resource examples: " + e.getMessage());
            }
        }
    }

    private void saveResourceToFolder(String resourcePath, File targetFile) {
        try (InputStream in = CodeDSLAddon.class.getResourceAsStream(resourcePath);
             OutputStream out = new FileOutputStream(targetFile)) {
            if (in != null) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
            }
        } catch (Exception ignored) {}
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
        if (configFolder == null) {
            throw new IllegalStateException("Config folder context is missing!");
        }
        
        try {
            File configFile = new File(configFolder, "config.yml");
            if (!configFile.exists()) {
                throw new IllegalStateException("Core config.yml was not initialized by CodeDSLPlugin inside plugins/CodeDSL/!");
            }
            addonConfig = YamlConfiguration.loadConfiguration(configFile);
        } catch (Exception e) {
            if (api != null) {
                api.logError("Failed to load configuration layout properties: " + e.getMessage());
            }
            throw new Exception("Configuration workflow exception caught", e);
        }
    }

    public void reloadConfig() {
        if (configFolder != null) {
            try {
                File configFile = new File(configFolder, "config.yml");
                if (configFile.exists()) {
                    addonConfig = YamlConfiguration.loadConfiguration(configFile);
                    if (api != null) {
                        api.log("Configuration reloaded successfully");
                    }
                }
            } catch (Exception e) {
                if (api != null) {
                    api.logError("Failed to reload configuration maps: " + e.getMessage());
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

    public void saveVariablesConfig() {
        try {
            if (variablesProperties != null && variablesStorageFile != null) {
                try (FileOutputStream fos = new FileOutputStream(variablesStorageFile)) {
                    variablesProperties.store(fos, "CodeDSL Persistent Variables - Do not edit manually");
                    if (api != null) {
                        api.log("Saved persistent variables: " + variablesProperties.size() + " entries");
                    }
                }
            }
        } catch (Exception e) {
            if (api != null) {
                api.logError("Failed to save variables: " + e.getMessage());
            }
        }
    }
    
    // FIX 3: Add helper methods for variable access
    public String getVariable(String key, String defaultValue) {
        return variablesProperties.getProperty(key, defaultValue);
    }
    
    public void setVariable(String key, String value) {
        variablesProperties.setProperty(key, value);
    }
    
    public void removeVariable(String key) {
        variablesProperties.remove(key);
    }
    
    public Properties getAllVariables() {
        return variablesProperties;
    }
}