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