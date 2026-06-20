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
    private final Map<String, File> commandToScriptFile = new ConcurrentHashMap<>();  // NEW: Track which script each command comes from
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

    /**
     * Run script - execute FULL script file (no command registration)
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
            if (api != null) {
                api.log("Executing: " + fileName);
            }
            
            // Execute FULL script (no command registration)
            processor.executeScript(scriptFile, sender);
            
        } catch (Exception e) {
            if (api != null) {
                api.sendError(sender, "Error executing CodeDSL script: " + e.getMessage());
                api.logError("CodeDSL Error: " + e.getMessage());
            }
        }
    }

    /**
     * Load script - register commands from script file
     */
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
            if (api != null) {
                api.log("Script loaded to memory: " + fileName);
            }
        } catch (Exception e) {
            sender.sendMessage("§cError processing script: " + e.getMessage());
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
            if (api != null) {
                api.log("Script unloaded: " + fileName);
            }
        } else {
            if (api != null) {
                api.log("Script not loaded in memory: " + fileName);
            }
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
                    commandToScriptFile.put(finalizedCommand.toLowerCase(), scriptTargetFile);

                    DynamicScriptCommand.register(
                        CodeDSLPlugin.getInstance(),
                        finalizedCommand,
                        new ArrayList<>(),
                        finalizedPermission,
                        (commandSender, args) -> {
                            // Execute ONLY the command block, not the full script
                            executeCommandBlock(finalizedCommand, scriptTargetFile, commandSender);
                        },
                        api
                    );

                    if (api != null) {
                        api.log("✓ Registered command: /" + finalizedCommand);
                    }

                    detectedCommandName = null;
                    detectedPermission = null;
                }
            }
        } catch (Exception e) {
            if (api != null) {
                api.logError("Failed to parse script: " + file.getName() + ": " + e.getMessage());
            }
        }
    }

    public void registerCommandHandler(String commandName, Object handler, String scriptFile) {
        String key = commandName.toLowerCase();
        commandHandlers.put(key, handler);
        scriptCommandMap.computeIfAbsent(scriptFile, k -> new ArrayList<>()).add(key);
    }

    /**
     * Execute ONLY the command block (from trigger: to next command or EOF)
     */
    private void executeCommandBlock(String commandName, File scriptFile, CommandSender sender) {
        try {
            List<String> lines = Files.readAllLines(scriptFile.toPath());
            List<String> commandBlock = new ArrayList<>();
            boolean inCommandBlock = false;

            for (String line : lines) {
                String trimmed = line.trim();

                // Start collecting when we find the trigger
                if (inCommandBlock) {
                    // Stop if we hit another command definition
                    if (trimmed.startsWith("command /") && !trimmed.contains(commandName)) {
                        break;
                    }
                    // Skip the trigger: line itself
                    if (!trimmed.equals("trigger:")) {
                        commandBlock.add(line);
                    }
                }

                // Find the trigger for this command
                if (trimmed.startsWith("trigger:") && !inCommandBlock) {
                    // Backtrack to find the command definition
                    for (int i = lines.indexOf(line) - 1; i >= 0; i--) {
                        String prevLine = lines.get(i).trim();
                        if (prevLine.startsWith("command /") && prevLine.contains(commandName)) {
                            inCommandBlock = true;
                            break;
                        }
                        if (prevLine.startsWith("command /") && !prevLine.contains(commandName)) {
                            break;
                        }
                    }
                }
            }

            if (!commandBlock.isEmpty()) {
                // Execute only the command block
                String blockContent = String.join("\n", commandBlock);
                processor.executeCommandBlock(blockContent, sender);
                
                if (api != null) {
                    api.log("✓ Executed command: /" + commandName);
                }
            }
        } catch (Exception e) {
            sender.sendMessage("§c✗ Error executing command: " + e.getMessage());
            if (api != null) {
                api.logError("Command execution error: " + e.getMessage());
            }
        }
    }

    /**
     * Execute registered command handler (OLD - kept for compatibility)
     */
    public boolean executeCommandHandler(String commandName, CommandSender sender) {
        String key = commandName.toLowerCase();
        Object handler = commandHandlers.get(key);

        if (handler == null) {
            sender.sendMessage("§cCommand handler not found: /" + commandName);
            return false;
        }

        try {
            if (handler instanceof File) {
                File scriptFile = (File) handler;
                executeCommandBlock(commandName, scriptFile, sender);
            }
            return true;
        } catch (Exception e) {
            sender.sendMessage("§c✗ Error executing command: " + e.getMessage());
            if (api != null) {
                api.logError("Command execution error: " + e.getMessage());
            }
            return false;
        }
    }

    public void unloadScriptHandlers(String scriptFile) {
        List<String> commands = scriptCommandMap.remove(scriptFile);
        if (commands != null) {
            for (String cmd : commands) {
                commandHandlers.remove(cmd);
                commandToScriptFile.remove(cmd.toLowerCase());
                if (api != null) {
                    api.log("✓ Unregistered command: /" + cmd);
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
        commandToScriptFile.clear();
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