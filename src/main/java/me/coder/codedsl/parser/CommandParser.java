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