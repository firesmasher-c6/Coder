package me.coder.codedsl.manager;

import me.coder.api.CoderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionManager implements Listener {
    
    private static final String VERSION_URL = "https://codestuff.pages.dev/version/CodeDSLVersion.txt";
    private static final Pattern VERSION_PATTERN = Pattern.compile("^([\\d.]+)\\s*\\|\\s*(.+)$");
    
    private CoderAPI api;
    private String currentVersion;
    private String latestVersion;
    private String downloadLink;
    private String spigotmcLink;
    private String directDownloadLink;
    private boolean updateAvailable = false;
    private boolean checkInProgress = false;
    private String pendingUpdateVersion = null; // Track pending update for confirmation
    
    public VersionManager(CoderAPI api, String currentVersion) {
        this.api = api;
        this.currentVersion = currentVersion;
    }
    
    /**
     * Check for updates asynchronously
     */
    public void checkForUpdates() {
        // Prevent multiple concurrent version checks
        if (checkInProgress) {
            return;
        }
        
        checkInProgress = true;
        
        try {
            org.bukkit.plugin.Plugin plugin = Bukkit.getPluginManager().getPlugin("CodeDSL");
            if (plugin != null) {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, this::fetchLatestVersion);
            } else {
                log("CodeDSL plugin not found, cannot check for updates");
                checkInProgress = false;
            }
        } catch (Exception e) {
            logError("Failed to schedule update check: " + e.getMessage());
            checkInProgress = false;
        }
    }
    
    /**
     * Fetch latest version from remote server using modern Java HttpClient
     * FIXED: Uses Java 11+ HttpClient instead of deprecated URLConnection
     */
    private void fetchLatestVersion() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(5))
                    .build();
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(VERSION_URL))
                    .timeout(java.time.Duration.ofSeconds(5))
                    .header("User-Agent", "CodeDSL-VersionCheck/1.0")
                    .GET()
                    .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                String body = response.body();
                String[] lines = body.split("\n");
                
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    
                    Matcher matcher = VERSION_PATTERN.matcher(line);
                    if (matcher.find()) {
                        this.latestVersion = matcher.group(1);
                        String linksSection = matcher.group(2);
                        
                        // Parse spigotmc and direct-download links
                        parseDownloadLinks(linksSection);
                        
                        if (isNewerVersion(latestVersion, currentVersion)) {
                            this.updateAvailable = true;
                            logClean("CodeDSL Update available: " + latestVersion + " (current: " + currentVersion + ")");
                        } else {
                            logClean("CodeDSL is up to date (" + currentVersion + ")");
                        }
                        break;
                    }
                }
            } else {
                logError("Failed to fetch version info: HTTP " + response.statusCode());
            }
        } catch (java.net.ConnectException e) {
            logError("Failed to connect to version server: " + e.getMessage());
        } catch (java.net.http.HttpTimeoutException e) {
            logError("Version check timed out: " + e.getMessage());
        } catch (InterruptedException e) {
            logError("Version check was interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logError("Failed to check for updates: " + e.getMessage());
        } finally {
            checkInProgress = false;
        }
    }

    /**
     * Parse download links from version info
     * Format: spigotmc: URL | direct-download: URL
     */
    private void parseDownloadLinks(String linksSection) {
        String[] parts = linksSection.split("\\|");
        for (String part : parts) {
            part = part.trim();
            if (part.startsWith("spigotmc:")) {
                this.spigotmcLink = part.substring(9).trim();
            } else if (part.startsWith("direct-download:")) {
                this.directDownloadLink = part.substring(16).trim();
            }
        }
        this.downloadLink = this.spigotmcLink; // Default to spigotmc link for display
    }
    
    /**
     * Compare version strings (e.g., "1.9.5" vs "1.9.6")
     * Returns true if newVersion is newer than currentVersion
     */
    private boolean isNewerVersion(String newVersion, String currentVersion) {
        try {
            if (newVersion == null || currentVersion == null) {
                return false;
            }
            
            String[] newParts = newVersion.split("\\.");
            String[] currentParts = currentVersion.split("\\.");
            
            int maxLength = Math.max(newParts.length, currentParts.length);
            
            for (int i = 0; i < maxLength; i++) {
                int newPart = 0;
                int currentPart = 0;
                
                try {
                    if (i < newParts.length) {
                        newPart = Integer.parseInt(newParts[i]);
                    }
                } catch (NumberFormatException e) {
                    logError("Failed to parse version number: " + newParts[i]);
                }
                
                try {
                    if (i < currentParts.length) {
                        currentPart = Integer.parseInt(currentParts[i]);
                    }
                } catch (NumberFormatException e) {
                    logError("Failed to parse version number: " + currentParts[i]);
                }
                
                if (newPart > currentPart) return true;
                if (newPart < currentPart) return false;
            }
            
            return false;
        } catch (Exception e) {
            logError("Error comparing versions: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Alert operator when joining if update is available
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        if (player == null || !player.isOp()) {
            return;
        }
        
        if (updateAvailable && latestVersion != null && downloadLink != null) {
            org.bukkit.plugin.Plugin plugin = Bukkit.getPluginManager().getPlugin("CodeDSL");
            if (plugin != null) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        player.sendMessage("§a━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                        player.sendMessage("§a CodeDSL Update Available");
                        player.sendMessage("§a━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                        player.sendMessage("§f Current: §7" + currentVersion);
                        player.sendMessage("§f Latest:  §a" + latestVersion);
                        player.sendMessage("§f Download: §b" + downloadLink);
                        player.sendMessage("§f Command: §e/codedsl update");
                        player.sendMessage("§a━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    }
                }, 20L);  // 1 second delay
            }
        }
    }
    
    /**
     * Handle /codedsl update command - checks for updates and prompts for download
     */
    public void handleUpdateCommand(Player player) {
        if (updateAvailable && latestVersion != null) {
            player.sendMessage("§a━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            player.sendMessage("§a CodeDSL Update Available");
            player.sendMessage("§a━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            player.sendMessage("§f Current: §7" + currentVersion);
            player.sendMessage("§f Latest:  §a" + latestVersion);
            player.sendMessage("§f Download: §b" + downloadLink);
            player.sendMessage("");
            player.sendMessage("§eClick the link above or run: §f/codedsl confirm");
            player.sendMessage("§ato automatically download and update!");
            player.sendMessage("§a━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            
            // Store pending update
            this.pendingUpdateVersion = latestVersion;
        } else if (!updateAvailable && latestVersion != null) {
            player.sendMessage("§aCodeDSL is up to date (v" + currentVersion + ")");
        } else {
            player.sendMessage("§cCould not check for updates. Try again later.");
        }
    }

    /**
     * Handle /codedsl update command for console (sets pending update)
     */
    public void handleUpdateCommandConsole() {
        if (updateAvailable && latestVersion != null) {
            // Store pending update for console
            this.pendingUpdateVersion = latestVersion;
        }
    }

    /**
     * Handle /codedsl confirm command - downloads and installs the update
     */
    public void handleConfirmUpdate(Player player) {
        if (pendingUpdateVersion == null) {
            player.sendMessage("§cNo pending update. Run §f/codedsl update §cfirst.");
            return;
        }

        if (directDownloadLink == null || directDownloadLink.isEmpty()) {
            player.sendMessage("§cDirect download link not available.");
            return;
        }

        player.sendMessage("§eDownloading CodeDSL v" + latestVersion + "...");
        
        // Download in async task
        Bukkit.getScheduler().runTaskAsynchronously(
            Bukkit.getPluginManager().getPlugin("CodeDSL"),
            () -> downloadAndUpdatePlugin(player)
        );
    }

    /**
     * Handle /codedsl confirm command for console
     */
    public void handleConfirmUpdateConsole() {
        if (pendingUpdateVersion == null) {
            Bukkit.getLogger().warning("[CodeDSL] No pending update. Run /codedsl update first.");
            return;
        }

        if (directDownloadLink == null || directDownloadLink.isEmpty()) {
            Bukkit.getLogger().severe("[CodeDSL] Direct download link not available.");
            return;
        }

        Bukkit.getLogger().info("[CodeDSL] Downloading CodeDSL v" + latestVersion + "...");
        
        // Download in async task
        Bukkit.getScheduler().runTaskAsynchronously(
            Bukkit.getPluginManager().getPlugin("CodeDSL"),
            () -> downloadAndUpdatePluginConsole()
        );
    }

    /**
     * Download plugin JAR from direct download link and update
     */
    private void downloadAndUpdatePlugin(Player player) {
        try {
            // Get plugins folder
            File pluginsFolder = Bukkit.getPluginManager().getPlugin("CodeDSL").getDataFolder().getParentFile();
            File downloadedFile = new File(pluginsFolder, "CodeDSL-" + latestVersion + ".jar");

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(30))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(directDownloadLink))
                    .timeout(java.time.Duration.ofSeconds(30))
                    .header("User-Agent", "CodeDSL-UpdateDownload/1.0")
                    .GET()
                    .build();

            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() == 200) {
                // Write downloaded file
                try (InputStream input = response.body();
                     FileOutputStream output = new FileOutputStream(downloadedFile)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = input.read(buffer)) != -1) {
                        output.write(buffer, 0, bytesRead);
                    }
                }

                // Find and delete old CodeDSL JAR
                findAndDeleteOldJar(pluginsFolder);

                // Notify player
                Bukkit.getScheduler().runTask(
                    Bukkit.getPluginManager().getPlugin("CodeDSL"),
                    () -> {
                        player.sendMessage("§a━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                        player.sendMessage("§a CodeDSL Update Downloaded!");
                        player.sendMessage("§a━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                        player.sendMessage("§aVersion: §f" + latestVersion);
                        player.sendMessage("§aFile: §f" + downloadedFile.getName());
                        player.sendMessage("§e⚠ Please run §f/reload §eto apply the update!");
                        player.sendMessage("§a━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                        logClean("Downloaded CodeDSL v" + latestVersion);
                    }
                );

                pendingUpdateVersion = null;
            } else {
                player.sendMessage("§cFailed to download update: HTTP " + response.statusCode());
                logError("Download failed with status: " + response.statusCode());
            }
        } catch (Exception e) {
            player.sendMessage("§cError downloading update: " + e.getMessage());
            logError("Download error: " + e.getMessage());
        }
    }

    /**
     * Download plugin JAR from direct download link and update (Console version)
     */
    private void downloadAndUpdatePluginConsole() {
        try {
            // Get plugins folder
            File pluginsFolder = Bukkit.getPluginManager().getPlugin("CodeDSL").getDataFolder().getParentFile();
            File downloadedFile = new File(pluginsFolder, "CodeDSL-" + latestVersion + ".jar");

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(30))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(directDownloadLink))
                    .timeout(java.time.Duration.ofSeconds(30))
                    .header("User-Agent", "CodeDSL-UpdateDownload/1.0")
                    .GET()
                    .build();

            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() == 200) {
                // Write downloaded file
                try (InputStream input = response.body();
                     FileOutputStream output = new FileOutputStream(downloadedFile)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = input.read(buffer)) != -1) {
                        output.write(buffer, 0, bytesRead);
                    }
                }

                // Find and delete old CodeDSL JAR
                findAndDeleteOldJar(pluginsFolder);

                // Notify console
                Bukkit.getScheduler().runTask(
                    Bukkit.getPluginManager().getPlugin("CodeDSL"),
                    () -> {
                        Bukkit.getLogger().info("━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                        Bukkit.getLogger().info("CodeDSL Update Downloaded!");
                        Bukkit.getLogger().info("━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                        Bukkit.getLogger().info("Version: " + latestVersion);
                        Bukkit.getLogger().info("File: " + downloadedFile.getName());
                        Bukkit.getLogger().info("⚠ Please run /reload to apply the update!");
                        Bukkit.getLogger().info("━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                        logClean("Downloaded CodeDSL v" + latestVersion);
                    }
                );

                pendingUpdateVersion = null;
            } else {
                Bukkit.getLogger().severe("[CodeDSL] Failed to download update: HTTP " + response.statusCode());
                logError("Download failed with status: " + response.statusCode());
            }
        } catch (Exception e) {
            Bukkit.getLogger().severe("[CodeDSL] Error downloading update: " + e.getMessage());
            logError("Download error: " + e.getMessage());
        }
    }
    private void findAndDeleteOldJar(File pluginsFolder) {
        if (!pluginsFolder.exists() || !pluginsFolder.isDirectory()) {
            return;
        }

        File[] files = pluginsFolder.listFiles();
        if (files != null) {
            for (File file : files) {
                String name = file.getName().toLowerCase();
                if ((name.startsWith("codedsl") || name.startsWith("code-dsl") || name.startsWith("coder")) 
                    && name.endsWith(".jar") 
                    && !name.contains(latestVersion)) {
                    if (file.delete()) {
                        logClean("Deleted old version: " + file.getName());
                    }
                }
            }
        }
    }

    /**
     * Log message without [Coder] prefix (uses Bukkit logger directly)
     * Use this instead of api.log for cleaner console output
     */
    private void logClean(String message) {
        Bukkit.getLogger().info("[CodeDSL] " + message);
    }

    /**
     * Log info message
     */
    private void log(String message) {
        if (api != null) {
            api.log("[CodeDSL] " + message);
        }
    }
    
    /**
     * Log error message
     */
    private void logError(String message) {
        if (api != null) {
            api.logError("[CodeDSL] " + message);
        }
    }
    
    // ============ Getters ============
    
    public String getCurrentVersion() {
        return currentVersion;
    }
    
    public String getLatestVersion() {
        return latestVersion;
    }
    
    public String getDownloadLink() {
        return downloadLink;
    }

    public String getSpigotmcLink() {
        return spigotmcLink;
    }

    public String getDirectDownloadLink() {
        return directDownloadLink;
    }

    public String getPendingUpdateVersion() {
        return pendingUpdateVersion;
    }
    
    public boolean isUpdateAvailable() {
        return updateAvailable;
    }
    
    public boolean isCheckInProgress() {
        return checkInProgress;
    }
    
    // ============ Setters ============
    
    public void setCurrentVersion(String version) {
        if (version != null && !version.isEmpty()) {
            this.currentVersion = version;
        }
    }
    
    /**
     * Manual update available setter (for testing)
     */
    public void setUpdateAvailable(boolean available) {
        this.updateAvailable = available;
    }
    
    /**
     * Manual version setter (for testing)
     */
    public void setLatestVersion(String version) {
        this.latestVersion = version;
    }
    
    /**
     * Manual download link setter (for testing)
     */
    public void setDownloadLink(String link) {
        this.downloadLink = link;
    }

    /**
     * Manual spigotmc link setter (for testing)
     */
    public void setSpigotmcLink(String link) {
        this.spigotmcLink = link;
    }

    /**
     * Manual direct download link setter (for testing)
     */
    public void setDirectDownloadLink(String link) {
        this.directDownloadLink = link;
    }

    /**
     * Manual pending update setter (for testing)
     */
    public void setPendingUpdateVersion(String version) {
        this.pendingUpdateVersion = version;
    }
}