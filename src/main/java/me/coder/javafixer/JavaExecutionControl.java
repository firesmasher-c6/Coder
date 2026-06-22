package me.coder.javafixer;

import org.bukkit.command.CommandSender;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class JavaExecutionControl {

    private static final List<String> BLACKLISTED_KEYWORDS = List.of(
        "Runtime.getRuntime().exec",
        "ProcessBuilder",
        "java.lang.Process",
        "/bin/sh",
        "/bin/bash",
        "cmd.exe",
        "powershell",
        "wmic",
        "dmidecode"
    );

    /**
     * Checks if a Java file is safe to execute
     * Returns true if safe, false if it violates security policies
     */
    public static boolean isExecutionSafe(File javaFile, CommandSender sender) {
        // Console always trusted
        if (sender.getName().equalsIgnoreCase("CONSOLE")) {
            return true; 
        }

        // OP always trusted
        if (sender.isOp()) {
            return true;
        }

        // Scan for blacklisted content
        try {
            String content = Files.readString(javaFile.toPath());
            
            for (String keyword : BLACKLISTED_KEYWORDS) {
                if (content.contains(keyword)) {
                    notifyViolation(sender, keyword);
                    return false;
                }
            }
        } catch (IOException e) {
            sender.sendMessage("§c[JavaFixer] Failed to scan file. Execution blocked.");
            return false;
        }

        return true;
    }

    private static void notifyViolation(CommandSender sender, String violation) {
        sender.sendMessage("§c❌ [JavaFixer Security] Execution blocked!");
        sender.sendMessage("§4║ §cReason: Script contains unauthorized system access.");
        sender.sendMessage("§4║ §eBlocked: §f" + violation);
    }
}