package me.coder.manager;

import org.bukkit.command.CommandSender;
import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class UserExecutionControl {
    
    private static final Map<CommandSender, PendingScript> pendingScripts = new HashMap<>();
    
    // Dangerous imports that require confirmation
    private static final Set<String> DANGEROUS_IMPORTS = new HashSet<>(Arrays.asList(
        "subprocess",
        "os",
        "sys",
        "runtime",
        "process",
        "exec",
        "Runtime",
        "ProcessBuilder",
        "reflection",
        "java.lang.reflect",
        "java.io.File",
        "java.nio.file",
        "RandomAccessFile"
    ));
    
    // Terminal/shell paths that are blocked
    private static final Set<String> BLOCKED_TERMINALS = new HashSet<>(Arrays.asList(
        "/bin/sh",
        "/bin/bash",
        "/bin/zsh",
        "/bin/fish",
        "/usr/bin/bash",
        "/usr/bin/sh",
        "cmd.exe",
        "powershell",
        "pwsh"
    ));

    /**
     * Check if a script file contains terminal/shell commands
     * @param file The script file to check
     * @return true if terminal commands found
     */
    public static boolean hasTerminalCommands(File file) {
        try {
            List<String> lines = Files.readAllLines(file.toPath());
            
            for (String line : lines) {
                String lowerLine = line.toLowerCase();
                
                // Check for blocked terminal paths
                for (String terminal : BLOCKED_TERMINALS) {
                    if (lowerLine.contains(terminal.toLowerCase())) {
                        return true;
                    }
                }
                
                // Check for shell execution patterns
                if (lowerLine.contains("subprocess.") || 
                    lowerLine.contains("os.system") ||
                    lowerLine.contains("os.popen") ||
                    lowerLine.contains("ProcessBuilder") ||
                    lowerLine.contains("Runtime.getRuntime().exec")) {
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return false;
    }

    /**
     * Check if a script file contains dangerous imports
     * @param file The script file to check
     * @return List of dangerous imports found, or empty list if none
     */
    public static List<String> checkDangerousImports(File file) {
        List<String> found = new ArrayList<>();
        
        try {
            List<String> lines = Files.readAllLines(file.toPath());
            
            for (int lineNum = 0; lineNum < lines.size(); lineNum++) {
                String line = lines.get(lineNum);
                
                // Check for Python imports
                if (line.trim().startsWith("import ") || line.trim().startsWith("from ")) {
                    for (String dangerous : DANGEROUS_IMPORTS) {
                        if (line.toLowerCase().contains(dangerous.toLowerCase())) {
                            if (!found.contains(dangerous)) {
                                found.add(dangerous + " (line " + (lineNum + 1) + ")");
                            }
                        }
                    }
                }
                
                // Check for Java imports
                if (line.trim().startsWith("import ")) {
                    for (String dangerous : DANGEROUS_IMPORTS) {
                        if (line.contains(dangerous)) {
                            if (!found.contains(dangerous)) {
                                found.add(dangerous + " (line " + (lineNum + 1) + ")");
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return found;
    }

    /**
     * Scan script line by line and check what it does (strict UEC)
     * @param file The script file to scan
     * @return ScriptAnalysis result with findings
     */
    public static ScriptAnalysis scanScriptLineLine(File file) {
        ScriptAnalysis analysis = new ScriptAnalysis();
        
        try {
            List<String> lines = Files.readAllLines(file.toPath());
            
            for (int lineNum = 0; lineNum < lines.size(); lineNum++) {
                String line = lines.get(lineNum).trim();
                
                // Skip comments and empty lines
                if (line.isEmpty() || line.startsWith("//") || line.startsWith("#") || line.startsWith("*")) {
                    continue;
                }
                
                // Analyze what the line does
                analyzeLineAction(line, lineNum + 1, analysis);
            }
        } catch (IOException e) {
            analysis.addError("Failed to read script: " + e.getMessage());
        }
        
        return analysis;
    }

    /**
     * Analyze what a single line of code does
     */
    private static void analyzeLineAction(String line, int lineNum, ScriptAnalysis analysis) {
        // File operations
        if (line.contains("File") && (line.contains("delete") || line.contains("write"))) {
            analysis.addAction("Line " + lineNum + ": File operation detected - " + line);
        }
        
        // Network operations
        if (line.contains("Socket") || line.contains("ServerSocket") || line.contains("URL")) {
            analysis.addAction("Line " + lineNum + ": Network operation detected - " + line);
        }
        
        // Process execution
        if (line.contains("Runtime") || line.contains("ProcessBuilder")) {
            analysis.addAction("Line " + lineNum + ": Process execution detected - " + line);
        }
        
        // System operations
        if (line.contains("System.exit") || line.contains("System.setProperty")) {
            analysis.addAction("Line " + lineNum + ": System operation detected - " + line);
        }
        
        // Reflection (can bypass security)
        if (line.contains("getDeclaredMethod") || line.contains("getMethod") || line.contains("setAccessible")) {
            analysis.addAction("Line " + lineNum + ": Reflection usage detected - " + line);
        }
        
        // String concatenation bypass attempt
        if (line.contains("\"") && line.contains("+")) {
            // Check for common bypass patterns
            if (line.toLowerCase().contains("tem") || line.toLowerCase().contains("process") || 
                line.toLowerCase().contains("runtime")) {
                analysis.addWarning("Line " + lineNum + ": Potential concatenation bypass - " + line);
            }
        }
    }

    /**
     * Add a pending script for confirmation
     * @param sender The user executing the script
     * @param fileName The script filename
     * @param dangerousImports List of dangerous imports found
     */
    public static void addPendingScript(CommandSender sender, String fileName, List<String> dangerousImports) {
        pendingScripts.put(sender, new PendingScript(fileName, dangerousImports));
    }

    /**
     * Get a pending script for a sender
     * @param sender The user
     * @return The pending script or null
     */
    public static PendingScript getPendingScript(CommandSender sender) {
        return pendingScripts.get(sender);
    }

    /**
     * Remove a pending script
     * @param sender The user
     */
    public static void removePendingScript(CommandSender sender) {
        pendingScripts.remove(sender);
    }

    /**
     * Send the execution control warning
     * @param sender The user
     * @param dangerousImports List of dangerous imports
     */
    public static void sendExecutionWarning(CommandSender sender, List<String> dangerousImports) {
        sender.sendMessage("§c=============================");
        sender.sendMessage("§c=== USER EXECUTION CONTROL ===");
        sender.sendMessage("§c[!] This Script Has System Imports:");
        
        for (String imp : dangerousImports) {
            sender.sendMessage("§6    - " + imp);
        }
        
        sender.sendMessage("§e");
        sender.sendMessage("§eTo continue running this script please do:");
        sender.sendMessage("§a/coder confirm");
        sender.sendMessage("§e");
        sender.sendMessage("§eIf you wish to not run this script please do:");
        sender.sendMessage("§c/coder cancel");
        sender.sendMessage("§c=============================");
    }

    /**
     * Inner class representing a pending script
     */
    public static class PendingScript {
        public String fileName;
        public List<String> dangerousImports;
        public long createdAt;

        public PendingScript(String fileName, List<String> dangerousImports) {
            this.fileName = fileName;
            this.dangerousImports = dangerousImports;
            this.createdAt = System.currentTimeMillis();
        }

        /**
         * Check if this pending script has expired (older than 5 minutes)
         * @return true if expired
         */
        public boolean isExpired() {
            return System.currentTimeMillis() - createdAt > 300000; // 5 minutes
        }
    }

    /**
     * Inner class for detailed script analysis results
     */
    public static class ScriptAnalysis {
        private List<String> actions;
        private List<String> warnings;
        private List<String> errors;

        public ScriptAnalysis() {
            this.actions = new ArrayList<>();
            this.warnings = new ArrayList<>();
            this.errors = new ArrayList<>();
        }

        public void addAction(String action) {
            actions.add(action);
        }

        public void addWarning(String warning) {
            warnings.add(warning);
        }

        public void addError(String error) {
            errors.add(error);
        }

        public List<String> getActions() {
            return actions;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public List<String> getErrors() {
            return errors;
        }

        public boolean isSafe() {
            return errors.isEmpty() && warnings.isEmpty();
        }

        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            if (!actions.isEmpty()) {
                sb.append("Actions: ").append(actions.size()).append("\n");
            }
            if (!warnings.isEmpty()) {
                sb.append("Warnings: ").append(warnings.size()).append("\n");
            }
            if (!errors.isEmpty()) {
                sb.append("Errors: ").append(errors.size()).append("\n");
            }
            return sb.toString();
        }
    }
}