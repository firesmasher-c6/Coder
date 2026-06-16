package me.coder.codedsl;

import me.coder.api.CoderAPI;
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
        for (String line : lines) {
            line = line.trim();
            
            // Skip empty lines and comments
            if (line.isEmpty() || line.startsWith("#")) {
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
                String message = extractString(line.substring(12));
                api.log(replacePlaceholders(message));
            }

            // Handle variable assignment
            else if (line.startsWith("var ")) {
                parseVariable(line.substring(4), false);
            }

            // Handle obfuscated variable assignment
            else if (line.startsWith("obfuscatedVAR ")) {
                parseVariable(line.substring(14), true);
            }

            // Handle variable read
            else if (line.startsWith("varRead ")) {
                String varName = line.substring(8).trim();
                if (variables.containsKey(varName)) {
                    api.log("[CodeDSL] Variable '" + varName + "': " + variables.get(varName));
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

            // Handle if statements
            else if (line.startsWith("if ")) {
                processIfStatement(line, sender);
            }
        }
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
            } else {
                variables.put(name, value);
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
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            text = text.replace("{" + entry.getKey() + "}", entry.getValue());
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
        // Example: if arg-1 is "text":
        if (line.contains(" is ")) {
            String[] parts = line.substring(3).split(" is ");
            if (parts.length == 2) {
                String condition = parts[0].trim();
                String value = extractString(parts[1]);
                
                // Simple condition evaluation
                if (condition.equals(value)) {
                    api.log("[CodeDSL] If condition matched");
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
                    if (line.contains("=")) {
                        String[] parts = line.split("=", 2);
                        if (parts.length == 2) {
                            variables.put(parts[0].trim(), parts[1].trim());
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
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                sb.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
            }
            Files.write(varFile.toPath(), sb.toString().getBytes());
        } catch (Exception e) {
            api.logError("Error saving variables: " + e.getMessage());
        }
    }
}