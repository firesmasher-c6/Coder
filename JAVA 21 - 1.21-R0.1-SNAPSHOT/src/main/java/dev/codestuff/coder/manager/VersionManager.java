package dev.codestuff.coder.manager;

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
    private final String CURRENT_VERSION;
    private final String VERSION_URL = "https://codestuff.pages.dev/version/CoderVersion.txt";
    private String latestVersion;
    private String downloadLink;
    private Timer versionCheckTimer;
    
    public VersionManager(Plugin plugin) {
        this.plugin = plugin;
        this.CURRENT_VERSION = plugin.getDescription().getVersion();
        this.latestVersion = CURRENT_VERSION;
    }
    
    /**
     * Start the version check timer
     */
    public void start() {
        log("info", "Version checker starting. Current version: §f" + CURRENT_VERSION + "§7. Checks every §f12 hours§7.");
        startVersionCheck();
    }
    
    /**
     * Start periodic version checks (every 12 hours)
     */
    private void startVersionCheck() {
        versionCheckTimer = new Timer("Coder-VersionCheck", true);
        versionCheckTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkLatestVersion();
            }
        }, 0, TimeUnit.HOURS.toMillis(12));
    }
    
    /**
     * Check for latest version from remote
     */
    private void checkLatestVersion() {
        log("info", "§7Contacting update server: §f" + VERSION_URL);
        try {
            String response = fetchVersionInfo();
            if (response != null && !response.isEmpty()) {
                parseVersionInfo(response);
            } else {
                log("warn", "Update server returned an empty response. Skipping this check.");
            }
        } catch (Exception e) {
            log("warn", "Could not reach the update server. Check your internet connection.");
            log("warn", "§7Details: §c" + e.getClass().getSimpleName() + " §7— " + e.getMessage());
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
        log("info", "§7Update server responded with HTTP §f" + status);
        
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
        
        log("warn", "Update server returned unexpected HTTP status §c" + status + "§7. Cannot check for updates.");
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
            
            log("info", "§7Version check complete.");
            log("info", "§7  Running : §f" + CURRENT_VERSION);
            log("info", "§7  Latest  : §f" + latestVersion);

            if (isUpdateAvailable()) {
                log("warn", "§eA new version is available! §7(" + CURRENT_VERSION + " → " + latestVersion + ")");
                log("warn", "§7Run §f/coder update §7in-game to see details, or §f/coder update-jar §7to install.");
                if (downloadLink != null && !downloadLink.isEmpty()) {
                    log("info", "§7Download link: §f" + downloadLink);
                }
            } else {
                log("info", "§aCoder is up to date. §7(" + CURRENT_VERSION + ")");
            }

        } catch (Exception e) {
            log("warn", "Failed to parse the version info response.");
            log("warn", "§7Raw response was: §f" + response);
            log("warn", "§7Error: §c" + e.getClass().getSimpleName() + " §7— " + e.getMessage());
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
            log("warn", "Update-jar was requested but no download link was parsed from the version file.");
            return;
        }
        
        sender.sendMessage("§f[Coder] §eDownloading Coder §f" + latestVersion + "§e...");
        
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                downloadAndInstallUpdate(sender);
            } catch (Exception e) {
                log("severe", "Update process threw an unhandled exception: §c" + e.getClass().getSimpleName() + " §7— " + e.getMessage());
                sender.sendMessage("§f[Coder] §cUpdate failed unexpectedly. Check the server console for details.");
            }
        });
    }

    public void handleApiVersionCommand(CommandSender sender) {
        sender.sendMessage("§f[§bCoder§f] §7Checking API Version, Please wait..");

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                java.io.InputStream stream = plugin.getResource("version-api.yml");
                if (stream == null) {
                    sender.sendMessage("§f[§bCoder§f] §cCould not find version-api.yml in the plugin jar.");
                    return;
                }

                org.bukkit.configuration.file.YamlConfiguration apiYml =
                    org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                        new java.io.InputStreamReader(stream)
                    );

                String apiVersion  = apiYml.getString("api-version", "unknown");
                String apiPath     = apiYml.getString("api-path",     "dev.codestuff.coder.api");
                String pluginVer   = CURRENT_VERSION;

                sender.sendMessage("§f[§bCoder§f] §fYou're running Coder §bv" + pluginVer
                        + "§f, implementing api version §b'" + apiVersion + "'§f.");
                sender.sendMessage("§f[§bCoder§f] §fWith the API Path §b'" + apiPath + "'§f.");
            } catch (Exception e) {
                sender.sendMessage("§f[§bCoder§f] §cFailed to read API version info: " + e.getMessage());
                log("warn", "Failed to read version-api.yml: " + e.getMessage());
            }
        });
    }

    private void downloadAndInstallUpdate(CommandSender sender) throws IOException {
        File pluginsDir = new File("plugins");
        
        if (!isVersionGreater(latestVersion, CURRENT_VERSION)) {
            sender.sendMessage("§f[Coder] §cUpdate aborted: the version on the server is not newer than what you're running.");
            log("severe", "Update aborted. Version validation failed.");
            log("severe", "§7  Running : §f" + CURRENT_VERSION);
            log("severe", "§7  Web     : §c" + latestVersion + " §7(not greater — possible bad version file)");
            return;
        }
        
        log("info", "§7Starting download of Coder §f" + latestVersion + "§7 from: §f" + downloadLink);
        File tempJar = new File(pluginsDir, "Coder-temp.jar");

        sender.sendMessage("§f[Coder] §aDownloading Coder §f" + latestVersion + "§a...");

        String resolvedUrl = resolveRedirects(downloadLink);
        String resolvedFilename = extractFilenameFromUrl(resolvedUrl);
        log("info", "§7Resolved download URL : §f" + resolvedUrl);
        log("info", "§7Resolved filename     : §f" + resolvedFilename);

        downloadFile(resolvedUrl, tempJar);

        log("info", "§aDownload complete. §7Saved to: §f" + tempJar.getAbsolutePath());
        sender.sendMessage("§f[Coder] §aDownload complete!");
        sender.sendMessage("§f[Coder] §eVerifying downloaded JAR...");

        String downloadedVersion = getVersionFromJar(tempJar);
        if (downloadedVersion == null || downloadedVersion.isEmpty()) {
            sender.sendMessage("§f[Coder] §cFailed to read version from downloaded JAR. Aborting install.");
            log("severe", "Could not read plugin.yml version from the downloaded JAR: §f" + tempJar.getName());
            log("severe", "§7The file may be corrupted or not a valid Coder JAR. Deleting temp file.");
            tempJar.delete();
            return;
        }

        log("info", "§7JAR verified. Packaged version: §f" + downloadedVersion);
        sender.sendMessage("§f[Coder] §aJAR verified. Installing...");

        File finalJar = new File(pluginsDir,
                (resolvedFilename != null && resolvedFilename.endsWith(".jar"))
                        ? resolvedFilename
                        : "Coder-" + downloadedVersion + ".jar");
        
        File[] oldJars = pluginsDir.listFiles((dir, name) -> 
            name.startsWith("Coder-") && name.endsWith(".jar") && !name.equals("Coder-temp.jar")
        );
        
        if (oldJars != null && oldJars.length > 0) {
            log("info", "§7Removing §f" + oldJars.length + "§7 old Coder JAR(s)...");
            for (File oldJar : oldJars) {
                if (oldJar.delete()) {
                    log("info", "§7  Deleted: §f" + oldJar.getName());
                } else {
                    log("warn", "§7  Could not delete: §c" + oldJar.getName() + " §7(file may be locked)");
                }
            }
        }
        
        if (tempJar.renameTo(finalJar)) {
            log("info", "§aInstall successful. §7Coder §f" + downloadedVersion + " §7is ready at: §f" + finalJar.getName());
            sender.sendMessage("§f[Coder] §aSuccessfully installed Coder §f" + downloadedVersion);
            sender.sendMessage("§f[Coder] §cRestart the server to load the new version.");
        } else {
            log("severe", "Failed to rename temp JAR to final destination.");
            log("severe", "§7  From : §f" + tempJar.getAbsolutePath());
            log("severe", "§7  To   : §f" + finalJar.getAbsolutePath());
            log("severe", "§7Check that the server has write permissions in the plugins folder.");
            sender.sendMessage("§f[Coder] §cFailed to finalize the install. Check the server console for details.");
        }
    }
    
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
            } else {
                log("warn", "Downloaded JAR has no plugin.yml entry. Is this a valid plugin?");
            }
        } catch (IOException e) {
            log("warn", "IOException while reading plugin.yml from JAR: §c" + e.getMessage());
        }
        return null;
    }
    

    private boolean isVersionGreater(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        
        for (int i = 0; i < Math.max(parts1.length, parts2.length); i++) {
            int num1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int num2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            
            if (num1 > num2) return true;
            if (num1 < num2) return false;
        }
        
        return false;
    }

    @SuppressWarnings("deprecation")
    private String resolveRedirects(String urlString) throws IOException {
        String current = urlString;
        int hops = 0;
        while (hops++ < 10) {
            URL url = new URL(current);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setInstanceFollowRedirects(false);
            int status = conn.getResponseCode();
            conn.disconnect();
            if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
                String location = conn.getHeaderField("Location");
                if (location == null || location.isEmpty()) break;
                // Relative redirect -> make absolute
                if (location.startsWith("/")) {
                    URL base = new URL(current);
                    location = base.getProtocol() + "://" + base.getHost() + location;
                }
                log("info", "§7Redirect §f" + status + " §7→ §f" + location);
                current = location;
            } else {
                break;
            }
        }
        return current;
    }

    private String extractFilenameFromUrl(String urlString) {
        try {
            String path = new URL(urlString).getPath();
            int slash = path.lastIndexOf('/');
            String name = slash >= 0 ? path.substring(slash + 1) : path;
            // Strip any query string that may have ended up in the path segment
            int q = name.indexOf('?');
            if (q >= 0) name = name.substring(0, q);
            return name.isEmpty() ? null : name;
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    private void downloadFile(String urlString, File destFile) throws IOException {
        HttpURLConnection connection = null;
        InputStream input = null;
        FileOutputStream output = null;
        
        try {
            log("info", "§7Opening connection to: §f" + urlString);
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
                    log("info", "§7Following HTTP §f" + status + " §7redirect to: §f" + redirectLocation);
                    connection.disconnect();
                    downloadFile(redirectLocation, destFile);
                    return;
                }
            }
            
            if (status != 200) {
                throw new IOException("Download server returned HTTP " + status + " for URL: " + urlString);
            }
            
            input = connection.getInputStream();
            output = new FileOutputStream(destFile);
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            long totalBytes = 0;
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
            }
            log("info", "§7Downloaded §f" + (totalBytes / 1024) + " KB §7to §f" + destFile.getName());

        } finally {
            try {
                if (input != null) input.close();
                if (output != null) output.close();
            } catch (IOException e) {
                log("warn", "Failed to close download streams cleanly: §c" + e.getMessage());
            }
            if (connection != null) connection.disconnect();
        }
    }
    

    public void notifyOperatorIfUpdateAvailable(Player operator) {
        if (isUpdateAvailable()) {
            operator.sendMessage("§6§l=== Coder Update Available ===");
            operator.sendMessage("§6Current: §e" + CURRENT_VERSION);
            operator.sendMessage("§6Latest: §e" + latestVersion);
            operator.sendMessage("§6Run §f/coder update §6for details");
        }
    }

    public void stop() {
        if (versionCheckTimer != null) {
            versionCheckTimer.cancel();
            log("info", "§7Version check timer stopped.");
        }
    }


    private void log(String level, String message) {
        String clean = strip(message);
        switch (level) {
            case "info":
                plugin.getLogger().info(clean);
                break;
            case "warn":
                plugin.getLogger().warning(clean);
                break;
            case "severe":
                plugin.getLogger().severe(clean);
                break;
            default:
                plugin.getLogger().info(clean);
        }
    }

    private String strip(String s) {
        return s == null ? "" : s.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
    }
}