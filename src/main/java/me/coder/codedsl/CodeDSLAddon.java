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

/**
 * CodeDSL - A Domain Specific Language addon for the Coder plugin
 * Allows users to write scripts in a custom DSL (.cd or .code files)
 */
public class CodeDSLAddon implements CoderAddon {

    private ScriptManager scriptManager;
    private CoderAPI api;
    private File dataFolder;
    private YamlConfiguration addonConfig;
    
    // Core file storage references inside the dedicated variables folder
    private File variablesStorageFile;
    private File variablesObfFile;

    @Override
    public String getName() {
        return "CodeDSL";
    }

    @Override
    public String getVersion() {
        return "1.8.2";
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
            // Get API instance
            api = CoderAPI.getInstance();
            
            // Extract and load config.yml contents into memory
            setupConfig();

            // Respect global toggle from config
            if (addonConfig != null && !addonConfig.getBoolean("enabled", true)) {
                if (api != null) api.log("CodeDSL is disabled in config.yml. Skipping initialization.");
                return;
            }
            
            // Create data folders, sub-folders, and copy packaged example assets
            setupFolders();
            
            // Initialize script manager
            scriptManager = new ScriptManager(dataFolder, api);
            scriptManager.setAddonReference(this); // Passes reference down for dynamic config lookup
            
            // Automatically loop and load script files only if configured to do so
            loadAllScriptsOnStartup();
            
            // Register your command interceptor listener to catch cross-plugin /coder run events
            if (Bukkit.getPluginManager().getPlugin("Coder") != null) {
                Bukkit.getPluginManager().registerEvents(
                    new CommandInterceptor(api, scriptManager.getCommandParser(), scriptManager, scriptManager.getProcessor(), this),
                    Bukkit.getPluginManager().getPlugin("Coder")
                );
            } else {
                api.logError("Could not register CommandInterceptor: Main plugin 'Coder' was not found!");
            }
            
            // Register commands log hook
            registerCommands();
            
            // Log startup
            api.log("CodeDSL v" + getVersion() + " enabled!");
            api.log("Data folder: " + dataFolder.getAbsolutePath());
            
        } catch (Exception e) {
            if (api != null) {
                api.logError("Failed to enable CodeDSL: " + e.getMessage());
            }
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        // Cleanup
        if (scriptManager != null) {
            scriptManager.unloadAllScripts();
        }
        if (api != null) {
            api.log("CodeDSL disabled.");
        }
    }

    /**
     * Finds and loads script files automatically on enable if enabled in config.yml.
     */
    private void loadAllScriptsOnStartup() {
        // Read configuration value 'scripts.auto-load' (Defaulting to false as defined in your config)
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

        // Fetch user-defined syntax extensions dynamically from config slots
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
                
                // Safety Guard: Evaluate custom string entry to make sure it's valid
                if (!matches && !customExt.isEmpty() 
                        && !customExt.equals("your_prefered_custom_file_extension_here") 
                        && customExt.startsWith(".")) {
                    matches = name.endsWith(customExt);
                }

                if (matches) {
                    try {
                        // Pass false to loadScript to keep it completely silent during boot up
                        scriptManager.loadScriptSilent(file.getName(), Bukkit.getConsoleSender());
                        loadedScripts.add(file.getName());
                    } catch (Exception e) {
                        encounteredErrors.add(file.getName() + " (" + e.getMessage() + ")");
                    }
                }
            }
        }
        
        // Warning-free color formatting using explicit color tokens directly to the console sender
        ConsoleCommandSender console = Bukkit.getConsoleSender();
        String g = "§a";
        String r = "§c";
        
        console.sendMessage(g + "======== CodeDSL ========");
        console.sendMessage(g + "scripts loaded:");
        if (loadedScripts.isEmpty()) {
            console.sendMessage(g + "   None");
        } else {
            for (String script : loadedScripts) {
                console.sendMessage(g + "   - " + script);
            }
        }
        console.sendMessage("");
        
        console.sendMessage(g + "encountered errors:");
        if (encounteredErrors.isEmpty()) {
            console.sendMessage(g + "   None");
        } else {
            for (String error : encounteredErrors) {
                console.sendMessage(r + "   - " + error);
            }
        }
        console.sendMessage(g + "=========================");
    }

    /**
     * Extracts config.yml from jar resources to /plugins/CodeDSL/config.yml and loads it.
     */
    private void setupConfig() {
        File configFolder = new File(Bukkit.getServer().getPluginsFolder(), "CodeDSL");
        if (!configFolder.exists()) {
            configFolder.mkdirs();
        }
        
        File configFile = new File(configFolder, "config.yml");
        if (!configFile.exists()) {
            exportResource("config.yml", configFile);
            if (api != null) {
                api.log("Default config.yml exported to /plugins/CodeDSL/");
            }
        }

        // Parse flat config structure directly into a readable Bukkit YamlConfiguration memory instance
        this.addonConfig = YamlConfiguration.loadConfiguration(configFile);
    }

    /**
     * Setup all required folders and export packaged resource examples
     */
    private void setupFolders() {
        dataFolder = new File(Bukkit.getServer().getPluginsFolder(), "Coder/CodeDSL");
        
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        // 1. Create scripts directory
        new File(dataFolder, "scripts").mkdirs();
        
        // 2. Create examples directory
        File examplesDir = new File(dataFolder, "examples");
        examplesDir.mkdirs();
        
        // 3. Define and create the variables subdirectory path
        File variablesDir = new File(dataFolder, "variables");
        variablesDir.mkdirs();
        
        // 4. Bind the flat data files INSIDE the variables subdirectory instead of root!
        variablesStorageFile = new File(variablesDir, "variables.storage");
        variablesObfFile = new File(variablesDir, "variables.obf");
        
        try {
            if (!variablesStorageFile.exists()) variablesStorageFile.createNewFile();
            if (!variablesObfFile.exists()) variablesObfFile.createNewFile();
        } catch (Exception e) {
            if (api != null) api.logError("Could not initialize variable data files: " + e.getMessage());
        }

        if (api != null) {
            api.log("CodeDSL folders verified at: " + dataFolder.getAbsolutePath());
        }

        // Project resource files matching directory mapping
        String[] exampleFiles = {
            "command.cd",
            "EXAMPLES.cd",
            "readingFiles.cd",
            "StoringVariables.cd"
        };

        // Dynamically migrate all project script templates into /plugins/Coder/CodeDSL/examples/
        for (String fileName : exampleFiles) {
            File targetFile = new File(examplesDir, fileName);
            if (!targetFile.exists()) {
                exportResource("examples/" + fileName, targetFile);
            }
        }
    }

    /**
     * Extracts an embedded resource file from the JAR out to a physical destination file path.
     */
    private void exportResource(String resourcePath, File destinationFile) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                if (api != null) {
                    api.log("Could not find source resource inside jar: " + resourcePath);
                }
                return;
            }

            try (OutputStream out = new FileOutputStream(destinationFile)) {
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        } catch (Exception e) {
            if (api != null) {
                api.logError("Failed to extract resource asset '" + resourcePath + "': " + e.getMessage());
            }
            e.printStackTrace();
        }
    }

    /**
     * Register commands
     */
    private void registerCommands() {
        org.bukkit.command.PluginCommand cmd = Bukkit.getPluginCommand("codedsl");
        if (cmd != null) {
            cmd.setExecutor((sender, command, label, args) -> {
                if (args.length > 0 && args[0].equalsIgnoreCase("configreload")) {
                    if (!sender.hasPermission("codedsl.admin")) {
                        sender.sendMessage("§cNo permission.");
                        return true;
                    }
                    
                    // Reload the config from disk
                    File configFile = new File(Bukkit.getServer().getPluginsFolder(), "CodeDSL/config.yml");
                    this.addonConfig = YamlConfiguration.loadConfiguration(configFile);
                    
                    sender.sendMessage("§a[CodeDSL] Config reloaded successfully!");
                    return true;
                }
                
                sender.sendMessage("§cUsage: /codedsl configreload");
                return true;
            });
        }
        
        if (api != null) {
            api.log("CodeDSL commands registered via bootstrap layer.");
        }
    }

    public YamlConfiguration getAddonConfig() {
        return addonConfig;
    }

    public ScriptManager getScriptManager() {
        return scriptManager;
    }

    public File getDataFolder() {
        return dataFolder;
    }

    public File getVariablesStorageFile() {
        return variablesStorageFile;
    }

    public File getVariablesObfFile() {
        return variablesObfFile;
    }

    public CoderAPI getAPI() {
        return api;
    }
}