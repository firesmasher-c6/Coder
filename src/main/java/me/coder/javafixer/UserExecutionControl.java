package me.coder.javafixer;

import org.bukkit.command.CommandSender;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class UserExecutionControl {

    // Block malicious terminal environments and dangerous process components
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
     * Inspects the target file. Returns true if the file is safe, 
     * or false if it violates security policies.
     */
    public static boolean isExecutionSafe(File javaFile, CommandSender sender) {
        // 1. Automatically trust the server console/terminal operator
        if (sender.getName().equalsIgnoreCase("CONSOLE")) {
            return true; 
        }

        // 2. Bypass safety check ONLY for you (Change to your actual Minecraft username)
        if (sender.getName().equals("YourMinecraftName")) {
            return true;
        }

        // 3. Read and scan the source file content for regular players/staff
        try {
            String content = Files.readString(javaFile.toPath());
            
            for (String keyword : BLACKLISTED_KEYWORDS) {
                if (content.contains(keyword)) {
                    notifyViolation(sender, keyword);
                    return false;
                }
            }
        } catch (IOException e) {
            sender.sendMessage("§c[Security] Failed to run safety scan on file. Execution aborted.");
            return false;
        }

        return true;
    }

    private static void notifyViolation(CommandSender sender, String violation) {
        sender.sendMessage("§c❌ [Security Alert] Compilation blocked!");
        sender.sendMessage("§4║ §cReason: Your script contains unauthorized system handles.");
        sender.sendMessage("§4║ §eBlocked element: §f" + violation);
    }
}