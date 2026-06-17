package me.coder.codedsl;

import me.coder.api.CoderAPI;
import org.bukkit.command.CommandSender;
import java.io.File;
import java.io.IOException;
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

    public CodeDSLProcessor(File dataFolder, CoderAPI api) {
        this.dataFolder = dataFolder;
        this.api = api;
        loadVariables();
    }

    /**
     * Execute a CodeDSL script file
     */
    public void executeScript(File scriptFile, CommandSender sender) {
        try {
            List<String> lines = Files.readAllLines(scriptFile.toPath());
            processLines(lines, sender);
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
                    // Process indented block
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
                        // Process indented block
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

            // Handle console.log command
            else if (line.startsWith("console.log ")) {
                String message = line.substring(12).trim();
                // Handle multi-line: console.log { ... }
                if (message.startsWith("{")) {
                    message = message.substring(1);
                    if (message.endsWith("}")) {
                        message = message.substring(0, message.length() - 1);
                    }
                } else if (message.startsWith("\"") && message.endsWith("\"")) {
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

            // Handle if statements with indented blocks
            else if (line.startsWith("if ")) {
                String condition = line.substring(3).trim();
                if (condition.endsWith(":")) {
                    condition = condition.substring(0, condition.length() - 1).trim();
                }
                
                // Parse: var "name" = "value"
                if (condition.contains(" = ")) {
                    String[] parts = condition.split(" = ");
                    if (parts.length >= 2) {
                        String varPart = parts[0].replace("var", "").trim();
                        String valuePart = parts[1].trim();
                        
                        // Remove quotes
                        if (varPart.startsWith("\"") && varPart.endsWith("\"")) {
                            varPart = varPart.substring(1, varPart.length() - 1);
                        }
                        if (valuePart.startsWith("\"") && valuePart.endsWith("\"")) {
                            valuePart = valuePart.substring(1, valuePart.length() - 1);
                        }
                        
                        String actualValue = variables.get(varPart);
                        boolean conditionMet = actualValue != null && actualValue.equals(valuePart);
                        
                        // Process if block
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

            // Skip empty lines
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                i++;
                continue;
            }

            int currentIndent = getIndentation(line);
            
            // If not indented more than parent, exit block
            if (currentIndent <= baseIndent) {
                break;
            }

            // Process indented line
            String cleanedLine = trimmed;
            
            if (cleanedLine.startsWith("console.log ")) {
                String message = cleanedLine.substring(12).trim();
                // Extract string from quotes
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

            // Skip empty lines
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                i++;
                continue;
            }

            int currentIndent = getIndentation(line);
            
            // If not indented more than parent, exit block
            if (currentIndent <= baseIndent) {
                break;
            }

            // Check for else:
            if (trimmed.startsWith("else:") || trimmed.equals("else:")) {
                inIfBlock = false;
                inElseBlock = true;
                i++;
                continue;
            }

            // Process lines based on condition
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
     * Parse variable assignment
     */
    private void parseVariable(String statement, boolean obfuscated) {
        String[] parts = statement.split("=", 2);
        if (parts.length == 2) {
            String name = parts[0].trim();
            String value = parts[1].trim();
            
            if (obfuscated) {
                // Simple obfuscation (Base64)
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
     * Replace variable placeholders
     */
    private String replacePlaceholders(String text) {
        // Replace plain variables
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            text = text.replace("{var(" + entry.getKey() + ")}", entry.getValue());
        }
        
        // Replace obfuscated variables
        for (Map.Entry<String, String> entry : obfuscatedVars.entrySet()) {
            try {
                String decoded = new String(Base64.getDecoder().decode(entry.getValue()));
                text = text.replace("{obfvar(" + entry.getKey() + ")}", decoded);
            } catch (Exception e) {
                // Ignore decode errors
            }
        }
        
        return text;
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
     * Process if statements
     */
    private void processIfStatement(String line, CommandSender sender) {
        // Basic if statement handling
        // Example: if var "name" = "value":
        if (line.contains(" = ")) {
            String[] parts = line.split(" = ");
            if (parts.length >= 2) {
                String varPart = parts[0].replace("if var", "").replace("if", "").trim();
                String valuePart = parts[1].replace(":", "").trim();
                
                // Remove quotes
                if (varPart.startsWith("\"") && varPart.endsWith("\"")) {
                    varPart = varPart.substring(1, varPart.length() - 1);
                }
                if (valuePart.startsWith("\"") && valuePart.endsWith("\"")) {
                    valuePart = valuePart.substring(1, valuePart.length() - 1);
                }
                
                String actualValue = variables.get(varPart);
                if (actualValue != null && actualValue.equals(valuePart)) {
                    api.log("[CodeDSL] If condition matched: " + varPart + " = " + valuePart);
                }
            }
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
        } catch (IOException e) {
            api.logError("Error Saving Obfuscated Variable: " + e.getMessage());
        }
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