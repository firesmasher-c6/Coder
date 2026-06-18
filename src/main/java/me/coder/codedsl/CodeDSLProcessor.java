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