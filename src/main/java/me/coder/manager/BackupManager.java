package me.coder.manager;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BackupManager {
    
    private final Plugin plugin;
    private final File backupFolder;
    private final File logsFolder;
    private final ConfigManager configManager;
    private BukkitTask autoBackupTask;
    
    public BackupManager(Plugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.backupFolder = new File(plugin.getDataFolder().getParent(), "Coder/backups");
        this.logsFolder = new File(plugin.getDataFolder(), "Logs/Backups");
        
        if (!backupFolder.exists()) {
            backupFolder.mkdirs();
        }
        if (!logsFolder.exists()) {
            logsFolder.mkdirs();
        }
    }
    
    // Start manual backup
    public void startBackup(CommandSender executor) {
        if (!configManager.isCommandEnabled("backup")) {
            executor.sendMessage("§c[Coder] The backup command is disabled in config.yml");
            return;
        }
        
        executor.sendMessage("§d[Coder] Starting backup...");
        long startTime = System.currentTimeMillis();
        
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                File backupFile = createBackupFile();
                
                executor.sendMessage("§a[Coder] Creating backup: " + backupFile.getName());
                
                createZipBackup(backupFile, executor);
                
                long duration = System.currentTimeMillis() - startTime;
                long fileSize = backupFile.length();
                executor.sendMessage("§a[Coder] ✓ Backup complete!");
                executor.sendMessage("§a[Coder] Time: " + duration + "ms");
                executor.sendMessage("§a[Coder] Size: " + formatFileSize(fileSize));
                executor.sendMessage("§a[Coder] Location: backups/" + backupFile.getName());
                plugin.getLogger().info("Backup created: " + backupFile.getName() + " (" + formatFileSize(fileSize) + ") in " + duration + "ms");
                
                logBackup(backupFile.getName(), fileSize, duration, "MANUAL");

                
            } catch (Exception e) {
                executor.sendMessage("§c[Coder] Backup failed: " + e.getMessage());
                plugin.getLogger().severe("Backup error: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    // Start automatic backup task
    public void startAutoBackup(CommandSender executor) {
        if (!configManager.isCommandEnabled("auto-backup-start")) {
            executor.sendMessage("§c[Coder] The auto-backup-start command is disabled in config.yml");
            return;
        }
        
        if (autoBackupTask != null) {
            executor.sendMessage("§c[Coder] Auto-backup is already running!");
            return;
        }
        
        String scheduleStr = configManager.getBackupSchedule();
        long intervalTicks = parseScheduleToTicks(scheduleStr);
        
        if (intervalTicks == -1) {
            executor.sendMessage("§c[Coder] Invalid backup schedule: " + scheduleStr);
            executor.sendMessage("§c[Coder] Format: '1h', '30m', '1d', etc.");
            return;
        }
        
        autoBackupTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(
            plugin,
            this::performAutoBackup,
            intervalTicks,
            intervalTicks
        );
        
        executor.sendMessage("§a[Coder] ✓ Auto-backup started!");
        executor.sendMessage("§a[Coder] Schedule: Every " + scheduleStr);
        plugin.getLogger().info("Auto-backup started with schedule: " + scheduleStr);
    }
    
    // Stop automatic backup
    public void stopAutoBackup(CommandSender executor) {
        if (!configManager.isCommandEnabled("auto-backup-stop")) {
            executor.sendMessage("§c[Coder] The auto-backup-stop command is disabled in config.yml");
            return;
        }
        
        if (autoBackupTask == null) {
            executor.sendMessage("§c[Coder] Auto-backup is not running!");
            return;
        }
        
        autoBackupTask.cancel();
        autoBackupTask = null;
        
        executor.sendMessage("§a[Coder] ✓ Auto-backup stopped!");
        plugin.getLogger().info("Auto-backup stopped");
    }
    
    // Perform auto backup (silent)
    private void performAutoBackup() {
        try {
            long startTime = System.currentTimeMillis();
            File backupFile = createBackupFile();
            
            createZipBackup(backupFile, null);
            
            long duration = System.currentTimeMillis() - startTime;
            plugin.getLogger().info("Auto-backup completed: " + backupFile.getName() + " in " + duration + "ms");
            
            long fileSize = backupFile.length();
            logBackup(backupFile.getName(), fileSize, duration, "AUTO");
        } catch (Exception e) {
            plugin.getLogger().severe("Auto-backup failed: " + e.getMessage());
        }
    }
    
    // Create backup file with timestamp
    private File createBackupFile() {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        return new File(backupFolder, "coder-backup_" + timestamp + ".zip");
    }
    
    // Create ZIP backup
    private void createZipBackup(File backupFile, CommandSender executor) throws IOException {
        File coderFolder = plugin.getDataFolder();
        
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(backupFile))) {
            Files.walkFileTree(coderFolder.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path relative = coderFolder.toPath().relativize(file);
                    ZipEntry entry = new ZipEntry(relative.toString().replace("\\", "/"));
                    zos.putNextEntry(entry);
                    
                    Files.copy(file, zos);
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
    
    // Parse schedule string to ticks (1 tick = 50ms = 1/20 second)
    private long parseScheduleToTicks(String schedule) {
        try {
            String[] parts = schedule.toLowerCase().replaceAll("\\s+", "").split("(?<=\\d)(?=[a-z])");
            
            if (parts.length != 2) {
                return -1;
            }
            
            int value = Integer.parseInt(parts[0]);
            String unit = parts[1];
            
            if (value < 1 || value > 60) {
                return -1;
            }
            
            long ticks = 0;
            switch (unit) {
                case "m":
                    ticks = (long) value * 60 * 20; // minutes to ticks
                    break;
                case "h":
                    ticks = (long) value * 60 * 60 * 20; // hours to ticks
                    break;
                case "d":
                    ticks = (long) value * 24 * 60 * 60 * 20; // days to ticks
                    break;
                default:
                    return -1;
            }
            
            return ticks;
        } catch (Exception e) {
            return -1;
        }
    }
    
    // Format file size to readable string
    private String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return String.format("%.2f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }
    
    // Check if auto-backup should run on plugin start
    public void checkStartupBackup() {
        if (configManager.shouldBackupOnStart()) {
            plugin.getLogger().info("Creating startup backup...");
            try {
                long startTime = System.currentTimeMillis();
                File backupFile = createBackupFile();
                
                createZipBackup(backupFile, null);
                
                long duration = System.currentTimeMillis() - startTime;
                long fileSize = backupFile.length();
                plugin.getLogger().info("Startup backup created: " + backupFile.getName() + " in " + duration + "ms");
                
                logBackup(backupFile.getName(), fileSize, duration, "STARTUP");
            } catch (Exception e) {
                plugin.getLogger().severe("Startup backup failed: " + e.getMessage());
            }
        }
    }
    
    // Stop auto-backup on plugin disable
    public void stopOnDisable() {
        if (configManager.shouldCancelOnDisable() && autoBackupTask != null) {
            autoBackupTask.cancel();
            autoBackupTask = null;
            plugin.getLogger().info("Auto-backup stopped (plugin disable)");
        }
    }
    
    // Get backup folder
    public File getBackupFolder() {
        return backupFolder;
    }
    
    // List all backups
    public void listBackups(CommandSender executor) {
        File[] backups = backupFolder.listFiles();
        
        if (backups == null || backups.length == 0) {
            executor.sendMessage("§c[Coder] No backups found");
            return;
        }
        
        executor.sendMessage("§a[Coder] ========== Backups ==========");
        for (File backup : backups) {
            executor.sendMessage("§a  • " + backup.getName() + " (" + formatFileSize(backup.length()) + ")");
        }
        executor.sendMessage("§a[Coder] ================================");
    }
    
    // Log backup to file
    private void logBackup(String fileName, long fileSize, long duration, String type) {
        try {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            String logFileName = new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ".log";
            File logFile = new File(logsFolder, logFileName);
            
            try (FileWriter fw = new FileWriter(logFile, true);
                 PrintWriter pw = new PrintWriter(fw)) {
                
                pw.println("[" + timestamp + "] [" + type + "] " + fileName + " | Size: " + formatFileSize(fileSize) + " | Time: " + duration + "ms");
                pw.flush();
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to write backup log: " + e.getMessage());
        }
    }
}