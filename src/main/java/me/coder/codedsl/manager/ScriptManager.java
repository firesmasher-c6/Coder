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
import java.util.concurrent.ConcurrentHashMap;

public class ScriptManager {

    private final File dataFolder;
    private final CoderAPI api;
    private final CodeDSLProcessor processor;
    private final CommandParser commandParser;
    private final Map<String, CodeDSLProcessor> loadedScripts = new ConcurrentHashMap<>();
    private final Map<String, Object> commandHandlers = new ConcurrentHashMap<>();
    private final Map<String, List<String>> scriptCommandMap = new ConcurrentHashMap<>();
    private CodeDSLAddon addonReference;

    public ScriptManager(File dataFolder, CoderAPI api) {
        this.dataFolder = dataFolder;
        this.api = api;
        File scriptsFolder = new File(dataFolder, "scripts");
        this.processor = new CodeDSLProcessor(dataFolder, api);
        this.commandParser = new CommandParser(api, scriptsFolder);
    }

    public void setAddonReference(CodeDSLAddon addon) {
        this.addonReference = addon;
    }

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
            parseAndRegisterScriptCommands(scriptFile);
            processor.executeScript(scriptFile, sender);
        } catch (Exception e) {
            api.sendError(sender, "Error executing CodeDSL script: " + e.getMessage());
            api.logError("CodeDSL Error: " + e.getMessage());
        }
    }

    public void reloadScript(String fileName, CommandSender sender) {
        runScript(fileName, sender);
        api.sendSuccess(sender, "[CodeDSL] Script reloaded: " + fileName);
    }

    public void loadScript(String fileName, CommandSender sender) {
        File scriptsDir = new File(dataFolder, "scripts");
        File scriptFile = new File(scriptsDir, fileName);

        if (!scriptFile.exists()) {
            sender.sendMessage("§cCodeDSL script not found: " + fileName);
            return;
        }

        try {
            commandParser.parseCommandDefinitions(scriptFile);
            parseAndRegisterScriptCommands(scriptFile);
            loadedScripts.put(fileName, processor);
            api.sendSuccess(sender, "[CodeDSL] Script loaded to memory: " + fileName);
        } catch (Exception e) {
            sender.sendMessage("§cError processing script registration elements: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void loadScriptSilent(String fileName, CommandSender sender) throws Exception {
        File scriptsDir = new File(dataFolder, "scripts");
        File scriptFile = new File(scriptsDir, fileName);

        if (!scriptFile.exists()) {
            throw new java.io.FileNotFoundException("Script file does not exist");
        }

        commandParser.parseCommandDefinitionsSilent(scriptFile);
        parseAndRegisterScriptCommands(scriptFile);
        loadedScripts.put(fileName, processor);
    }

    public void unloadScript(String fileName, CommandSender sender) {
        unloadScriptHandlers(fileName);
        if (loadedScripts.remove(fileName) != null) {
            api.sendSuccess(sender, "[CodeDSL] Script unloaded: " + fileName);
        } else {
            api.sendError(sender, "[CodeDSL] Script not loaded in memory: " + fileName);
        }
    }

    private void parseAndRegisterScriptCommands(File file) {
        try {
            List<String> lines = Files.readAllLines(file.toPath());
            String detectedCommandName = null;
            String detectedPermission = null;

            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

                if (trimmed.startsWith("command /")) {
                    detectedCommandName = trimmed.substring(9).trim();
                    if (detectedCommandName.contains(" ")) {
                        detectedCommandName = detectedCommandName.split(" ")[0];
                    }
                    if (detectedCommandName.contains("[")) {
                        detectedCommandName = detectedCommandName.split("\\[")[0];
                    }
                    continue;
                }

                if (detectedCommandName != null && trimmed.startsWith("permission:")) {
                    detectedPermission = trimmed.substring(11).trim();
                    continue;
                }

                if (detectedCommandName != null && trimmed.startsWith("trigger:")) {
                    final String finalizedCommand = detectedCommandName;
                    final String finalizedPermission = detectedPermission;
                    final File scriptTargetFile = file;

                    registerCommandHandler(finalizedCommand, scriptTargetFile, file.getName());

                    DynamicScriptCommand.register(
                        CodeDSLPlugin.getInstance(),
                        finalizedCommand,
                        new ArrayList<>(),
                        finalizedPermission,
                        (commandSender, args) -> {
                            try {
                                executeCommandHandler(finalizedCommand, commandSender);
                            } catch (Exception ex) {
                                commandSender.sendMessage("§cExecution failure inside CodeDSL Processor processing loop.");
                                ex.printStackTrace();
                            }
                        },
                        api
                    );

                    detectedCommandName = null;
                    detectedPermission = null;
                }
            }
        } catch (Exception e) {
            api.logError("Failed to parse script syntax architecture mapping for " + file.getName() + ": " + e.getMessage());
        }
    }

    public void registerCommandHandler(String commandName, Object handler, String scriptFile) {
        String key = commandName.toLowerCase();
        commandHandlers.put(key, handler);
        scriptCommandMap.computeIfAbsent(scriptFile, k -> new ArrayList<>()).add(key);
        if (api != null) {
            api.log("[CodeDSL] Registered command handler: /" + key);
        }
    }

    public boolean executeCommandHandler(String commandName, CommandSender sender) {
        String key = commandName.toLowerCase();
        Object handler = commandHandlers.get(key);

        if (handler == null) {
            sender.sendMessage("§cCommand handler not found: /" + commandName);
            return false;
        }

        try {
            if (handler instanceof File) {
                processor.executeScript((File) handler, sender);
            }
            return true;
        } catch (Exception e) {
            sender.sendMessage("§cError executing command /" + commandName + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public void unloadScriptHandlers(String scriptFile) {
        List<String> commands = scriptCommandMap.remove(scriptFile);
        if (commands != null) {
            for (String cmd : commands) {
                commandHandlers.remove(cmd);
                if (api != null) {
                    api.log("[CodeDSL] Unregistered command handler: /" + cmd);
                }
            }
        }
    }

    public boolean hasCommandHandler(String commandName) {
        return commandHandlers.containsKey(commandName.toLowerCase());
    }

    public Set<String> getRegisteredCommands() {
        return new HashSet<>(commandHandlers.keySet());
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
        commandHandlers.clear();
        scriptCommandMap.clear();
        loadedScripts.clear();
        commandParser.unregisterAllCommands();
        if (api != null) {
            api.log("All CodeDSL scripts unloaded");
        }
    }

    public Collection<String> getLoadedScripts() {
        return loadedScripts.keySet();
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