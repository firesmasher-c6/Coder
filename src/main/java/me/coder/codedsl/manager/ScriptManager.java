package me.coder.codedsl.manager;

import me.coder.api.CoderAPI;
import me.coder.codedsl.CodeDSLAddon;
import me.coder.codedsl.CodeDSLPlugin;
import me.coder.codedsl.CodeDSLProcessor;
import me.coder.codedsl.commands.DynamicScriptCommand;
import me.coder.codedsl.parser.CommandParser;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

/**
 * Manages CodeDSL scripts loading, memory caching, and dynamic runtime command parsing.
 */
public class ScriptManager {

    private final File dataFolder;
    private final CoderAPI api;
    private final CodeDSLProcessor processor;
    private final CommandParser commandParser;
    private final Map<String, CodeDSLProcessor> loadedScripts = new HashMap<>();
    private CodeDSLAddon addonReference; // Added to read config extensions dynamically

    public ScriptManager(File dataFolder, CoderAPI api) {
        this.dataFolder = dataFolder;
        this.api = api;
        
        File scriptsFolder = new File(dataFolder, "scripts");
        this.processor = new CodeDSLProcessor(dataFolder, api);
        this.commandParser = new CommandParser(api, scriptsFolder);
    }

    // Setter so Addon can pass itself after initialization
    public void setAddonReference(CodeDSLAddon addon) {
        this.addonReference = addon;
    }

    /**
     * Run a CodeDSL script directly from the local files storage directory
     */
    public void runScript(String fileName, CommandSender sender) {
        File scriptsDir = new File(dataFolder, "scripts");
        File scriptFile = new File(scriptsDir, fileName);

        if (!scriptFile.exists()) {
            sender.sendMessage("§cCodeDSL script not found: " + fileName);
            return;
        }

        if (!isValidCodeDSLFile(fileName)) {
            sender.sendMessage("§cInvalid file type. Supported: " + getSupportedExtensionsString());
            return;
        }

        try {
            api.sendSuccess(sender, "[CodeDSL] Executing: " + fileName);
            
            // Parse commands and cache inside script registries
            parseAndRegisterScriptCommands(scriptFile);
            
            // Hand over execution pipeline to your primary processor logic
            processor.executeScript(scriptFile, sender);
        } catch (Exception e) {
            api.sendError(sender, "Error executing CodeDSL script: " + e.getMessage());
            api.logError("CodeDSL Error: " + e.getMessage());
        }
    }

    /**
     * Reload a CodeDSL script and refresh its command pipelines
     */
    public void reloadScript(String fileName, CommandSender sender) {
        runScript(fileName, sender);
        api.sendSuccess(sender, "[CodeDSL] Script reloaded: " + fileName);
    }

    /**
     * Load a script to memory and map its structures
     */
    public void loadScript(String fileName, CommandSender sender) {
        File scriptsDir = new File(dataFolder, "scripts");
        File scriptFile = new File(scriptsDir, fileName);

        if (!scriptFile.exists()) {
            sender.sendMessage("§cCodeDSL script not found: " + fileName);
            return;
        }

        try {
            // Read internal syntax declarations and register any command lines found inside
            commandParser.parseCommandDefinitions(scriptFile);
            parseAndRegisterScriptCommands(scriptFile);
            
            loadedScripts.put(fileName, processor);
            api.sendSuccess(sender, "[CodeDSL] Script loaded to memory: " + fileName);
        } catch (Exception e) {
            sender.sendMessage("§cError processing script registration elements: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Loads a script into runtime registries quietly without printing individual verification logs.
     */
    public void loadScriptSilent(String fileName, CommandSender sender) throws Exception {
        File scriptsDir = new File(dataFolder, "scripts");
        File scriptFile = new File(scriptsDir, fileName);

        if (!scriptFile.exists()) {
            throw new java.io.FileNotFoundException("Script file does not exist");
        }

        // We make sure your CommandParser handles a quiet registration call during startup boot loop
        commandParser.parseCommandDefinitionsSilent(scriptFile);
        parseAndRegisterScriptCommands(scriptFile);
        
        loadedScripts.put(fileName, processor);
    }

    /**
     * Unload a script from memory
     */
    public void unloadScript(String fileName, CommandSender sender) {
        if (loadedScripts.remove(fileName) != null) {
            api.sendSuccess(sender, "[CodeDSL] Script unloaded: " + fileName);
        } else {
            api.sendError(sender, "[CodeDSL] Script not loaded in memory: " + fileName);
        }
    }

    /**
     * Scans structural configuration files looking for raw DSL syntax declarations to inject
     */
    private void parseAndRegisterScriptCommands(File file) {
        try {
            List<String> lines = Files.readAllLines(file.toPath());
            String detectedCommandName = null;
            String detectedPermission = null;

            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

                // Match layout pattern: command /example
                if (trimmed.startsWith("command /")) {
                    detectedCommandName = trimmed.substring(9).trim();
                    // Strip off arguments or brackets if any exist on that declaration line
                    if (detectedCommandName.contains(" ")) {
                        detectedCommandName = detectedCommandName.split(" ")[0];
                    }
                    if (detectedCommandName.contains("[")) {
                        detectedCommandName = detectedCommandName.split("\\[")[0];
                    }
                    continue;
                }

                // Match permission assignment inside declaration nodes
                if (detectedCommandName != null && trimmed.startsWith("permission:")) {
                    detectedPermission = trimmed.substring(11).trim();
                    continue;
                }

                // When trigger node initializes, compile structural hook into server pipeline
                if (detectedCommandName != null && trimmed.startsWith("trigger:")) {
                    final String finalizedCommand = detectedCommandName;
                    final String finalizedPermission = detectedPermission;
                    final File scriptTargetFile = file;

                    DynamicScriptCommand.register(
                        CodeDSLPlugin.getInstance(),
                        finalizedCommand,
                        new ArrayList<>(),
                        finalizedPermission,
                        (commandSender, args) -> {
                            try {
                                processor.executeScript(scriptTargetFile, commandSender);
                            } catch (Exception ex) {
                                commandSender.sendMessage("§cExecution failure inside CodeDSL Processor processing loop.");
                                ex.printStackTrace();
                            }
                        },
                        api
                    );

                    // Reset variables for subsequent command entries inside the same file
                    detectedCommandName = null;
                    detectedPermission = null;
                }
            }
        } catch (Exception e) {
            api.logError("Failed to parse script syntax architecture mapping for " + file.getName() + ": " + e.getMessage());
        }
    }

    private boolean isValidCodeDSLFile(String fileName) {
        String lower = fileName.toLowerCase();
        if (addonReference != null && addonReference.getAddonConfig() != null) {
            YamlConfiguration config = addonReference.getAddonConfig();
            
            String mainExt = config.getString("file-extensions.main", ".cd").toLowerCase();
            String legacyExt = config.getString("file-extensions.legacy", ".code").toLowerCase();
            String oldExt = config.getString("file-extensions.old", ".cdsl").toLowerCase();
            String customExt = config.getString("file-extensions.custom", "").toLowerCase();

            if (lower.endsWith(mainExt) || lower.endsWith(legacyExt) || lower.endsWith(oldExt)) {
                return true;
            }

            if (!customExt.isEmpty() 
                && !customExt.equals("your_prefered_custom_file_extension_here") 
                && customExt.startsWith(".")) {
                return lower.endsWith(customExt);
            }
            
            return false;
        }
        return lower.endsWith(".cd") || lower.endsWith(".code");
    }

    private String getSupportedExtensionsString() {
        if (addonReference != null && addonReference.getAddonConfig() != null) {
            YamlConfiguration config = addonReference.getAddonConfig();
            StringBuilder sb = new StringBuilder();
            sb.append(config.getString("file-extensions.main", ".cd"))
              .append(", ").append(config.getString("file-extensions.legacy", ".code"))
              .append(", ").append(config.getString("file-extensions.old", ".cdsl"));
            
            String customExt = config.getString("file-extensions.custom", "");
            if (!customExt.isEmpty() 
                && !customExt.equalsIgnoreCase("YOUR_PREFERED_CUSTOM_FILE_EXTENSION_HERE") 
                && customExt.startsWith(".")) {
                sb.append(", ").append(customExt);
            }
            return sb.toString();
        }
        return ".cd, .code";
    }

    public void unloadAllScripts() {
        loadedScripts.clear();
        commandParser.unregisterAllCommands();
        api.log("All CodeDSL scripts unloaded");
    }

    public CommandParser getCommandParser() {
        return this.commandParser;
    }

    public CodeDSLProcessor getProcessor() {
        return this.processor;
    }

    public File getDataFolder() {
        return dataFolder;
    }
}