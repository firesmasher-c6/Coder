package me.coder.manager;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.GZIPOutputStream;

public class ActionLoggerManager {
    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private boolean enabled;
    private boolean compressLogs;
    private final File logsDirectory;

    public ActionLoggerManager(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.logsDirectory = new File(plugin.getDataFolder(), "Logs/ActionLogs");
        
        loadConfig();
        ensureLogsDirectory();
    }

    private void loadConfig() {
        this.enabled = plugin.getConfig().getBoolean("actions-manager.enabled", true);
        this.compressLogs = plugin.getConfig().getBoolean("actions-manager.compress-logs", true);
    }

    private void ensureLogsDirectory() {
        if (!logsDirectory.exists()) {
            logsDirectory.mkdirs();
        }
    }

    /**
     * Log an action performed by a player
     */
    public void logAction(Player player, String action) {
        if (!enabled) {
            return;
        }

        try {
            String logEntry = formatLogEntry(player, action);
            String logFileName = getLogFileName();
            File logFile = new File(logsDirectory, logFileName);

            // Append to existing log file
            try (FileWriter fw = new FileWriter(logFile, true);
                 BufferedWriter bw = new BufferedWriter(fw)) {
                bw.write(logEntry);
                bw.newLine();
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to log action: " + e.getMessage());
        }
    }

    /**
     * Log a command execution
     */
    public void logCommandExecution(Player player, String command, boolean success) {
        if (!enabled) {
            return;
        }

        String action = (success ? "[SUCCESS]" : "[FAILED]") + " Command: " + command;
        logAction(player, action);
    }

    /**
     * Log script execution
     */
    public void logScriptExecution(Player player, String scriptName, String scriptType) {
        if (!enabled) {
            return;
        }

        String action = "[SCRIPT] Type: " + scriptType + " | Script: " + scriptName;
        logAction(player, action);
    }

    /**
     * Format a single log entry with timestamp and player info
     */
    private String formatLogEntry(Player player, String action) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String timestamp = now.format(formatter);
        return String.format("[%s] Player: %s (UUID: %s) | %s", timestamp, player.getName(), player.getUniqueId(), action);
    }

    /**
     * Get the log file name based on current date
     */
    private String getLogFileName() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return "actions-" + now.format(formatter) + ".log";
    }

    /**
     * Compress a log file to .log.gz
     */
    public void compressLogFile(File logFile) {
        if (!logFile.exists() || !compressLogs) {
            return;
        }

        try {
            File compressedFile = new File(logFile.getAbsolutePath() + ".gz");

            try (FileInputStream fis = new FileInputStream(logFile);
                 GZIPOutputStream gzos = new GZIPOutputStream(new FileOutputStream(compressedFile))) {

                byte[] buffer = new byte[1024];
                int len;
                while ((len = fis.read(buffer)) != -1) {
                    gzos.write(buffer, 0, len);
                }
            }

            // Delete original file after compression
            if (logFile.delete()) {
                plugin.getLogger().info("Log file compressed and archived: " + compressedFile.getName());
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to compress log file: " + e.getMessage());
        }
    }

    /**
     * Enable activity logging
     */
    public void enableLogging() {
        this.enabled = true;
        plugin.getConfig().set("actions-manager.enabled", true);
        plugin.saveConfig();
    }

    /**
     * Disable activity logging
     */
    public void disableLogging() {
        this.enabled = false;
        plugin.getConfig().set("actions-manager.enabled", false);
        plugin.saveConfig();
    }

    /**
     * Check if logging is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Reload configuration
     */
    public void reloadConfig() {
        plugin.reloadConfig();
        loadConfig();
    }
}