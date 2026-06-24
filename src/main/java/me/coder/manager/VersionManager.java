package me.coder.manager;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import org.bukkit.configuration.file.YamlConfiguration;

public class VersionManager {
    
    private final Plugin plugin;
    private final String CURRENT_VERSION = "2.1.3";
    private final String VERSION_URL = "https://codestuff.pages.dev/version/CoderVersion.txt";
    private String latestVersion;
    private String downloadLink;
    private Timer versionCheckTimer;
    
    public VersionManager(Plugin plugin) {
        this.plugin = plugin;
        this.latestVersion = CURRENT_VERSION;
    }
    
    /**
     * Start the version check timer
     */
    public void start() {
        startVersionCheck();
    }
    
    /**
     * Start periodic version checks (every 6 hours)
     */
    private void startVersionCheck() {
        versionCheckTimer = new Timer("Coder-VersionCheck", true);
        versionCheckTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkLatestVersion();
            }
        }, 0, TimeUnit.HOURS.toMillis(6));
    }
    
    /**
     * Check for latest version from remote
     */
    private void checkLatestVersion() {
        try {
            String response = fetchVersionInfo();
            if (response != null && !response.isEmpty()) {
                parseVersionInfo(response);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[Coder] Failed to check version: " + e.getMessage());
        }
    }
    
    /**
     * Fetch version info from remote URL
     */
    @SuppressWarnings("deprecation")
    private String fetchVersionInfo() throws IOException {
        URL url = new URL(VERSION_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.setInstanceFollowRedirects(true);
        
        int status = connection.getResponseCode();
        
        if (status == 200 || status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line).append("\n");
                }
                return response.toString().trim();
            }
        }
        
        return null;
    }
    
    /**
     * Parse version info from response
     * Format: VERSION | download-link: URL | modrinth: URL
     */
    private void parseVersionInfo(String response) {
        try {
            String[] parts = response.split("\\|");
            
            if (parts.length >= 1) {
                latestVersion = parts[0].trim();
            }
            
            for (String part : parts) {
                part = part.trim();
                if (part.startsWith("download-link:")) {
                    downloadLink = part.substring("download-link:".length()).trim();
                }
            }
            
            plugin.getLogger().info("[Coder] Version check complete. Latest: " + latestVersion);
        } catch (Exception e) {
            plugin.getLogger().warning("[Coder] Failed to parse version info: " + e.getMessage());
        }
    }
    
    /**
     * Get current version
     */
    public String getCurrentVersion() {
        return CURRENT_VERSION;
    }
    
    /**
     * Get latest version
     */
    public String getLatestVersion() {
        return latestVersion;
    }
    
    /**
     * Check if update is available
     */
    public boolean isUpdateAvailable() {
        return !CURRENT_VERSION.equals(latestVersion) && !latestVersion.isEmpty();
    }
    
    /**
     * Handle /coder update command
     */
    public void handleUpdateCommand(CommandSender sender) {
        sender.sendMessage("§f[Coder] §aCurrent version: §f" + CURRENT_VERSION);
        sender.sendMessage("§f[Coder] §aLatest version: §f" + latestVersion);
        
        if (isUpdateAvailable()) {
            sender.sendMessage("§f[Coder] §cA new version is available!");
            sender.sendMessage("§f[Coder] §eRun §f/coder update-jar §eto download and install the latest version.");
        } else {
            sender.sendMessage("§f[Coder] §aYou are running the latest version.");
        }
    }
    
    /**
     * Handle /coder update-jar command
     */
    public void handleUpdateJarCommand(CommandSender sender) {
        if (!isUpdateAvailable()) {
            sender.sendMessage("§f[Coder] §aYou are already running the latest version.");
            return;
        }
        
        if (downloadLink == null || downloadLink.isEmpty()) {
            sender.sendMessage("§f[Coder] §cFailed to get download link. Please check the update manually.");
            return;
        }
        
        sender.sendMessage("§f[Coder] §eDownloading Coder §f" + latestVersion + "§e...");
        
        // Run download in async task
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                downloadAndInstallUpdate(sender);
            } catch (Exception e) {
                plugin.getLogger().severe("[Coder] Update failed: " + e.getMessage());
                sender.sendMessage("§f[Coder] §cUpdate failed: " + e.getMessage());
            }
        });
    }
    
    /**
     * Download the JAR file following redirects
     */
    private void downloadAndInstallUpdate(CommandSender sender) throws IOException {
        File pluginsDir = new File("plugins");
        
        // Validate that latest version is greater than current
        if (!isVersionGreater(latestVersion, CURRENT_VERSION)) {
            sender.sendMessage("§f[CoderVersionManager] §cERROR: STATUS WEB-381");
            sender.sendMessage("§f[CoderVersionManager] §cERROR: RETURNED A LOWER NUMBER THAN " + CURRENT_VERSION);
            plugin.getLogger().severe("[Coder] Version validation failed: Web version " + latestVersion + " is not greater than " + CURRENT_VERSION);
            return;
        }
        
        File tempJar = new File(pluginsDir, "Coder-temp.jar");
        
        sender.sendMessage("§f[Coder] §aDownloading Coder §f" + latestVersion + "§a...");
        
        // Download to temp file
        downloadFile(downloadLink, tempJar);
        
        sender.sendMessage("§f[Coder] §aDownload complete!");
        sender.sendMessage("§f[Coder] §eInstalling new version...");
        
        // Get version from the downloaded jar's plugin.yml
        String downloadedVersion = getVersionFromJar(tempJar);
        if (downloadedVersion == null || downloadedVersion.isEmpty()) {
            sender.sendMessage("§f[Coder] §cFailed to read version from downloaded JAR");
            tempJar.delete();
            return;
        }
        
        // Final jar name based on version
        File finalJar = new File(pluginsDir, "Coder-" + downloadedVersion + ".jar");
        
        // Delete old versioned jars (Coder-*.jar)
        File[] oldJars = pluginsDir.listFiles((dir, name) -> 
            name.startsWith("Coder-") && name.endsWith(".jar") && !name.equals("Coder-temp.jar")
        );
        
        if (oldJars != null) {
            for (File oldJar : oldJars) {
                if (oldJar.delete()) {
                    plugin.getLogger().info("[Coder] Deleted old JAR: " + oldJar.getName());
                }
            }
        }
        
        // Rename temp to final name
        if (tempJar.renameTo(finalJar)) {
            sender.sendMessage("§f[Coder] §aSuccessfully installed Coder §f" + downloadedVersion);
            sender.sendMessage("§f[Coder] §cPlease restart the server to load the new version.");
        } else {
            sender.sendMessage("§f[Coder] §cFailed to install JAR. Please check server logs.");
            plugin.getLogger().severe("[Coder] Failed to rename downloaded JAR");
        }
    }
    
    /**
     * Get the version from a JAR's plugin.yml
     */
    private String getVersionFromJar(File jarFile) {
        try (java.util.jar.JarFile jar = new java.util.jar.JarFile(jarFile)) {
            java.util.jar.JarEntry entry = jar.getJarEntry("plugin.yml");
            if (entry != null) {
                try (java.io.InputStream input = jar.getInputStream(entry)) {
                    org.bukkit.configuration.file.YamlConfiguration config = 
                        org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                            new java.io.InputStreamReader(input)
                        );
                    return config.getString("version");
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("[Coder] Failed to read version from JAR: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Compare two version strings (e.g., "1.9.3" vs "1.3.2")
     * Returns true if v1 > v2
     */
    private boolean isVersionGreater(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        
        for (int i = 0; i < Math.max(parts1.length, parts2.length); i++) {
            int num1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int num2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            
            if (num1 > num2) return true;
            if (num1 < num2) return false;
        }
        
        return false; // versions are equal
    }
    
    /**
     * Download a file following HTTP redirects
     */
    @SuppressWarnings("deprecation")
    private void downloadFile(String urlString, File destFile) throws IOException {
        HttpURLConnection connection = null;
        InputStream input = null;
        FileOutputStream output = null;
        
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setInstanceFollowRedirects(true);
            
            int status = connection.getResponseCode();
            
            // Handle redirects manually for better control
            if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
                String redirectLocation = connection.getHeaderField("Location");
                if (redirectLocation != null) {
                    connection.disconnect();
                    downloadFile(redirectLocation, destFile);
                    return;
                }
            }
            
            if (status != 200) {
                throw new IOException("HTTP status: " + status);
            }
            
            input = connection.getInputStream();
            output = new FileOutputStream(destFile);
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
        } finally {
            try {
                if (input != null) input.close();
                if (output != null) output.close();
            } catch (IOException e) {
                plugin.getLogger().warning("[Coder] Error closing streams: " + e.getMessage());
            }
            if (connection != null) connection.disconnect();
        }
    }
    
    /**
     * Notify an operator if an update is available
     */
    public void notifyOperatorIfUpdateAvailable(Player operator) {
        if (isUpdateAvailable()) {
            operator.sendMessage("§6§l=== Coder Update Available ===");
            operator.sendMessage("§6Current: §e" + CURRENT_VERSION);
            operator.sendMessage("§6Latest: §e" + latestVersion);
            operator.sendMessage("§6Run §f/coder update §6for details");
        }
    }
    
    /**
     * Stop the version check timer
     */
    public void stop() {
        if (versionCheckTimer != null) {
            versionCheckTimer.cancel();
        }
    }
}
