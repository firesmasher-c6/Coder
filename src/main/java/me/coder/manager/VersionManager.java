package me.coder.manager;

import me.coder.CoderPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.util.concurrent.CompletableFuture;

/**
 * Manages version checking, updating, and config file monitoring
 */
public class VersionManager {
    
    private final CoderPlugin plugin;
    private final String VERSION_CHECK_URL = "https://codestuff.pages.dev/version/CoderVersion.txt";
    private final String DOWNLOAD_URL_TEMPLATE = "https://github.com/firesmasher-c6/Coder/releases/download/%s/Coder-%s.jar";
    
    private String currentVersion;
    private String latestVersion;
    private boolean updateAvailable = false;
    private boolean hasNotifiedOperators = false;
    private WatchService configWatcher;
    private Thread configWatcherThread;
    
    public VersionManager(CoderPlugin plugin) {
        this.plugin = plugin;
        this.currentVersion = plugin.getPluginMeta().getVersion();
    }
    
    /**
     * Start the version manager - checks for updates and watches config file
     */
    public void start() {
        // Check for updates on startup
        checkForUpdates();
        
        // Start watching config.yml for changes
        startConfigWatcher();
        
        plugin.getLogger().info("[VersionManager] Started - Current version: " + currentVersion);
    }
    
    /**
     * Stop the version manager and cleanup resources
     */
    public void stop() {
        stopConfigWatcher();
    }
    
    /**
     * Check for updates asynchronously
     */
    public void checkForUpdates() {
        CompletableFuture.runAsync(() -> {
            try {
                String remoteVersion = fetchRemoteVersion();
                if (remoteVersion != null && !remoteVersion.isEmpty()) {
                    this.latestVersion = remoteVersion.trim();
                    this.updateAvailable = isNewerVersion(currentVersion, latestVersion);
                    
                    if (updateAvailable) {
                        plugin.getLogger().info("[VersionManager] Update available! Latest: " + latestVersion + " | Current: " + currentVersion);
                        hasNotifiedOperators = false; // Reset notification flag so operators are notified
                    } else {
                        plugin.getLogger().info("[VersionManager] Plugin is up to date. Version: " + currentVersion);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[VersionManager] Failed to check for updates: " + e.getMessage());
            }
        });
    }
    
    /**
     * Fetch the remote version from the version check URL
     */
    private String fetchRemoteVersion() throws Exception {
        URI uri = new URI(VERSION_CHECK_URL);
        URL url = uri.toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestProperty("User-Agent", "CoderPlugin/1.0");
        
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                return reader.readLine();
            }
        } else {
            throw new Exception("HTTP " + responseCode);
        }
    }
    
    /**
     * Compare two version strings to determine if newVersion is newer
     * Supports formats like 1.4.2, 1.5.0, etc.
     */
    private boolean isNewerVersion(String currentVersion, String newVersion) {
        try {
            String[] currentParts = currentVersion.split("\\.");
            String[] newParts = newVersion.split("\\.");
            
            int maxLength = Math.max(currentParts.length, newParts.length);
            
            for (int i = 0; i < maxLength; i++) {
                int current = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
                int newVer = i < newParts.length ? Integer.parseInt(newParts[i]) : 0;
                
                if (newVer > current) {
                    return true;
                } else if (newVer < current) {
                    return false;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get the download link for the latest version
     */
    public String getDownloadLink() {
        if (latestVersion == null || latestVersion.isEmpty()) {
            return null;
        }
        return String.format(DOWNLOAD_URL_TEMPLATE, "v" + latestVersion, latestVersion);
    }
    
    /**
     * Start watching the config.yml file for changes
     */
    private void startConfigWatcher() {
        try {
            Path configPath = plugin.getDataFolder().toPath();
            configWatcher = FileSystems.getDefault().newWatchService();
            configPath.register(configWatcher, StandardWatchEventKinds.ENTRY_MODIFY);
            
            configWatcherThread = new Thread(() -> {
                try {
                    while (true) {
                        WatchKey key = configWatcher.take();
                        
                        for (WatchEvent<?> event : key.pollEvents()) {
                            Path changedFile = (Path) event.context();
                            if (changedFile.toString().equals("config.yml")) {
                                plugin.getLogger().info("[VersionManager] config.yml has been modified, reloading...");
                                // Add a small delay to ensure file write is complete
                                Thread.sleep(500);
                                plugin.reloadConfig();
                                plugin.getLogger().info("[VersionManager] config.yml reloaded successfully");
                            }
                        }
                        
                        if (!key.reset()) {
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    plugin.getLogger().info("[VersionManager] Config watcher stopped");
                }
            });
            
            configWatcherThread.setDaemon(true);
            configWatcherThread.setName("Coder-ConfigWatcher");
            configWatcherThread.start();
            
            plugin.getLogger().info("[VersionManager] Config file watcher started");
        } catch (Exception e) {
            plugin.getLogger().warning("[VersionManager] Failed to start config watcher: " + e.getMessage());
        }
    }
    
    /**
     * Stop watching the config.yml file
     */
    private void stopConfigWatcher() {
        if (configWatcherThread != null) {
            configWatcherThread.interrupt();
        }
        if (configWatcher != null) {
            try {
                configWatcher.close();
            } catch (Exception e) {
                plugin.getLogger().warning("[VersionManager] Error closing config watcher: " + e.getMessage());
            }
        }
    }
    
    /**
     * Check if update is available and notify an operator
     */
    public void notifyOperatorIfUpdateAvailable(org.bukkit.entity.Player operator) {
        if (updateAvailable && !hasNotifiedOperators) {
            operator.sendMessage("§6§l[Coder]§r §6An update is available!");
            operator.sendMessage("§6Current Version: §e" + currentVersion);
            operator.sendMessage("§6Latest Version: §e" + latestVersion);
            operator.sendMessage("§6Use §e/coder update §6to get the download link");
            hasNotifiedOperators = true;
        }
    }
    
    // Getters
    
    public String getCurrentVersion() {
        return currentVersion;
    }
    
    public String getLatestVersion() {
        return latestVersion;
    }
    
    public boolean isUpdateAvailable() {
        return updateAvailable;
    }
    
    public String getVersionCheckUrl() {
        return VERSION_CHECK_URL;
    }
}