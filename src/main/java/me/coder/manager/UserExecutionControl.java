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
            
            for (String line : lines) {
                // Check for Python imports
                if (line.trim().startsWith("import ") || line.trim().startsWith("from ")) {
                    for (String dangerous : DANGEROUS_IMPORTS) {
                        if (line.toLowerCase().contains(dangerous.toLowerCase())) {
                            if (!found.contains(dangerous)) {
                                found.add(dangerous);
                            }
                        }
                    }
                }
                
                // Check for Java imports
                if (line.trim().startsWith("import ")) {
                    for (String dangerous : DANGEROUS_IMPORTS) {
                        if (line.contains(dangerous)) {
                            if (!found.contains(dangerous)) {
                                found.add(dangerous);
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
}