# CodeDSLPlugin
package me.coder.codedsl;

import java.io.File;
import me.coder.api.CoderAPI;
import me.coder.codedsl.commands.CodeDSLCommand;
import me.coder.codedsl.manager.ScriptManager;
import org.bukkit.plugin.java.JavaPlugin;

public class CodeDSLPlugin extends JavaPlugin {

   private static CodeDSLPlugin instance;
   private CodeDSLAddon addonInstance;

   public void onEnable() {
      instance = this;
      this.saveDefaultConfig();
      this.addonInstance = new CodeDSLAddon();
      this.addonInstance.onEnable();
      ScriptManager scriptManager = this.addonInstance.getScriptManager();
      CoderAPI api = this.addonInstance.getAPI();
      File dataFolder = this.addonInstance.getDataFolder();
      if (scriptManager != null && api != null && dataFolder != null) {
         CodeDSLCommand.register(this, scriptManager, api, dataFolder);
      } else {
         this.getLogger().severe("Could not programmatically register commands: CodeDSLAddon failed to provide vital fields during setup!");
      }

   }

   public void onDisable() {
      if (this.addonInstance != null) {
         this.addonInstance.onDisable();
      }

   }

   public static CodeDSLPlugin getInstance() {
      return instance;
   }

   public CodeDSLAddon getAddonInstance() {
      return this.addonInstance;
   }
}

# CodeDSLAddon
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
            
            dataFolder = new File(CodeDSLPlugin.getInstance().getDataFolder(), "addons/CodeDSL");
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
                api.logError("Could not register CommandInterceptor: Main plugin 'Coder' was not found!");
            }
            
            registerCommands();
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
        if (scriptManager != null) {
            scriptManager.unloadAllScripts();
        }
        if (api != null) {
            api.log("CodeDSL disabled.");
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
                    }
                }
            }
            
            addonConfig = YamlConfiguration.loadConfiguration(configFile);
            
            if (api != null) {
                api.log("[CodeDSL] Configuration loaded from: " + configFile.getAbsolutePath());
            }
            
        } catch (Exception e) {
            if (api != null) {
                api.logError("Failed to setup configuration: " + e.getMessage());
            }
            throw e;
        }
    }

    private void setupFolders() throws Exception {
        try {
            dataFolder.mkdirs();
            new File(dataFolder, "scripts").mkdirs();
            new File(dataFolder, "variables").mkdirs();
            new File(dataFolder, "cache").mkdirs();
            
            variablesStorageFile = new File(dataFolder, "variables/variables.yml");
            variablesObfFile = new File(dataFolder, "variables/variables.obf");
            
            if (!variablesStorageFile.exists()) {
                variablesStorageFile.createNewFile();
            }
            
            if (api != null) {
                api.log("CodeDSL folder structure initialized at: " + dataFolder.getAbsolutePath());
            }
        } catch (Exception e) {
            if (api != null) {
                api.logError("Failed to setup folders: " + e.getMessage());
            }
            throw e;
        }
    }

    public void reloadConfig() {
        try {
            if (api != null) {
                api.log("[CodeDSL] Starting configuration reload...");
            }
            
            setupConfig();
            
            boolean autoLoadEnabled = addonConfig != null && addonConfig.getBoolean("scripts.auto-load", false);
            
            if (autoLoadEnabled && scriptManager != null) {
                if (api != null) {
                    api.log("[CodeDSL] Auto-load is enabled. Reloading all scripts...");
                }
                
                scriptManager.unloadAllScripts();
                loadAllScriptsOnStartup();
                
                if (api != null) {
                    api.log("[CodeDSL] All scripts reloaded with new configuration");
                }
            } else if (api != null) {
                api.log("[CodeDSL] Configuration reloaded (auto-load disabled, scripts not reloaded)");
            }
            
        } catch (Exception e) {
            if (api != null) {
                api.logError("Failed to reload CodeDSL configuration: " + e.getMessage());
            }
            e.printStackTrace();
        }
    }

    public ScriptManager getScriptManager() {
        return scriptManager;
    }

    public CoderAPI getAPI() {
        return api;
    }

    public File getDataFolder() {
        return dataFolder;
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
}

# CodeDSLProcessor
package me.coder.codedsl;

import me.coder.api.CoderAPI;
import me.coder.codedsl.manager.PlaceholderManager;
import org.bukkit.command.CommandSender;
import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Processes and executes CodeDSL scripts (.cd and .code files)
 */
public class CodeDSLProcessor {

    private final CoderAPI api;
    private final File dataFolder;
    private final Map<String, String> variables = new HashMap<>();
    private final Map<String, String> obfuscatedVars = new HashMap<>();
    private final PlaceholderManager placeholderManager;

    public CodeDSLProcessor(File dataFolder, CoderAPI api) {
        this.dataFolder = dataFolder;
        this.api = api;
        this.placeholderManager = new PlaceholderManager(api, variables, obfuscatedVars);
        loadVariables();
    }

    /**
     * Execute a CodeDSL script file
     */
    public void executeScript(File scriptFile, CommandSender sender) {
        try {
            List<String> lines = Files.readAllLines(scriptFile.toPath());
            api.log("[CodeDSL] Executing: " + scriptFile.getName());
            processLines(lines, sender);
            api.log("[CodeDSL] Script execution complete!");
        } catch (Exception e) {
            api.logError("Error executing script: " + e.getMessage());
            sender.sendMessage("§cError executing CodeDSL script: " + e.getMessage());
        }
    }

    /**
     * Process script lines
     */
    private void processLines(List<String> lines, CommandSender sender) {
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String originalLine = line;
            line = line.trim();
            
            // Skip empty lines and comments
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            // Handle varRead with indented block
            if (line.startsWith("varRead ")) {
                String varName = line.substring(8).trim();
                if (varName.endsWith(":")) {
                    varName = varName.substring(0, varName.length() - 1).trim();
                }
                if (varName.startsWith("\"") && varName.endsWith("\"")) {
                    varName = varName.substring(1, varName.length() - 1);
                }
                if (variables.containsKey(varName)) {
                    api.log("[CodeDSL] Variable '" + varName + "': " + variables.get(varName));
                    i = processIndentedBlock(lines, i, originalLine, sender);
                }
                continue;
            }

            // Handle obfvarRead with indented block
            if (line.startsWith("obfvarRead ")) {
                String varName = line.substring(11).trim();
                if (varName.endsWith(":")) {
                    varName = varName.substring(0, varName.length() - 1).trim();
                }
                if (varName.startsWith("\"") && varName.endsWith("\"")) {
                    varName = varName.substring(1, varName.length() - 1);
                }
                if (obfuscatedVars.containsKey(varName)) {
                    try {
                        String decoded = new String(Base64.getDecoder().decode(obfuscatedVars.get(varName)));
                        api.log("[CodeDSL] Obfuscated variable '" + varName + "': " + decoded);
                        i = processIndentedBlock(lines, i, originalLine, sender);
                    } catch (Exception e) {
                        api.log("[CodeDSL] Error decoding obfuscated variable: " + varName);
                    }
                }
                continue;
            }

            // Handle broadcast command
            if (line.startsWith("broadcast ")) {
                String message = extractString(line.substring(10));
                api.broadcast(replacePlaceholders(message));
            }

            // Handle send command
            else if (line.startsWith("send ")) {
                String[] parts = line.substring(5).split(" to ");
                if (parts.length == 2) {
                    String message = extractString(parts[0]);
                    String playerName = parts[1].trim();
                    var player = org.bukkit.Bukkit.getPlayer(playerName);
                    if (player != null) {
                        player.sendMessage(replacePlaceholders(message));
                    }
                }
            }

            // Handle console.log command (including multi-line)
            else if (line.startsWith("console.log ")) {
                String message = line.substring(12).trim();
                
                // Handle multi-line: console.log { ... }
                if (message.startsWith("{")) {
                    StringBuilder fullMessage = new StringBuilder();
                    message = message.substring(1).trim();
                    
                    // If closing brace on same line
                    if (message.endsWith("}")) {
                        message = message.substring(0, message.length() - 1).trim();
                        fullMessage.append(message);
                    } else {
                        // Collect lines until closing brace
                        fullMessage.append(message).append("\n");
                        i++;
                        while (i < lines.size()) {
                            String nextLine = lines.get(i);
                            String nextTrimmed = nextLine.trim();
                            
                            if (nextTrimmed.endsWith("}")) {
                                fullMessage.append(nextTrimmed, 0, nextTrimmed.length() - 1).append("\n");
                                break;
                            } else {
                                fullMessage.append(nextTrimmed).append("\n");
                            }
                            i++;
                        }
                    }
                    
                    message = fullMessage.toString().trim();
                } else if (message.startsWith("\"") && message.endsWith("\"")) {
                    // Single line with quotes
                    message = message.substring(1, message.length() - 1);
                }
                
                message = replacePlaceholders(message);
                api.log(message);
            }

            // Handle variable assignment
            else if (line.startsWith("var ")) {
                parseVariable(line.substring(4), false);
            }

            // Handle obfuscated variable assignment
            else if (line.startsWith("obfuscatedVAR ")) {
                parseVariable(line.substring(14), true);
            }

            // Handle delete variable
            else if (line.startsWith("deleteVar ")) {
                String varName = line.substring(10).trim();
                if (varName.startsWith("\"") && varName.endsWith("\"")) {
                    varName = varName.substring(1, varName.length() - 1);
                }
                if (variables.remove(varName) != null) {
                    saveVariables();
                    api.log("[CodeDSL] Variable deleted: " + varName);
                }
            }

            // Handle delete obfuscated variable
            else if (line.startsWith("deleteObfVar ")) {
                String varName = line.substring(13).trim();
                if (varName.startsWith("\"") && varName.endsWith("\"")) {
                    varName = varName.substring(1, varName.length() - 1);
                }
                if (obfuscatedVars.remove(varName) != null) {
                    saveObfuscatedVariables();
                    api.log("[CodeDSL] Obfuscated variable deleted: " + varName);
                }
            }

            // Handle delay (in ticks)
            else if (line.startsWith("delay ")) {
                try {
                    String ticks = line.substring(6).trim();
                    long delayTicks = Long.parseLong(ticks);
                    Thread.sleep(delayTicks * 50);
                    api.log("[CodeDSL] Delayed " + delayTicks + " ticks");
                } catch (NumberFormatException | InterruptedException e) {
                    api.logError("Error parsing delay: " + e.getMessage());
                }
            }

            // Handle wait (by time units)
            else if (line.startsWith("wait ")) {
                String waitStr = line.substring(5).trim();
                long delayMs = parseTimeUnit(waitStr);
                if (delayMs > 0) {
                    try {
                        Thread.sleep(delayMs);
                        api.log("[CodeDSL] Waited " + waitStr);
                    } catch (InterruptedException e) {
                        api.logError("Wait interrupted: " + e.getMessage());
                    }
                }
            }

            // Handle import statements
            else if (line.startsWith("import ")) {
                String importStr = line.substring(7).trim();
                handleImport(importStr);
            }

            // Handle command definitions
            else if (line.startsWith("command ")) {
                String commandDef = line.substring(8).trim();
                if (commandDef.endsWith(":")) {
                    commandDef = commandDef.substring(0, commandDef.length() - 1).trim();
                }
                api.log("[CodeDSL] Command defined: " + commandDef + " (custom commands not yet fully implemented)");
            }

            // Handle run blocks
            else if (line.startsWith("run ")) {
                if (line.contains("{")) {
                    String runContent = line.substring(4).trim();
                    if (runContent.startsWith("{")) {
                        runContent = runContent.substring(1);
                        i = processRunBlock(lines, i, originalLine, sender, runContent);
                    }
                }
            }

            // Handle async blocks
            else if (line.startsWith("async:")) {
                i = processAsyncBlock(lines, i, originalLine, sender);
            }

            // Handle every loop
            else if (line.startsWith("every ")) {
                String everyContent = line.substring(6).trim();
                if (everyContent.contains("{")) {
                    String[] parts = everyContent.split("\\{");
                    if (parts.length >= 1) {
                        String timeStr = parts[0].trim();
                        String timeValue = "";
                        String timeUnit = "";
                        for (int j = 0; j < timeStr.length(); j++) {
                            char c = timeStr.charAt(j);
                            if (Character.isDigit(c)) {
                                timeValue += c;
                            } else {
                                timeUnit = timeStr.substring(j).trim();
                                break;
                            }
                        }
                        
                        if (!timeValue.isEmpty() && !timeUnit.isEmpty()) {
                            long delayMs = 0;
                            if (timeUnit.equals("s")) {
                                delayMs = Long.parseLong(timeValue) * 1000;
                            } else if (timeUnit.equals("m")) {
                                delayMs = Long.parseLong(timeValue) * 60000;
                            } else if (timeUnit.equals("t")) {
                                delayMs = Long.parseLong(timeValue) * 50;
                            }
                            
                            if (delayMs > 0) {
                                api.log("[CodeDSL] Started every loop: every " + timeValue + timeUnit);
                                i = processEveryBlock(lines, i, originalLine, sender, delayMs);
                            }
                        }
                    }
                }
            }

            // Handle execute command
            else if (line.startsWith("execute command ")) {
                String command = extractString(line.substring(16));
                if (line.contains(" in console")) {
                    api.executeCommand(command);
                }
            }

            // Handle file operations
            else if (line.startsWith("fileRead ")) {
                String filepath = line.substring(9).trim();
                readFile(filepath);
            }

            // Handle file write
            else if (line.startsWith("fileWrite ")) {
                String fileWriteContent = line.substring(10).trim();
                if (fileWriteContent.contains("{")) {
                    String[] parts = fileWriteContent.split("\\{", 2);
                    if (parts.length == 2) {
                        String filepath = parts[0].trim();
                        String content = parts[1];
                        
                        // Collect lines until closing brace
                        StringBuilder fileContent = new StringBuilder(content);
                        i++;
                        while (i < lines.size()) {
                            String nextLine = lines.get(i);
                            String nextTrimmed = nextLine.trim();
                            
                            if (nextTrimmed.endsWith("}")) {
                                fileContent.append(nextTrimmed, 0, nextTrimmed.length() - 1);
                                break;
                            } else {
                                fileContent.append(nextTrimmed).append("\n");
                            }
                            i++;
                        }
                        
                        writeFile(filepath, fileContent.toString().trim());
                    }
                }
            }

            // Handle file delete
            else if (line.startsWith("fileDel ")) {
                String filepath = line.substring(8).trim();
                deleteFile(filepath);
            }

            // Handle if statements with indented blocks
            else if (line.startsWith("if ")) {
                String condition = line.substring(3).trim();
                if (condition.endsWith(":")) {
                    condition = condition.substring(0, condition.length() - 1).trim();
                }
                
                if (condition.contains(" = ")) {
                    String[] parts = condition.split(" = ");
                    if (parts.length >= 2) {
                        String varPart = parts[0].replace("var", "").trim();
                        String valuePart = parts[1].trim();
                        
                        if (varPart.startsWith("\"") && varPart.endsWith("\"")) {
                            varPart = varPart.substring(1, varPart.length() - 1);
                        }
                        if (valuePart.startsWith("\"") && valuePart.endsWith("\"")) {
                            valuePart = valuePart.substring(1, valuePart.length() - 1);
                        }
                        
                        String actualValue = variables.get(varPart);
                        boolean conditionMet = actualValue != null && actualValue.equals(valuePart);
                        
                        i = processIfElseBlock(lines, i, originalLine, sender, conditionMet);
                    }
                }
            }
        }
    }

    /**
     * Process indented block after a statement
     */
    private int processIndentedBlock(List<String> lines, int startIndex, String parentLine, CommandSender sender) {
        int baseIndent = getIndentation(parentLine);
        int i = startIndex + 1;

        while (i < lines.size()) {
            String line = lines.get(i);
            String trimmed = line.trim();

            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                i++;
                continue;
            }

            int currentIndent = getIndentation(line);
            if (currentIndent <= baseIndent) {
                break;
            }

            String cleanedLine = trimmed;
            
            if (cleanedLine.startsWith("console.log ")) {
                String message = cleanedLine.substring(12).trim();
                if (message.startsWith("\"") && message.endsWith("\"")) {
                    message = message.substring(1, message.length() - 1);
                } else if (message.startsWith("{")) {
                    message = message.substring(1);
                    if (message.endsWith("}")) {
                        message = message.substring(0, message.length() - 1);
                    }
                }
                message = replacePlaceholders(message);
                api.log(message);
            }
            else if (cleanedLine.startsWith("broadcast ")) {
                String message = extractString(cleanedLine.substring(10));
                api.broadcast(replacePlaceholders(message));
            }
            else if (cleanedLine.startsWith("send ")) {
                String[] parts = cleanedLine.substring(5).split(" to ");
                if (parts.length == 2) {
                    String message = extractString(parts[0]);
                    String playerName = parts[1].trim();
                    var player = org.bukkit.Bukkit.getPlayer(playerName);
                    if (player != null) {
                        player.sendMessage(replacePlaceholders(message));
                    }
                }
            }

            i++;
        }

        return i - 1;
    }

    /**
     * Process if/else block
     */
    private int processIfElseBlock(List<String> lines, int startIndex, String parentLine, CommandSender sender, boolean conditionMet) {
        int baseIndent = getIndentation(parentLine);
        int i = startIndex + 1;
        boolean inIfBlock = true;
        boolean inElseBlock = false;

        while (i < lines.size()) {
            String line = lines.get(i);
            String trimmed = line.trim();

            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                i++;
                continue;
            }

            int currentIndent = getIndentation(line);
            if (currentIndent <= baseIndent) {
                break;
            }

            if (trimmed.startsWith("else:") || trimmed.equals("else:")) {
                inIfBlock = false;
                inElseBlock = true;
                i++;
                continue;
            }

            if ((inIfBlock && conditionMet) || (inElseBlock && !conditionMet)) {
                String cleanedLine = trimmed;
                
                if (cleanedLine.startsWith("console.log ")) {
                    String message = cleanedLine.substring(12).trim();
                    if (message.startsWith("\"") && message.endsWith("\"")) {
                        message = message.substring(1, message.length() - 1);
                    } else if (message.startsWith("{")) {
                        message = message.substring(1);
                        if (message.endsWith("}")) {
                            message = message.substring(0, message.length() - 1);
                        }
                    }
                    message = replacePlaceholders(message);
                    api.log(message);
                }
                else if (cleanedLine.startsWith("broadcast ")) {
                    String message = extractString(cleanedLine.substring(10));
                    api.broadcast(replacePlaceholders(message));
                }
                else if (cleanedLine.startsWith("send ")) {
                    String[] parts = cleanedLine.substring(5).split(" to ");
                    if (parts.length == 2) {
                        String message = extractString(parts[0]);
                        String playerName = parts[1].trim();
                        var player = org.bukkit.Bukkit.getPlayer(playerName);
                        if (player != null) {
                            player.sendMessage(replacePlaceholders(message));
                        }
                    }
                }
            }

            i++;
        }

        return i - 1;
    }

    /**
     * Process every block (repeating loop)
     */
    private int processEveryBlock(List<String> lines, int startIndex, String parentLine, CommandSender sender, long delayMs) {
        int baseIndent = getIndentation(parentLine);
        int i = startIndex + 1;
        List<String> blockLines = new ArrayList<>();

        while (i < lines.size()) {
            String line = lines.get(i);
            String trimmed = line.trim();

            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                i++;
                continue;
            }

            int currentIndent = getIndentation(line);
            if (currentIndent <= baseIndent) {
                break;
            }

            blockLines.add(trimmed);
            i++;
        }

        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(delayMs);
                    for (String blockLine : blockLines) {
                        if (blockLine.startsWith("broadcast ")) {
                            String message = extractString(blockLine.substring(10));
                            api.broadcast(replacePlaceholders(message));
                        } else if (blockLine.startsWith("console.log ")) {
                            String message = blockLine.substring(12).trim();
                            if (message.startsWith("\"") && message.endsWith("\"")) {
                                message = message.substring(1, message.length() - 1);
                            }
                            message = replacePlaceholders(message);
                            api.log(message);
                        } else if (blockLine.startsWith("send ")) {
                            String[] parts = blockLine.substring(5).split(" to ");
                            if (parts.length == 2) {
                                String message = extractString(parts[0]);
                                String playerName = parts[1].trim();
                                var player = org.bukkit.Bukkit.getPlayer(playerName);
                                if (player != null) {
                                    player.sendMessage(replacePlaceholders(message));
                                }
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();

        return i - 1;
    }

    /**
     * Parse variable assignment
     */
    private void parseVariable(String statement, boolean obfuscated) {
        String[] parts = statement.split("=", 2);
        if (parts.length == 2) {
            String name = parts[0].trim();
            String value = parts[1].trim();
            
            if (obfuscated) {
                String obf = Base64.getEncoder().encodeToString(value.getBytes());
                obfuscatedVars.put(name, obf);
                saveObfuscatedVariables();
                api.log("[CodeDSL] Obfuscated variable stored: " + name);
            } else {
                variables.put(name, value);
                saveVariables();
                api.log("[CodeDSL] Variable stored: " + name + " = " + value);
            }
        }
    }

    /**
     * Extract string from quotes
     */
    private String extractString(String input) {
        Pattern pattern = Pattern.compile("\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return input.trim();
    }

    /**
     * Replace all placeholders using PlaceholderManager
     */
    private String replacePlaceholders(String text) {
        return placeholderManager.replacePlaceholders(text);
    }

    /**
     * Read file contents
     */
    private void readFile(String filepath) {
        try {
            File file = new File(filepath);
            if (file.exists()) {
                List<String> lines = Files.readAllLines(file.toPath());
                for (String line : lines) {
                    api.log("[FileRead] " + line);
                }
            }
        } catch (Exception e) {
            api.logError("Error reading file: " + e.getMessage());
        }
    }

    /**
     * Load variables from storage
     */
    private void loadVariables() {
        try {
            File varFile = new File(dataFolder, "variables/variables.storage");
            if (varFile.exists()) {
                List<String> lines = Files.readAllLines(varFile.toPath());
                for (String line : lines) {
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    if (line.contains("=")) {
                        String[] parts = line.split("=", 2);
                        if (parts.length == 2) {
                            variables.put(parts[0].trim(), parts[1].trim());
                        }
                    }
                }
            }
            
            File obfFile = new File(dataFolder, "variables/variables.obf");
            if (obfFile.exists()) {
                List<String> lines = Files.readAllLines(obfFile.toPath());
                for (String line : lines) {
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    if (line.contains("=")) {
                        String[] parts = line.split("=", 2);
                        if (parts.length == 2) {
                            obfuscatedVars.put(parts[0].trim(), parts[1].trim());
                        }
                    }
                }
            }
        } catch (Exception e) {
            api.logError("Error loading variables: " + e.getMessage());
        }
    }

    /**
     * Save variables to storage
     */
    public void saveVariables() {
        try {
            File varFile = new File(dataFolder, "variables/variables.storage");
            varFile.getParentFile().mkdirs();
            StringBuilder sb = new StringBuilder();
            sb.append("# CodeDSL Plain Variables\n");
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                sb.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
            }
            Files.write(varFile.toPath(), sb.toString().getBytes());
        } catch (Exception e) {
            api.logError("Error saving variables: " + e.getMessage());
        }
    }

    /**
     * Save obfuscated variables to storage
     */
    public void saveObfuscatedVariables() {
        try {
            File obfFile = new File(dataFolder, "variables/variables.obf");
            obfFile.getParentFile().mkdirs();
            StringBuilder sb = new StringBuilder();
            sb.append("# CodeDSL Obfuscated Variables (Base64)\n");
            for (Map.Entry<String, String> entry : obfuscatedVars.entrySet()) {
                sb.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
            }
            Files.write(obfFile.toPath(), sb.toString().getBytes());
        } catch (Exception e) {
            api.logError("Error saving obfuscated variables: " + e.getMessage());
        }
    }

    /**
     * Write file contents
     */
    private void writeFile(String filepath, String content) {
        try {
            File file = new File(filepath);
            file.getParentFile().mkdirs();
            Files.write(file.toPath(), content.getBytes());
            api.log("[CodeDSL] File written: " + filepath);
        } catch (Exception e) {
            api.logError("Error writing file: " + e.getMessage());
        }
    }

    /**
     * Delete file
     */
    private void deleteFile(String filepath) {
        try {
            File file = new File(filepath);
            if (file.delete()) {
                api.log("[CodeDSL] File deleted: " + filepath);
            } else {
                api.logError("Could not delete file: " + filepath);
            }
        } catch (Exception e) {
            api.logError("Error deleting file: " + e.getMessage());
        }
    }

    /**
     * Parse time unit (1s, 5m, 10t)
     */
    private long parseTimeUnit(String timeStr) {
        String timeValue = "";
        String timeUnit = "";
        
        for (int j = 0; j < timeStr.length(); j++) {
            char c = timeStr.charAt(j);
            if (Character.isDigit(c)) {
                timeValue += c;
            } else {
                timeUnit = timeStr.substring(j).trim();
                break;
            }
        }
        
        if (timeValue.isEmpty() || timeUnit.isEmpty()) {
            return -1;
        }
        
        long value = Long.parseLong(timeValue);
        if (timeUnit.equals("s")) {
            return value * 1000;
        } else if (timeUnit.equals("m")) {
            return value * 60000;
        } else if (timeUnit.equals("t")) {
            return value * 50;
        }
        return -1;
    }

    /**
     * Handle import statements
     */
    private void handleImport(String importStr) {
        if (importStr.equalsIgnoreCase("async")) {
            api.log("[CodeDSL] Async library imported");
        } else if (importStr.equalsIgnoreCase("Connection")) {
            api.log("[CodeDSL] Connection library imported");
        } else if (importStr.equalsIgnoreCase("bukkit")) {
            api.log("[CodeDSL] Bukkit library imported");
        } else {
            api.log("[CodeDSL] Unknown import: " + importStr);
        }
    }

    /**
     * Process run block { ... }
     */
    private int processRunBlock(List<String> lines, int startIndex, String parentLine, CommandSender sender, String firstContent) {
        int baseIndent = getIndentation(parentLine);
        int i = startIndex + 1;
        List<String> runLines = new ArrayList<>();
        
        if (firstContent.length() > 0 && !firstContent.equals("{")) {
            runLines.add(firstContent.trim());
        }

        while (i < lines.size()) {
            String line = lines.get(i);
            String trimmed = line.trim();

            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                i++;
                continue;
            }

            if (trimmed.endsWith("}")) {
                runLines.add(trimmed.substring(0, trimmed.length() - 1).trim());
                break;
            }

            runLines.add(trimmed);
            i++;
        }

        // Execute all lines in run block
        for (String runLine : runLines) {
            if (runLine.startsWith("broadcast ")) {
                String message = extractString(runLine.substring(10));
                api.broadcast(replacePlaceholders(message));
            } else if (runLine.startsWith("send ")) {
                String[] parts = runLine.substring(5).split(" to ");
                if (parts.length == 2) {
                    String message = extractString(parts[0]);
                    String playerName = parts[1].trim();
                    var player = org.bukkit.Bukkit.getPlayer(playerName);
                    if (player != null) {
                        player.sendMessage(replacePlaceholders(message));
                    }
                }
            } else if (runLine.startsWith("console.log ")) {
                String message = runLine.substring(12).trim();
                if (message.startsWith("\"") && message.endsWith("\"")) {
                    message = message.substring(1, message.length() - 1);
                }
                message = replacePlaceholders(message);
                api.log(message);
            }
        }

        return i;
    }

    /**
     * Process async block
     */
    private int processAsyncBlock(List<String> lines, int startIndex, String parentLine, CommandSender sender) {
        int baseIndent = getIndentation(parentLine);
        int i = startIndex + 1;
        List<String> asyncLines = new ArrayList<>();

        while (i < lines.size()) {
            String line = lines.get(i);
            String trimmed = line.trim();

            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                i++;
                continue;
            }

            int currentIndent = getIndentation(line);
            if (currentIndent <= baseIndent) {
                break;
            }

            asyncLines.add(trimmed);
            i++;
        }

        // Execute async block in new thread
        new Thread(() -> {
            for (String asyncLine : asyncLines) {
                if (asyncLine.startsWith("broadcast ")) {
                    String message = extractString(asyncLine.substring(10));
                    api.broadcast(replacePlaceholders(message));
                } else if (asyncLine.startsWith("send ")) {
                    String[] parts = asyncLine.substring(5).split(" to ");
                    if (parts.length == 2) {
                        String message = extractString(parts[0]);
                        String playerName = parts[1].trim();
                        var player = org.bukkit.Bukkit.getPlayer(playerName);
                        if (player != null) {
                            player.sendMessage(replacePlaceholders(message));
                        }
                    }
                } else if (asyncLine.startsWith("console.log ")) {
                    String message = asyncLine.substring(12).trim();
                    if (message.startsWith("\"") && message.endsWith("\"")) {
                        message = message.substring(1, message.length() - 1);
                    }
                    message = replacePlaceholders(message);
                    api.log(message);
                }
            }
        }).start();

        return i - 1;
    }

    /**
     * Get indentation level of a line
     */
    private int getIndentation(String line) {
        int count = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') {
                count++;
            } else if (c == '\t') {
                count += 4;
            } else {
                break;
            }
        }
        return count;
    }
}

# syntaxes/SyntaxExecutor
package me.coder.codedsl.syntaxes;

import me.coder.api.CoderAPI;
import me.coder.codedsl.manager.VariableManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.nio.file.Files;
import java.util.regex.Matcher;

/**
 * Executes validated CodeDSL syntax
 */
public class SyntaxExecutor {

    private final CoderAPI api;
    private final VariableManager variableManager;

    public SyntaxExecutor(CoderAPI api, VariableManager variableManager) {
        this.api = api;
        this.variableManager = variableManager;
    }

    /**
     * Execute a line of CodeDSL
     */
    public void execute(String line, CommandSender sender, SyntaxValidator.SyntaxResult result) {
        if (!result.valid) {
            api.sendError(sender, "[CodeDSL] Invalid syntax: " + result.errorMessage);
            return;
        }

        try {
            switch (result.syntaxType) {
                case "variable":
                    executeVariable(line, result.matcher);
                    break;
                case "obfvar":
                    executeObfuscatedVariable(line, result.matcher);
                    break;
                case "broadcast":
                    executeBroadcast(result.matcher);
                    break;
                case "send":
                    executeSend(result.matcher, sender);
                    break;
                case "console_log":
                    executeConsoleLog(line);
                    break;
                case "file_read":
                    executeFileRead(result.matcher);
                    break;
                case "file_write":
                    executeFileWrite(result.matcher);
                    break;
                case "file_del":
                    executeFileDelete(result.matcher);
                    break;
                case "var_read":
                    executeVarRead(result.matcher, sender);
                    break;
                case "delete_var":
                    executeDeleteVar(result.matcher);
                    break;
                case "delete_obfvar":
                    executeDeleteObfVar(result.matcher);
                    break;
                case "execute":
                    executeCommand(result.matcher, sender);
                    break;
                case "if_statement":
                    executeIf(result.matcher, sender);
                    break;
                case "COMMENT":
                    // Ignore comments
                    break;
                default:
                    api.sendWarning(sender, "[CodeDSL] Syntax not implemented: " + result.syntaxType);
            }
        } catch (Exception e) {
            api.sendError(sender, "[CodeDSL] Error executing: " + e.getMessage());
            api.logError("CodeDSL Execution Error: " + e.getMessage());
        }
    }

    private void executeVariable(String line, Matcher matcher) {
        matcher.reset(line);
        if (matcher.find()) {
            String name = matcher.group(1).trim();
            String value = matcher.group(2).trim();
            
            // Remove quotes if present
            if (value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }
            
            // Save to file immediately
            variableManager.setVariable(name, value);
            api.log("[CodeDSL] Variable saved: " + name + " = " + value);
        }
    }

    private void executeObfuscatedVariable(String line, Matcher matcher) {
        matcher.reset(line);
        if (matcher.find()) {
            String name = matcher.group(1).trim();
            String value = matcher.group(2).trim();
            
            // Remove quotes if present
            if (value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }
            
            // Save to file immediately (obfuscated)
            variableManager.setObfuscatedVariable(name, value);
            api.log("[CodeDSL] Obfuscated variable saved: " + name);
        }
    }

    private void executeBroadcast(Matcher matcher) {
        if (matcher.find()) {
            String message = matcher.group(1);
            message = variableManager.replacePlaceholders(message);
            api.broadcast(message);
        }
    }

    private void executeSend(Matcher matcher, CommandSender sender) {
        if (matcher.find()) {
            String message = matcher.group(1);
            String playerName = matcher.group(2).trim();
            
            message = variableManager.replacePlaceholders(message);
            
            Player player = Bukkit.getPlayer(playerName);
            if (player != null) {
                player.sendMessage(message);
            } else {
                api.sendWarning(sender, "[CodeDSL] Player not found: " + playerName);
            }
        }
    }

    private void executeConsoleLog(String line) {
        // Extract content after console.log
        String content = line.substring(11).trim();
        
        // Handle multi-line: console.log { ... }
        if (content.startsWith("{")) {
            content = content.substring(1);
            if (content.endsWith("}")) {
                content = content.substring(0, content.length() - 1);
            }
        } else if (content.startsWith("\"") && content.endsWith("\"")) {
            content = content.substring(1, content.length() - 1);
        }
        
        content = variableManager.replacePlaceholders(content);
        api.log("[CodeDSL] " + content);
    }

    private void executeFileRead(Matcher matcher) {
        if (matcher.find()) {
            String filepath = matcher.group(1).trim();
            try {
                File file = new File(filepath);
                if (file.exists()) {
                    for (String line : Files.readAllLines(file.toPath())) {
                        api.log("[File] " + line);
                    }
                } else {
                    api.logWarning("[CodeDSL] File not found: " + filepath);
                }
            } catch (Exception e) {
                api.logError("[CodeDSL] Error reading file: " + e.getMessage());
            }
        }
    }

    private void executeFileWrite(Matcher matcher) {
        if (matcher.find()) {
            String filepath = matcher.group(1).trim();
            String content = matcher.group(2);
            
            content = variableManager.replacePlaceholders(content);
            
            try {
                File file = new File(filepath);
                file.getParentFile().mkdirs();
                Files.write(file.toPath(), content.getBytes());
                api.log("[CodeDSL] File written: " + filepath);
            } catch (Exception e) {
                api.logError("[CodeDSL] Error writing file: " + e.getMessage());
            }
        }
    }

    private void executeFileDelete(Matcher matcher) {
        if (matcher.find()) {
            String filepath = matcher.group(1).trim();
            File file = new File(filepath);
            if (file.delete()) {
                api.log("[CodeDSL] File deleted: " + filepath);
            } else {
                api.logWarning("[CodeDSL] Could not delete file: " + filepath);
            }
        }
    }

    private void executeVarRead(Matcher matcher, CommandSender sender) {
        if (matcher.find()) {
            String name = matcher.group(1);
            String value = variableManager.getVariable(name);
            if (value != null) {
                api.log("[CodeDSL] Variable '" + name + "' = " + value);
            } else {
                api.sendWarning(sender, "[CodeDSL] Variable not found: " + name);
            }
        }
    }

    private void executeDeleteVar(Matcher matcher) {
        if (matcher.find()) {
            String name = matcher.group(1);
            variableManager.deleteVariable(name);
        }
    }

    private void executeDeleteObfVar(Matcher matcher) {
        if (matcher.find()) {
            String name = matcher.group(1);
            variableManager.deleteObfuscatedVariable(name);
        }
    }

    private void executeCommand(Matcher matcher, CommandSender sender) {
        if (matcher.find()) {
            String command = matcher.group(1);
            String target = matcher.group(2).toLowerCase();
            
            command = variableManager.replacePlaceholders(command);
            
            if (target.equals("console")) {
                api.executeCommand(command);
            } else if (target.equals("player") && sender instanceof Player) {
                Bukkit.dispatchCommand((Player) sender, command);
            }
        }
    }

    private void executeIf(Matcher matcher, CommandSender sender) {
        if (matcher.find()) {
            String varName = matcher.group(1);
            String expectedValue = matcher.group(2).trim();
            
            // Remove quotes from expected value
            if (expectedValue.startsWith("\"") && expectedValue.endsWith("\"")) {
                expectedValue = expectedValue.substring(1, expectedValue.length() - 1);
            }
            
            String actualValue = variableManager.getVariable(varName);
            if (actualValue != null && actualValue.equals(expectedValue)) {
                api.log("[CodeDSL] If condition matched for: " + varName);
            }
        }
    }
}

# syntaxes/SyntaxValidator
package me.coder.codedsl.syntaxes;

import me.coder.api.CoderAPI;
import java.util.*;
import java.util.regex.*;

/**
 * Validates CodeDSL syntax before execution
 */
public class SyntaxValidator {

    private final CoderAPI api;
    private final Map<String, Pattern> syntaxPatterns = new HashMap<>();

    public SyntaxValidator(CoderAPI api) {
        this.api = api;
        initializeSyntaxPatterns();
    }

    /**
     * Initialize all syntax patterns
     */
    private void initializeSyntaxPatterns() {
        // Variable: var name = value
        syntaxPatterns.put("variable", Pattern.compile("^var\\s+([a-zA-Z0-9_]+)\\s*=\\s*(.+)$"));
        
        // Obfuscated Variable: obfuscatedVAR name = value
        syntaxPatterns.put("obfvar", Pattern.compile("^obfuscatedVAR\\s+([a-zA-Z0-9_]+)\\s*=\\s*(.+)$"));
        
        // Broadcast: broadcast "message"
        syntaxPatterns.put("broadcast", Pattern.compile("^broadcast\\s+\"(.*)\"$"));
        
        // Send: send "message" to player_name
        syntaxPatterns.put("send", Pattern.compile("^send\\s+\"(.*)\"\\s+to\\s+(.+)$"));
        
        // Console log: console.log "message" or console.log { ... }
        syntaxPatterns.put("console_log", Pattern.compile("^console\\.log\\s+(.*)$"));
        
        // File Read: fileRead /path/to/file
        syntaxPatterns.put("file_read", Pattern.compile("^fileRead\\s+(.+)$"));
        
        // File Write: fileWrite /path/to/file { ... }
        syntaxPatterns.put("file_write", Pattern.compile("^fileWrite\\s+([^\\{]+)\\{(.*)\\}$", Pattern.DOTALL));
        
        // File Delete: fileDel /path/to/file
        syntaxPatterns.put("file_del", Pattern.compile("^fileDel\\s+(.+)$"));
        
        // Variable Read: varRead "name"
        syntaxPatterns.put("var_read", Pattern.compile("^varRead\\s+\"([a-zA-Z0-9_]+)\"$"));
        
        // Delete Variable: deleteVar "name"
        syntaxPatterns.put("delete_var", Pattern.compile("^deleteVar\\s+\"([a-zA-Z0-9_]+)\"$"));
        
        // Delete Obfuscated Var: deleteObfVar "name"
        syntaxPatterns.put("delete_obfvar", Pattern.compile("^deleteObfVar\\s+\"([a-zA-Z0-9_]+)\"$"));
        
        // Wait: wait 1s, wait 5m, wait 10t
        syntaxPatterns.put("wait", Pattern.compile("^wait\\s+(\\d+)([smth])$"));
        
        // Delay: delay 20 (in ticks)
        syntaxPatterns.put("delay", Pattern.compile("^delay\\s+(\\d+)$"));
        
        // Execute Command: execute command "cmd" in console/player
        syntaxPatterns.put("execute", Pattern.compile("^execute\\s+command\\s+\"(.*)\"\\s+in\\s+(console|player)$"));
        
        // If Statement: if var "name" = value:
        syntaxPatterns.put("if_statement", Pattern.compile("^if\\s+var\\s+\"([a-zA-Z0-9_]+)\"\\s*=\\s*(.+):$"));
        
        // Command: command /name [<args>]:
        syntaxPatterns.put("command", Pattern.compile("^command\\s+(/\\S+).*:$"));
        
        // Every: every 5s { ... }
        syntaxPatterns.put("every", Pattern.compile("^every\\s+(\\d+)([smh])\\s*\\{(.*)\\}", Pattern.DOTALL));
        
        // Import: import async, import Connection, etc
        syntaxPatterns.put("import", Pattern.compile("^import\\s+(async|Connection|bukkit|[a-zA-Z0-9_]+)$"));
    }

    /**
     * Validate a single line of CodeDSL
     */
    public SyntaxResult validateLine(String line) {
        line = line.trim();

        // Skip empty lines and comments
        if (line.isEmpty() || line.startsWith("#")) {
            return new SyntaxResult(true, "COMMENT", null);
        }

        // Check against all syntax patterns
        for (Map.Entry<String, Pattern> entry : syntaxPatterns.entrySet()) {
            Pattern pattern = entry.getValue();
            Matcher matcher = pattern.matcher(line);
            
            if (matcher.find()) {
                return new SyntaxResult(true, entry.getKey(), matcher);
            }
        }

        // Unknown syntax
        return new SyntaxResult(false, "UNKNOWN", null, "Unknown CodeDSL syntax: " + line);
    }

    /**
     * Syntax validation result
     */
    public static class SyntaxResult {
        public boolean valid;
        public String syntaxType;
        public Matcher matcher;
        public String errorMessage;

        public SyntaxResult(boolean valid, String syntaxType, Matcher matcher) {
            this.valid = valid;
            this.syntaxType = syntaxType;
            this.matcher = matcher;
            this.errorMessage = null;
        }

        public SyntaxResult(boolean valid, String syntaxType, Matcher matcher, String errorMessage) {
            this(valid, syntaxType, matcher);
            this.errorMessage = errorMessage;
        }
    }
}

# placeholders/Placeholders
package me.coder.codedsl.placeholders;

import org.bukkit.Bukkit;

/**
 * Handles all server-side placeholders for CodeDSL
 * Replaces {placeholder(...)} tags with server information
 */
public class Placeholders {

    /**
     * Replace all server placeholders
     */
    public String replaceServerPlaceholders(String text) {
        // Time placeholder
        if (text.contains("{placeholder(time)}")) {
            org.bukkit.World world = Bukkit.getWorld("world");
            if (world != null) {
                text = text.replace("{placeholder(time)}", String.valueOf(world.getTime()));
            }
        }
        
        // Online players
        if (text.contains("{placeholder(online)}")) {
            text = text.replace("{placeholder(online)}", String.valueOf(Bukkit.getOnlinePlayers().size()));
        }
        
        // Max players
        if (text.contains("{placeholder(online_max)}")) {
            text = text.replace("{placeholder(online_max)}", String.valueOf(Bukkit.getMaxPlayers()));
        }
        
        // World online players
        if (text.contains("{placeholder(worldonline)}")) {
            text = text.replace("{placeholder(worldonline)}", String.valueOf(Bukkit.getOnlinePlayers().size()));
        }
        
        // Server uptime
        if (text.contains("{placeholder(server_uptime)}")) {
            text = text.replace("{placeholder(server_uptime)}", getServerUptime());
        }
        
        // MSPT
        if (text.contains("{placeholder(mspt)}")) {
            text = text.replace("{placeholder(mspt)}", String.format("%.2f", getMSPT()));
        }
        
        // TPS
        if (text.contains("{placeholder(tps)}")) {
            text = text.replace("{placeholder(tps)}", String.format("%.2f", getTPS()));
        }
        
        // Loaded plugins
        if (text.contains("{placeholder(plugins)}")) {
            text = text.replace("{placeholder(plugins)}", getLoadedPlugins());
        }
        
        return text;
    }

    /**
     * Get server uptime in human-readable format
     */
    private String getServerUptime() {
        long uptime = System.currentTimeMillis() / 1000;
        long days = uptime / 86400;
        long hours = (uptime % 86400) / 3600;
        long minutes = (uptime % 3600) / 60;
        long seconds = uptime % 60;
        
        if (days > 0) {
            return days + "d " + hours + "h " + minutes + "m";
        } else if (hours > 0) {
            return hours + "h " + minutes + "m " + seconds + "s";
        } else if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        } else {
            return seconds + "s";
        }
    }

    /**
     * Get MSPT (milliseconds per tick)
     * Lower is better, should be <50ms for smooth gameplay
     */
    private double getMSPT() {
        // Simplified implementation
        // In a real scenario, you'd track actual tick times
        return 0.0;
    }

    /**
     * Get TPS (ticks per second)
     * Should be 20.0 for normal operation
     */
    private double getTPS() {
        // Simplified implementation
        // In a real scenario, you'd calculate from recent tick times
        return 20.0;
    }

    /**
     * Get loaded plugins list
     */
    private String getLoadedPlugins() {
        StringBuilder plugins = new StringBuilder();
        for (org.bukkit.plugin.Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            if (plugins.length() > 0) plugins.append(", ");
            plugins.append(plugin.getName());
        }
        return plugins.toString();
    }
}

# parser/CommandParser
package me.coder.codedsl.parser;

import me.coder.api.CoderAPI;
import me.coder.codedsl.CodeDSLProcessor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses CodeDSL command definitions from script files
 */
public class CommandParser {

    private final CoderAPI api;
    private final File scriptsFolder;
    private final Map<String, CodeDSLCommandDef> registeredCommands = new HashMap<>();

    public CommandParser(CoderAPI api, File scriptsFolder) {
        this.api = api;
        this.scriptsFolder = scriptsFolder;
    }

    /**
     * Parse command definitions from a script file (with console logs)
     */
    public void parseCommandDefinitions(File scriptFile) {
        parseCommandDefinitionsInternal(scriptFile, false);
    }

    /**
     * Parse command definitions cleanly without printing confirmation lines
     */
    public void parseCommandDefinitionsSilent(File scriptFile) {
        parseCommandDefinitionsInternal(scriptFile, true);
    }

    private void parseCommandDefinitionsInternal(File scriptFile, boolean silent) {
        try {
            List<String> lines = Files.readAllLines(scriptFile.toPath());
            Pattern cmdPattern = Pattern.compile("command\\s+(/[a-zA-Z0-9_]+)\\s*(.*)");

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                Matcher matcher = cmdPattern.matcher(line);

                if (matcher.find()) {
                    String rawCmdName = matcher.group(1);
                    
                    // Strip the leading slash so it maps correctly across internal queries
                    String cmdName = rawCmdName.startsWith("/") ? rawCmdName.substring(1) : rawCmdName;
                    
                    CodeDSLCommandDef cmdDef = new CodeDSLCommandDef(cmdName, scriptFile.getName());
                    
                    // Parse command properties
                    while (i < lines.size() - 1) {
                        i++;
                        String nextLine = lines.get(i).trim();
                        
                        if (nextLine.startsWith("permission:")) {
                            cmdDef.permission = nextLine.substring(11).trim();
                        } else if (nextLine.startsWith("trigger:")) {
                            cmdDef.hasTrigger = true;
                            break;
                        } else if (nextLine.isEmpty()) {
                            break;
                        }
                    }

                    registeredCommands.put(cmdName.toLowerCase(), cmdDef);
                    if (!silent && api != null) {
                        api.log("[CodeDSL] Command registered: " + cmdName);
                    }
                }
            }
        } catch (Exception e) {
            if (api != null) {
                api.logError("Error parsing commands: " + e.getMessage());
            }
        }
    }

    /**
     * Check if this is a CodeDSL registered command
     */
    public boolean isCodeDSLCommand(String message) {
        String cleanMessage = message.trim();
        if (cleanMessage.startsWith("/")) {
            cleanMessage = cleanMessage.substring(1);
        }
        
        String[] parts = cleanMessage.split(" ");
        if (parts.length == 0) return false;

        String cmdName = parts[0].toLowerCase();
        return registeredCommands.containsKey(cmdName);
    }

    /**
     * Process a CodeDSL command and execute its script file via the execution engine
     */
    public void processCommand(CommandSender sender, String message, CodeDSLProcessor processor) {
        String cleanMessage = message.trim();
        if (cleanMessage.startsWith("/")) {
            cleanMessage = cleanMessage.substring(1);
        }

        String[] parts = cleanMessage.split(" ", 2);
        String cmdName = parts[0].toLowerCase();

        if (!registeredCommands.containsKey(cmdName)) {
            sender.sendMessage("§cUnknown command: " + cmdName);
            return;
        }

        CodeDSLCommandDef cmdDef = registeredCommands.get(cmdName);

        // Check permission
        if (cmdDef.permission != null && !cmdDef.permission.isEmpty()) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (!player.hasPermission(cmdDef.permission)) {
                    sender.sendMessage("§cYou don't have permission to use this command!");
                    return;
                }
            }
        }

        // Execute the script that defines this command
        sender.sendMessage("§aExecuting CodeDSL command: " + cmdName);
        if (api != null) {
            api.log("[CodeDSL] Command executed: " + cmdName + " by " + sender.getName());
        }

        // Target the physical script file mapped inside the definitions block and pass off execution
        File scriptFile = new File(scriptsFolder, cmdDef.scriptFile);
        if (scriptFile.exists()) {
            processor.executeScript(scriptFile, sender);
        } else {
            sender.sendMessage("§cCould not locate source script file: " + cmdDef.scriptFile);
        }
    }

    /**
     * CodeDSL Command Definition
     */
    public static class CodeDSLCommandDef {
        public String name;
        public String scriptFile;
        public String permission = "";
        public boolean hasTrigger = false;

        public CodeDSLCommandDef(String name, String scriptFile) {
            this.name = name;
            this.scriptFile = scriptFile;
        }
    }

    /**
     * Get registered commands
     */
    public Collection<CodeDSLCommandDef> getRegisteredCommands() {
        return registeredCommands.values();
    }

    /**
     * Unregister all commands
     */
    public void unregisterAllCommands() {
        registeredCommands.clear();
    }
}

# manager/PlaceholderManager
package me.coder.codedsl.manager;

import me.coder.api.CoderAPI;
import me.coder.codedsl.placeholders.Placeholders;
import org.bukkit.Bukkit;
import java.util.Map;

/**
 * Manages placeholder replacements for CodeDSL scripts
 */
public class PlaceholderManager {

    private final CoderAPI api;
    private final Map<String, String> variables;
    private final Map<String, String> obfuscatedVars;
    private final Placeholders placeholders;

    public PlaceholderManager(CoderAPI api, Map<String, String> variables, Map<String, String> obfuscatedVars) {
        this.api = api;
        this.variables = variables;
        this.obfuscatedVars = obfuscatedVars;
        this.placeholders = new Placeholders();
    }

    /**
     * Replace all placeholders in text
     */
    public String replacePlaceholders(String text) {
        // Replace variable placeholders: {var(name)}
        text = replaceVariablePlaceholders(text);
        
        // Replace obfuscated variable placeholders: {obfvar(name)}
        text = replaceObfuscatedPlaceholders(text);
        
        // Replace server placeholders: {placeholder(...)}
        text = placeholders.replaceServerPlaceholders(text);
        
        return text;
    }

    /**
     * Replace variable placeholders: {var(name)}
     */
    private String replaceVariablePlaceholders(String text) {
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            text = text.replace("{var(" + entry.getKey() + ")}", entry.getValue());
            text = text.replace("{placeholder(var_" + entry.getKey() + ")}", entry.getValue());
        }
        return text;
    }

    /**
     * Replace obfuscated variable placeholders: {obfvar(name)}
     */
    private String replaceObfuscatedPlaceholders(String text) {
        for (Map.Entry<String, String> entry : obfuscatedVars.entrySet()) {
            try {
                String decoded = new String(java.util.Base64.getDecoder().decode(entry.getValue()));
                text = text.replace("{obfvar(" + entry.getKey() + ")}", decoded);
                text = text.replace("{placeholder(obfvar_" + entry.getKey() + ")}", decoded);
            } catch (Exception e) {
                // Ignore decode errors
            }
        }
        return text;
    }
}

# manager/ScriptManager
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

# manager/CommandHandlerStorage
package me.coder.codedsl.manager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CommandHandlerStorage {
    
    private static final Map<String, Object> COMMAND_HANDLERS = new ConcurrentHashMap<>();
    private static final Map<String, List<String>> SCRIPT_COMMAND_MAP = new ConcurrentHashMap<>();
    
    public static void registerCommandHandler(String commandName, Object handler, String scriptFile) {
        String key = commandName.toLowerCase();
        COMMAND_HANDLERS.put(key, handler);
        SCRIPT_COMMAND_MAP.computeIfAbsent(scriptFile, k -> new ArrayList<>()).add(key);
        System.out.println("[CodeDSL] Registered command handler: /" + key);
    }
    
    public static boolean executeCommandHandler(String commandName, org.bukkit.command.CommandSender sender, 
                                               me.coder.codedsl.CodeDSLProcessor processor) {
        String key = commandName.toLowerCase();
        Object handler = COMMAND_HANDLERS.get(key);
        
        if (handler == null) {
            sender.sendMessage("§cCommand handler not found: /" + commandName);
            return false;
        }
        
        try {
            if (handler instanceof java.io.File) {
                processor.executeScript((java.io.File) handler, sender);
            }
            return true;
        } catch (Exception e) {
            sender.sendMessage("§cError executing command /" + commandName + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public static void unloadScriptHandlers(String scriptFile) {
        List<String> commands = SCRIPT_COMMAND_MAP.remove(scriptFile);
        if (commands != null) {
            for (String cmd : commands) {
                COMMAND_HANDLERS.remove(cmd);
                System.out.println("[CodeDSL] Unregistered command handler: /" + cmd);
            }
        }
    }
    
    public static boolean hasCommandHandler(String commandName) {
        return COMMAND_HANDLERS.containsKey(commandName.toLowerCase());
    }
    
    public static Set<String> getRegisteredCommands() {
        return new HashSet<>(COMMAND_HANDLERS.keySet());
    }
    
    public static Object getCommandHandler(String commandName) {
        return COMMAND_HANDLERS.get(commandName.toLowerCase());
    }
    
    public static void clearAll() {
        COMMAND_HANDLERS.clear();
        SCRIPT_COMMAND_MAP.clear();
        System.out.println("[CodeDSL] All command handlers cleared");
    }
    
    public static int getRegisteredCommandCount() {
        return COMMAND_HANDLERS.size();
    }
}

# manager/VariableManager
package me.coder.codedsl.manager;

import me.coder.api.CoderAPI;
import java.io.File;
import java.nio.file.Files;
import java.util.*;

/**
 * Manages variable storage and persistence for CodeDSL
 * Handles both plain and obfuscated variables
 */
public class VariableManager {

    private final File variablesFolder;
    private final File variablesStorageFile;
    private final File variablesObfFile;
    private final CoderAPI api;
    
    private final Map<String, String> variables = new HashMap<>();
    private final Map<String, String> obfuscatedVariables = new HashMap<>();

    public VariableManager(File dataFolder, CoderAPI api) {
        this.variablesFolder = new File(dataFolder, "variables");
        this.variablesStorageFile = new File(variablesFolder, "variables.storage");
        this.variablesObfFile = new File(variablesFolder, "variables.obf");
        this.api = api;

        if (!variablesFolder.exists()) {
            variablesFolder.mkdirs();
        }

        loadVariables();
    }

    /**
     * Store a plain variable
     */
    public void setVariable(String name, String value) {
        variables.put(name, value);
        saveVariables();
        api.log("[CodeDSL] Variable stored: " + name + " = " + value);
    }

    /**
     * Get a plain variable
     */
    public String getVariable(String name) {
        return variables.get(name);
    }

    /**
     * Check if plain variable exists
     */
    public boolean hasVariable(String name) {
        return variables.containsKey(name);
    }

    /**
     * Delete a plain variable
     */
    public void deleteVariable(String name) {
        if (variables.remove(name) != null) {
            saveVariables();
            api.log("[CodeDSL] Variable deleted: " + name);
        }
    }

    /**
     * Store an obfuscated variable (Base64 encoded)
     */
    public void setObfuscatedVariable(String name, String value) {
        String encoded = Base64.getEncoder().encodeToString(value.getBytes());
        obfuscatedVariables.put(name, encoded);
        saveObfuscatedVariables();
        api.log("[CodeDSL] Obfuscated variable stored: " + name);
    }

    /**
     * Get an obfuscated variable (decoded)
     */
    public String getObfuscatedVariable(String name) {
        String encoded = obfuscatedVariables.get(name);
        if (encoded != null) {
            try {
                return new String(Base64.getDecoder().decode(encoded));
            } catch (IllegalArgumentException e) {
                api.logError("Failed to decode obfuscated variable: " + name);
                return null;
            }
        }
        return null;
    }

    /**
     * Check if obfuscated variable exists
     */
    public boolean hasObfuscatedVariable(String name) {
        return obfuscatedVariables.containsKey(name);
    }

    /**
     * Delete an obfuscated variable
     */
    public void deleteObfuscatedVariable(String name) {
        if (obfuscatedVariables.remove(name) != null) {
            saveObfuscatedVariables();
            api.log("[CodeDSL] Obfuscated variable deleted: " + name);
        }
    }

    /**
     * Load all variables from storage files
     */
    private void loadVariables() {
        // Load plain variables
        if (variablesStorageFile.exists()) {
            try {
                List<String> lines = Files.readAllLines(variablesStorageFile.toPath());
                for (String line : lines) {
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        variables.put(parts[0].trim(), parts[1].trim());
                    }
                }
                api.log("[CodeDSL] Loaded " + variables.size() + " plain variables");
            } catch (Exception e) {
                api.logError("Error loading plain variables: " + e.getMessage());
            }
        }

        // Load obfuscated variables
        if (variablesObfFile.exists()) {
            try {
                List<String> lines = Files.readAllLines(variablesObfFile.toPath());
                for (String line : lines) {
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        obfuscatedVariables.put(parts[0].trim(), parts[1].trim());
                    }
                }
                api.log("[CodeDSL] Loaded " + obfuscatedVariables.size() + " obfuscated variables");
            } catch (Exception e) {
                api.logError("Error loading obfuscated variables: " + e.getMessage());
            }
        }
    }

    /**
     * Save all plain variables to storage file
     */
    private void saveVariables() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("# CodeDSL Plain Variables Storage\n");
            sb.append("# Format: name=value\n\n");
            
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                sb.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
            }
            
            Files.write(variablesStorageFile.toPath(), sb.toString().getBytes());
        } catch (Exception e) {
            api.logError("Error saving plain variables: " + e.getMessage());
        }
    }

    /**
     * Save all obfuscated variables to storage file
     */
    private void saveObfuscatedVariables() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("# CodeDSL Obfuscated Variables Storage (Base64)\n");
            sb.append("# Format: name=encoded_value\n\n");
            
            for (Map.Entry<String, String> entry : obfuscatedVariables.entrySet()) {
                sb.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
            }
            
            Files.write(variablesObfFile.toPath(), sb.toString().getBytes());
        } catch (Exception e) {
            api.logError("Error saving obfuscated variables: " + e.getMessage());
        }
    }

    /**
     * Replace variable placeholders in text: {var(name)} or {obfvar(name)}
     */
    public String replacePlaceholders(String text) {
        // Replace plain variables: {var(name)}
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            text = text.replace("{var(" + entry.getKey() + ")}", entry.getValue());
        }

        // Replace obfuscated variables: {obfvar(name)}
        for (String name : obfuscatedVariables.keySet()) {
            String decoded = getObfuscatedVariable(name);
            if (decoded != null) {
                text = text.replace("{obfvar(" + name + ")}", decoded);
            }
        }

        return text;
    }

    /**
     * Get all variables
     */
    public Map<String, String> getAllVariables() {
        return new HashMap<>(variables);
    }

    /**
     * Clear all variables
     */
    public void clearAll() {
        variables.clear();
        obfuscatedVariables.clear();
        saveVariables();
        saveObfuscatedVariables();
        api.log("[CodeDSL] All variables cleared");
    }
}

# listeners/CommandInterceptor
package me.coder.codedsl.listeners;

import me.coder.api.CoderAPI;
import me.coder.codedsl.CodeDSLAddon;
import me.coder.codedsl.CodeDSLProcessor;
import me.coder.codedsl.manager.ScriptManager;
import me.coder.codedsl.parser.CommandParser;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

/**
 * Intercepts commands to handle CodeDSL command registrations and cross-plugin /coder & /code redirects.
 */
public class CommandInterceptor implements Listener {

    private final CoderAPI api;
    private final CommandParser commandParser;
    private final ScriptManager scriptManager;
    private final CodeDSLProcessor processor;
    private final CodeDSLAddon addon;

    public CommandInterceptor(CoderAPI api, CommandParser commandParser, ScriptManager scriptManager, CodeDSLProcessor processor, CodeDSLAddon addon) {
        this.api = api;
        this.commandParser = commandParser;
        this.scriptManager = scriptManager;
        this.processor = processor;
        this.addon = addon;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        
        // 1. Intercept /coder run or /code run paths and redirect them to loadScript
        if (shouldRedirectCoderRun(message)) {
            event.setCancelled(true);
            executeRedirect(event.getPlayer(), message);
            return;
        }
        
        // 2. Check if this is a standard CodeDSL script-registered command
        if (commandParser.isCodeDSLCommand(message)) {
            commandParser.processCommand(event.getPlayer(), message, processor);
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerCommand(ServerCommandEvent event) {
        String command = event.getCommand();
        
        // 1. Intercept /coder run or /code run console commands and redirect them to loadScript
        if (shouldRedirectCoderRun(command)) {
            event.setCancelled(true);
            executeRedirect(event.getSender(), command);
            return;
        }
        
        // 2. Check if this is a standard CodeDSL script-registered command
        if (commandParser.isCodeDSLCommand(command)) {
            commandParser.processCommand(event.getSender(), command, processor);
            // Fix: Cancel the event so the server doesn't try to parse the unknown command
            event.setCancelled(true); 
            event.setCommand(""); 
        }
    }

    /**
     * Helper to verify if the command pattern matches '/coder run ' or '/code run ' 
     * targeting a configured file extension.
     */
    private boolean shouldRedirectCoderRun(String rawCommand) {
        String lower = rawCommand.toLowerCase().trim();
        if (lower.startsWith("/")) {
            lower = lower.substring(1);
        }

        if (lower.startsWith("coder run ") || lower.startsWith("code run ")) {
            String[] splitArgs = lower.split("\\s+");
            if (splitArgs.length >= 3) {
                String fileTarget = splitArgs[2].toLowerCase();
                
                YamlConfiguration config = addon.getAddonConfig();
                if (config == null) return fileTarget.endsWith(".cd") || fileTarget.endsWith(".code");

                // Pull configuration definitions
                String mainExt = config.getString("file-extensions.main", ".cd").toLowerCase();
                String legacyExt = config.getString("file-extensions.legacy", ".code").toLowerCase();
                String oldExt = config.getString("file-extensions.old", ".cdsl").toLowerCase();
                String customExt = config.getString("file-extensions.custom", "").toLowerCase();

                // 1. Match core extensions
                if (fileTarget.endsWith(mainExt) || fileTarget.endsWith(legacyExt) || fileTarget.endsWith(oldExt)) {
                    return true;
                }

                // 2. Safely match custom extension slot if it passes guards
                if (!customExt.isEmpty() 
                    && !customExt.equals("your_prefered_custom_file_extension_here") 
                    && customExt.startsWith(".")) {
                    return fileTarget.endsWith(customExt);
                }
            }
        }
        return false;
    }

    /**
     * Isolates the file name element out of the command args string and runs the redirection logic.
     */
    private void executeRedirect(CommandSender sender, String originalCommand) {
        String cleanCommand = originalCommand.trim();
        if (cleanCommand.startsWith("/")) {
            cleanCommand = cleanCommand.substring(1);
        }

        String[] splitArgs = cleanCommand.split("\\s+");
        if (splitArgs.length >= 3) {
            String targetFileName = splitArgs[2];
            
            if (api != null) {
                api.sendSuccess(sender, "[CodeDSL] Intercepted run command alias -> Redirecting file loading path...");
            } else {
                sender.sendMessage("§7[CodeDSL] Intercepted run command alias -> Redirecting file loading path...");
            }
            
            scriptManager.loadScript(targetFileName, sender);
        }
    }
}

# libraries/AsyncLibrary
package me.coder.codedsl.libraries;

/**
 * Async execution library for CodeDSL
 * Allows scripts to run on different threads
 */
public class AsyncLibrary implements LibraryRegistry.CodeDSLLibrary {

    @Override
    public String getName() {
        return "async";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public void onLoad() {
        // Initialize async library
    }

    @Override
    public void onUnload() {
        // Cleanup
    }

    /**
     * Run task asynchronously
     */
    public static void runAsync(Runnable runnable) {
        // This would be implemented with the plugin scheduler
        new Thread(runnable).start();
    }

    /**
     * Run task with delay
     */
    public static void runAsyncDelayed(Runnable runnable, long delayMillis) {
        new Thread(() -> {
            try {
                Thread.sleep(delayMillis);
                runnable.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    /**
     * Run task repeatedly
     */
    public static void runAsyncTimer(Runnable runnable, long delayMillis, long periodMillis) {
        new Thread(() -> {
            try {
                Thread.sleep(delayMillis);
                while (true) {
                    runnable.run();
                    Thread.sleep(periodMillis);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}

# libraries/BukkitLibrary
package me.coder.codedsl.libraries;

/**
 * Bukkit integration library for CodeDSL
 * Provides access to Bukkit API functions
 */
public class BukkitLibrary implements LibraryRegistry.CodeDSLLibrary {

    @Override
    public String getName() {
        return "bukkit";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public void onLoad() {
        // Initialize Bukkit library
    }

    @Override
    public void onUnload() {
        // Cleanup
    }

    /**
     * Get player by name
     */
    public static org.bukkit.entity.Player getPlayer(String name) {
        return org.bukkit.Bukkit.getPlayer(name);
    }

    /**
     * Broadcast message
     */
    public static void broadcast(String message) {
        org.bukkit.Bukkit.broadcastMessage(message);
    }

    /**
     * Execute command
     */
    public static void executeCommand(String command) {
        org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(), command);
    }

    /**
     * Get online players
     */
    public static int getOnlineCount() {
        return org.bukkit.Bukkit.getOnlinePlayers().size();
    }

    /**
     * Schedule async task
     */
    public static void scheduleAsync(Runnable runnable, long delay) {
        // This would be implemented with the plugin scheduler
    }
}

# libraries/COnnectionLibrary
package me.coder.codedsl.libraries;

import java.util.*;

/**
 * Connection library for CodeDSL
 * Allows scripts to communicate with each other
 */
public class ConnectionLibrary implements LibraryRegistry.CodeDSLLibrary {

    private static final Map<String, String> scriptConnections = new HashMap<>();

    @Override
    public String getName() {
        return "connection";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public void onLoad() {
        // Initialize connection library
    }

    @Override
    public void onUnload() {
        // Cleanup connections
        scriptConnections.clear();
    }

    /**
     * Connect to another script file
     */
    public static boolean connectToFile(String fileName) {
        scriptConnections.put(fileName, "connected");
        return true;
    }

    /**
     * Check if file is connected
     */
    public static boolean isConnected(String fileName) {
        return scriptConnections.containsKey(fileName);
    }

    /**
     * Disconnect from script file
     */
    public static void disconnect(String fileName) {
        scriptConnections.remove(fileName);
    }

    /**
     * Send message to another script
     */
    public static void sendMessage(String fileName, String message) {
        if (isConnected(fileName)) {
            // Message would be stored and retrieved by target script
            scriptConnections.put(fileName + ":message", message);
        }
    }

    /**
     * Receive message from another script
     */
    public static String receiveMessage(String fileName) {
        return scriptConnections.remove(fileName + ":message");
    }

    /**
     * Get all connected scripts
     */
    public static Collection<String> getConnectedScripts() {
        List<String> connected = new ArrayList<>();
        for (String key : scriptConnections.keySet()) {
            if (!key.contains(":")) {
                connected.add(key);
            }
        }
        return connected;
    }
}

# libraries/CustomImportScanner
package me.coder.codedsl.libraries;

import me.coder.api.CoderAPI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scans and processes import statements in CodeDSL scripts
 */
public class CustomImportScanner {

    private final LibraryRegistry libraryRegistry;
    private final CoderAPI api;
    private final Set<String> loadedLibraries = new HashSet<>();

    public CustomImportScanner(LibraryRegistry libraryRegistry, CoderAPI api) {
        this.libraryRegistry = libraryRegistry;
        this.api = api;
    }

    /**
     * Scan script lines for import statements
     */
    public void scanImports(List<String> scriptLines) {
        Pattern importPattern = Pattern.compile("import\\s+([a-zA-Z0-9_]+)");

        for (String line : scriptLines) {
            Matcher matcher = importPattern.matcher(line.trim());
            if (matcher.find()) {
                String libraryName = matcher.group(1).toLowerCase();
                loadLibrary(libraryName);
            }
        }
    }

    /**
     * Load a library by name
     */
    private void loadLibrary(String name) {
        if (loadedLibraries.contains(name)) {
            return; // Already loaded
        }

        if (libraryRegistry.hasLibrary(name)) {
            LibraryRegistry.CodeDSLLibrary lib = libraryRegistry.getLibrary(name);
            lib.onLoad();
            loadedLibraries.add(name);
            api.log("[CodeDSL] Library loaded: " + name);
        } else {
            api.logWarning("[CodeDSL] Unknown library: " + name);
        }
    }

    /**
     * Get loaded libraries
     */
    public Set<String> getLoadedLibraries() {
        return new HashSet<>(loadedLibraries);
    }

    /**
     * Unload all loaded libraries
     */
    public void unloadAll() {
        for (String libName : loadedLibraries) {
            LibraryRegistry.CodeDSLLibrary lib = libraryRegistry.getLibrary(libName);
            if (lib != null) {
                lib.onUnload();
            }
        }
        loadedLibraries.clear();
    }

    /**
     * Check if a library is imported
     */
    public boolean isImported(String name) {
        return loadedLibraries.contains(name.toLowerCase());
    }
}

# libraries/LibraryRegistry
package me.coder.codedsl.libraries;

import me.coder.api.CoderAPI;
import java.util.*;

/**
 * Registry for CodeDSL libraries and imports
 */
public class LibraryRegistry {

    private final Map<String, CodeDSLLibrary> libraries = new HashMap<>();
    private final CoderAPI api;

    public LibraryRegistry(CoderAPI api) {
        this.api = api;
        registerDefaultLibraries();
    }

    /**
     * Register default CodeDSL libraries
     */
    private void registerDefaultLibraries() {
        registerLibrary("bukkit", new BukkitLibrary());
        registerLibrary("async", new AsyncLibrary());
        registerLibrary("connection", new ConnectionLibrary());
    }

    /**
     * Register a custom library
     */
    public void registerLibrary(String name, CodeDSLLibrary library) {
        libraries.put(name.toLowerCase(), library);
        api.log("[CodeDSL] Library registered: " + name);
    }

    /**
     * Get a library by name
     */
    public CodeDSLLibrary getLibrary(String name) {
        return libraries.get(name.toLowerCase());
    }

    /**
     * Check if library is registered
     */
    public boolean hasLibrary(String name) {
        return libraries.containsKey(name.toLowerCase());
    }

    /**
     * Get all registered libraries
     */
    public Collection<CodeDSLLibrary> getLibraries() {
        return libraries.values();
    }

    /**
     * Library interface
     */
    public interface CodeDSLLibrary {
        String getName();
        String getVersion();
        void onLoad();
        void onUnload();
    }
}

# commands/CodeDSLCommand
package me.coder.codedsl.commands;

import me.coder.api.CoderAPI;
import me.coder.codedsl.CodeDSLPlugin;
import me.coder.codedsl.manager.ScriptManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class CodeDSLCommand implements CommandExecutor {
    
    private static ScriptManager scriptManager;
    private static CoderAPI api;
    private static File dataFolder;
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, 
                           @NotNull String label, @NotNull String[] args) {
        
        if (args.length == 0) {
            sender.sendMessage("§b--- CodeDSL Commands ---");
            sender.sendMessage("§a/codedsl run <filename> §7- Run a script");
            sender.sendMessage("§a/codedsl load <filename> §7- Load a script");
            sender.sendMessage("§a/codedsl unload <filename> §7- Unload a script");
            sender.sendMessage("§a/codedsl reload <filename> §7- Reload a script");
            sender.sendMessage("§a/codedsl list §7- List loaded scripts");
            sender.sendMessage("§a/codedsl configreload §7- Reload config");
            return true;
        }
        
        String subcommand = args[0].toLowerCase();
        
        switch (subcommand) {
            case "run":
                return handleRun(sender, args);
            case "load":
                return handleLoad(sender, args);
            case "unload":
                return handleUnload(sender, args);
            case "list":
                return handleList(sender);
            case "reload":
                return handleReload(sender, args);
            case "configreload":
                return handleConfigReload(sender);
            default:
                sender.sendMessage("§cUnknown subcommand: " + subcommand);
                return false;
        }
    }
    
    private boolean handleRun(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /codedsl run <filename>");
            return false;
        }
        
        try {
            String filename = args[1];
            if (api != null) {
                api.log("Loading script: " + filename);
            }
            scriptManager.loadScript(filename, sender);
            sender.sendMessage("§aScript loaded: " + filename);
            return true;
        } catch (Exception e) {
            sender.sendMessage("§cError running script: " + e.getMessage());
            if (api != null) {
                api.logError("Script execution failed: " + e.getMessage());
            }
            return false;
        }
    }
    
    private boolean handleLoad(CommandSender sender, String[] args) {
        return handleRun(sender, args);
    }
    
    private boolean handleUnload(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /codedsl unload <filename>");
            return false;
        }
        
        try {
            String filename = args[1];
            scriptManager.unloadScript(filename, sender);
            sender.sendMessage("§aScript unloaded: " + filename);
            return true;
        } catch (Exception e) {
            sender.sendMessage("§cError unloading script: " + e.getMessage());
            return false;
        }
    }
    
    private boolean handleList(CommandSender sender) {
        try {
            var scripts = scriptManager.getLoadedScripts();
            
            if (scripts.isEmpty()) {
                sender.sendMessage("§bNo scripts loaded.");
                return true;
            }
            
            sender.sendMessage("§b--- Loaded Scripts ---");
            for (String script : scripts) {
                sender.sendMessage("§a• " + script);
            }
            return true;
        } catch (Exception e) {
            sender.sendMessage("§cError listing scripts: " + e.getMessage());
            return false;
        }
    }
    
    private boolean handleReload(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /codedsl reload <filename>");
            return false;
        }
        
        try {
            String filename = args[1];
            scriptManager.unloadScript(filename, sender);
            sender.sendMessage("§eUnloading " + filename + "...");
            scriptManager.loadScript(filename, sender);
            sender.sendMessage("§aScript reloaded: " + filename);
            return true;
        } catch (Exception e) {
            sender.sendMessage("§cError reloading script: " + e.getMessage());
            return false;
        }
    }
    
    private boolean handleConfigReload(CommandSender sender) {
        try {
            CodeDSLPlugin plugin = CodeDSLPlugin.getInstance();
            
            if (plugin == null || plugin.getAddonInstance() == null) {
                sender.sendMessage("§cPlugin not initialized");
                return false;
            }
            
            plugin.getAddonInstance().reloadConfig();
            sender.sendMessage("§a✓ Configuration reloaded");
            
            if (api != null) {
                api.log("Config reloaded by " + sender.getName());
            }
            
            return true;
        } catch (Exception e) {
            sender.sendMessage("§c✗ Error: " + e.getMessage());
            if (api != null) {
                api.logError("Config reload failed: " + e.getMessage());
            }
            return false;
        }
    }
    
    public static void register(org.bukkit.plugin.Plugin plugin, ScriptManager scriptMgr, 
                               CoderAPI apiInstance, File folder) {
        try {
            scriptManager = scriptMgr;
            api = apiInstance;
            dataFolder = folder;
            
            org.bukkit.command.PluginCommand cmd = plugin.getServer().getPluginCommand("codedsl");
            if (cmd != null) {
                cmd.setExecutor(new CodeDSLCommand());
                if (api != null) api.log("CodeDSL commands registered");
            } else {
                if (api != null) api.logError("Failed to register /codedsl command: not found in plugin.yml");
            }
        } catch (Exception e) {
            if (api != null) api.logError("Failed to register commands: " + e.getMessage());
        }
    }
}

# pom.xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>me.coder.codedsl</groupId>
    <artifactId>CodeDSL</artifactId>
    <version>1.9.5</version>
    <packaging>jar</packaging>

    <name>CodeDSL</name>
    <description>A Domain Specific Language addon for the Coder plugin</description>

    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <repositories>
        <repository>
            <id>papermc-repo</id>
            <url>https://repo.papermc.io/repository/maven-public/</url>
        </repository>
    </repositories>

    <dependencies>
        <!-- Paper API -->
        <dependency>
            <groupId>io.papermc.paper</groupId>
            <artifactId>paper-api</artifactId>
            <version>1.21-R0.1-SNAPSHOT</version>
            <scope>provided</scope>
        </dependency>

        <!-- Coder Plugin API -->
        <dependency>
            <groupId>me.coder</groupId>
            <artifactId>Coder</artifactId>
            <version>1.4.2</version>
            <scope>system</scope>
            <systemPath>${basedir}/libs/Coder-1.4.2.jar</systemPath>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Java Compiler Plugin -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>21</source>
                    <target>21</target>
                </configuration>
            </plugin>

            <!-- Shade Plugin for bundling dependencies -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.5.0</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>shade</goal>
                        </goals>
                        <configuration>
                            <transformers>
                                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                    <mainClass>me.coder.codedsl.CodeDSLAddon</mainClass>
                                </transformer>
                            </transformers>
                        </configuration>
                    </execution>
                </executions>
            </plugin>

            <!-- Jar Plugin -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <version>3.3.0</version>
                <configuration>
                    <archive>
                        <manifest>
                            <mainClass>me.coder.codedsl.CodeDSLAddon</mainClass>
                        </manifest>
                    </archive>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>

# paper-plugin.yml
name: CodeDSL
version: 1.9.5
main: me.coder.codedsl.CodeDSLPlugin
description: A Domain Specific Language addon for the Coder plugin
author: Firesmasher
api-version: '1.21'

dependencies:
  server:
    Coder:
      load: BEFORE
      required: true

commands:
  codedsl:
    description: Execute and manage CodeDSL scripts
    usage: /codedsl <run|reload|load|unload|list> <filename>
    aliases:
      - cdsl
      - code-dsl

permissions:
  codedsl.use:
    description: Allows usage of CodeDSL commands
    default: op
  codedsl.command:
    description: Allows running CodeDSL commands
    default: op

