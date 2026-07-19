package me.coder.manager;

import me.coder.CoderPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.LogRecord;
import java.util.logging.Handler;

public class EditorManager {

    private static final String WORKER_URL = "https://coder-gwieditor.firesmasher.workers.dev";

    private final CoderPlugin plugin;
    private final HttpClient http = HttpClient.newHttpClient();

    // token -> session
    private final Map<String, EditorSession> sessions = new ConcurrentHashMap<>();
    // playerName (lowercase) -> token  (one session per player)
    private final Map<String, String> playerTokens = new ConcurrentHashMap<>();

    private BukkitTask pollTask;

    // ─── Console log forwarding ───────────────────────────────────────────────

    /** Bukkit log handler that captures server log lines and forwards them to the worker */
    private ConsoleLogHandler consoleLogHandler;

    /** Whether /plugins/Coder/.gwi/secure/ConsoleLogSender.yml allows player logs */
    private boolean sendPlayerLogs = false;

    public EditorManager(CoderPlugin plugin) {
        this.plugin = plugin;
        loadConsoleLogConfig();
        installConsoleLogHandler();
        startPolling();
    }

    // ─── Commands ─────────────────────────────────────────────────────────────

    public void startEditor(CommandSender sender) {
        String playerName = sender.getName();

        if (playerTokens.containsKey(playerName.toLowerCase())) {
            sender.sendMessage("§e[Coder] You already have an editor session open. Run §f/coder editor stop §eto close it first.");
            return;
        }

        String token = generateToken(playerName);
        EditorSession session = new EditorSession(token, playerName);
        sessions.put(token, session);
        playerTokens.put(playerName.toLowerCase(), token);

        List<Map<String, String>> files = buildFileList();
        String payload = buildStartPayload(token, playerName, files);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(WORKER_URL + "/api/session/start"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload))
                        .build();

                HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());

                if (res.statusCode() == 200) {
                    String link = WORKER_URL + "/editor?session=" + token;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        sender.sendMessage("§a[Coder] Editor started!");
                        sender.sendMessage("§7Open this link in your browser:");
                        sender.sendMessage("§f" + link);
                        sender.sendMessage("§7When someone connects, you'll be asked to trust them.");
                    });
                } else {
                    cleanup(playerName);
                    Bukkit.getScheduler().runTask(plugin, () ->
                            sender.sendMessage("§c[Coder] Worker rejected session start: " + res.statusCode()));
                }
            } catch (Exception e) {
                cleanup(playerName);
                Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage("§c[Coder] Failed to contact worker: " + e.getMessage()));
            }
        });
    }

    public void stopEditor(CommandSender sender) {
        String playerName = sender.getName();
        String token = playerTokens.get(playerName.toLowerCase());

        if (token == null) {
            sender.sendMessage("§c[Coder] You don't have an active editor session.");
            return;
        }

        invalidateSession(token, playerName);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String payload = "{\"token\":\"" + token + "\"}";
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(WORKER_URL + "/api/session/stop"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload))
                        .build();
                http.send(req, HttpResponse.BodyHandlers.ofString());
            } catch (Exception ignored) {}
        });

        sender.sendMessage("§a[Coder] Editor session closed.");
    }

    public void trustUser(CommandSender sender, String browserUser) {
        String playerName = sender.getName();
        String token = playerTokens.get(playerName.toLowerCase());

        if (token == null) {
            sender.sendMessage("§c[Coder] You don't have an active editor session.");
            return;
        }

        EditorSession session = sessions.get(token);
        if (session == null) return;

        if (!browserUser.equalsIgnoreCase(session.browserUser)) {
            sender.sendMessage("§c[Coder] No pending trust request from §f" + browserUser + "§c.");
            return;
        }

        session.status = EditorSession.EditorStatus.TRUSTED;
        pushAuthStatus(token, "trusted");
        sender.sendMessage("§a[Coder] Trusted §f" + browserUser + "§a. Editor is now unlocked.");
    }

    public void doNotTrustUser(CommandSender sender, String browserUser) {
        String playerName = sender.getName();
        String token = playerTokens.get(playerName.toLowerCase());

        if (token == null) {
            sender.sendMessage("§c[Coder] You don't have an active editor session.");
            return;
        }

        EditorSession session = sessions.get(token);
        if (session == null) return;

        if (!browserUser.equalsIgnoreCase(session.browserUser)) {
            sender.sendMessage("§c[Coder] No pending trust request from §f" + browserUser + "§c.");
            return;
        }

        session.status = EditorSession.EditorStatus.REJECTED;
        pushAuthStatus(token, "rejected");
        sender.sendMessage("§c[Coder] Rejected §f" + browserUser + "§c. Their tab will be closed.");
    }

    /**
     * /coder gen-pass
     * Generates a random SHA-256 server password and saves it to
     * /plugins/Coder/.gwi/secure/serverPassword.env
     * Prints the hash to the sender so they can paste it into the MC Console auth form.
     */
    public void generateServerPassword(CommandSender sender) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // Generate 32 random bytes → hex string → SHA-256 hash of that
                byte[] randomBytes = new byte[32];
                new SecureRandom().nextBytes(randomBytes);
                StringBuilder rawHex = new StringBuilder();
                for (byte b : randomBytes) rawHex.append(String.format("%02x", b));

                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] hashBytes = md.digest(rawHex.toString().getBytes(StandardCharsets.UTF_8));
                StringBuilder hash = new StringBuilder();
                for (byte b : hashBytes) hash.append(String.format("%02x", b));

                String serverPasswordHash = hash.toString();

                // Save to /plugins/Coder/.gwi/secure/serverPassword.env
                File secureDir = new File(plugin.getDataFolder(), ".gwi/secure");
                secureDir.mkdirs();
                File envFile = new File(secureDir, "serverPassword.env");
                String envContent = "serverPassword=" + serverPasswordHash + "\n";
                Files.writeString(envFile.toPath(), envContent, StandardCharsets.UTF_8);

                plugin.getLogger().info("Server password regenerated and saved to .gwi/secure/serverPassword.env");

                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage("§a[Coder] Server password generated!");
                    sender.sendMessage("§7Copy this hash into the MC Console auth form:");
                    sender.sendMessage("§f" + serverPasswordHash);
                    sender.sendMessage("§7Saved to: §fplugins/Coder/.gwi/secure/serverPassword.env");
                });
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to generate server password: " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage("§c[Coder] Failed to generate password: " + e.getMessage()));
            }
        });
    }

    // ─── Polling ──────────────────────────────────────────────────────────────

    private void startPolling() {
        pollTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (sessions.isEmpty()) return;

            for (Map.Entry<String, EditorSession> entry : sessions.entrySet()) {
                String token = entry.getKey();
                EditorSession session = entry.getValue();

                try {
                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(URI.create(WORKER_URL + "/api/poll?token=" + token))
                            .GET()
                            .build();

                    HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
                    if (res.statusCode() != 200) continue;

                    String body = res.body();
                    String action = extractJson(body, "action");
                    if (action == null) continue;

                    switch (action) {

                        case "auth_request": {
                            String name = extractJson(body, "browserUser");
                            if (name != null && !name.equals(session.browserUser)) {
                                session.browserUser = name;
                                session.status = EditorSession.EditorStatus.PENDING_TRUST;
                                Bukkit.getScheduler().runTask(plugin, () -> {
                                    CommandSender player = Bukkit.getPlayerExact(session.playerName);
                                    if (player == null) player = Bukkit.getConsoleSender();
                                    player.sendMessage("§e[Coder] §f" + name + " §ewants to access the editor.");
                                    player.sendMessage("§7Run §f/coder editor trust " + name + " §7to allow or §f/coder editor do-not-trust " + name + " §7to reject.");
                                });
                            }
                            break;
                        }

                        case "save_file": {
                            String fileName = extractJson(body, "fileName");
                            if (fileName != null && isAllowedFile(fileName)) {
                                String fileContent = fetchFileContent(token, fileName);
                                if (fileContent != null) {
                                    saveFile(fileName, fileContent);
                                    plugin.getLogger().info("" + session.browserUser + " saved file: " + fileName);
                                    Bukkit.getScheduler().runTask(plugin, () -> {
                                        CommandSender player = Bukkit.getPlayerExact(session.playerName);
                                        if (player != null)
                                            player.sendMessage("§a[Coder] §f" + session.browserUser + " §asaved §f" + fileName);
                                    });
                                }
                            }
                            break;
                        }

                        case "create_file": {
                            String fileName = extractJson(body, "fileName");
                            if (fileName != null && isAllowedFile(fileName)) {
                                createFile(fileName);
                                plugin.getLogger().info("" + session.browserUser + " created file: " + fileName);
                                Bukkit.getScheduler().runTask(plugin, () -> {
                                    CommandSender player = Bukkit.getPlayerExact(session.playerName);
                                    if (player != null)
                                        player.sendMessage("§a[Coder] §f" + session.browserUser + " §acreated §f" + fileName);
                                });
                            }
                            break;
                        }

                        case "create_folder": {
                            String folderName = extractJson(body, "folderName");
                            if (folderName != null && isAllowedFile(folderName)) {
                                createFolder(folderName);
                                plugin.getLogger().info("" + session.browserUser + " created folder: " + folderName);
                                Bukkit.getScheduler().runTask(plugin, () -> {
                                    CommandSender player = Bukkit.getPlayerExact(session.playerName);
                                    if (player != null)
                                        player.sendMessage("§a[Coder] §f" + session.browserUser + " §acreated folder §f" + folderName);
                                });
                            }
                            break;
                        }

                        case "rename": {
                            String oldName = extractJson(body, "oldName");
                            String newName = extractJson(body, "newName");
                            if (oldName != null && newName != null && isAllowedFile(oldName) && isAllowedFile(newName)) {
                                renameFileOrFolder(oldName, newName);
                                plugin.getLogger().info("" + session.browserUser + " renamed: " + oldName + " → " + newName);
                                Bukkit.getScheduler().runTask(plugin, () -> {
                                    CommandSender player = Bukkit.getPlayerExact(session.playerName);
                                    if (player != null)
                                        player.sendMessage("§a[Coder] §f" + session.browserUser + " §arenamed §f" + oldName + " §a→ §f" + newName);
                                });
                            }
                            break;
                        }

                        case "delete": {
                            String fileName = extractJson(body, "fileName");
                            if (fileName != null && isAllowedFile(fileName)) {
                                deleteFileOrFolder(fileName);
                                plugin.getLogger().info("" + session.browserUser + " deleted: " + fileName);
                                Bukkit.getScheduler().runTask(plugin, () -> {
                                    CommandSender player = Bukkit.getPlayerExact(session.playerName);
                                    if (player != null)
                                        player.sendMessage("§c[Coder] §f" + session.browserUser + " §cdeleted §f" + fileName);
                                });
                            }
                            break;
                        }

                        case "session_closed": {
                            String pName = session.playerName;
                            plugin.getLogger().info("Browser tab closed for session owned by " + pName);
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                CommandSender player = Bukkit.getPlayerExact(pName);
                                if (player != null)
                                    player.sendMessage("§e[Coder] Browser tab was closed. Session still active — run §f/coder editor stop §eto end it.");
                            });
                            break;
                        }

                        // ── Execute: browser requested running a file ──────────────────────
                        case "run_file": {
                            String fileName = extractJson(body, "fileName");
                            String runtime  = extractJson(body, "runtime");
                            if (fileName != null && isAllowedFile(fileName)) {
                                plugin.getLogger().info("Execute request: " + fileName + " (runtime: " + runtime + ") by " + session.browserUser);
                                final String tok = token;
                                final EditorSession sess = session;
                                Bukkit.getScheduler().runTask(plugin, () -> {
                                    CommandSender player = Bukkit.getPlayerExact(sess.playerName);
                                    if (player != null)
                                        player.sendMessage("§e[Coder] §f" + sess.browserUser + " §eis executing §f" + fileName + " §eon §f" + runtime);
                                    // Dispatch /coder run <fileName> on the main thread, then push result back
                                    plugin.getServer().dispatchCommand(Bukkit.getConsoleSender(), "coder run " + fileName.replaceFirst("^scripts/", ""));
                                });
                                // Push a log line back so the console viewer shows the execution was triggered
                                pushConsoleLogs(token, Collections.singletonList(
                                    "{\"level\":\"INFO\",\"text\":\"[Coder] Executing " + escJson(fileName) + " (" + escJson(runtime) + ")...\",\"ts\":" + System.currentTimeMillis() + "}"
                                ));
                            }
                            break;
                        }

                        // ── MC Console: browser submitted passwords for verification ───────
                        case "console_auth_request": {
                            String editorPassword = extractJson(body, "editorPassword");
                            String serverPassword  = extractJson(body, "serverPassword");
                            plugin.getLogger().info("MC Console auth request received for session " + token.substring(0, 8) + "...");
                            final String tok = token;
                            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                                boolean ok = verifyServerPassword(serverPassword);
                                if (ok) {
                                    plugin.getLogger().info("MC Console auth APPROVED for session " + tok.substring(0, 8) + "...");
                                    pushConsoleAuthResult(tok, "trusted");
                                } else {
                                    plugin.getLogger().warning("MC Console auth REJECTED — bad server password for session " + tok.substring(0, 8) + "...");
                                    pushConsoleAuthResult(tok, "rejected");
                                }
                            });
                            break;
                        }

                        // ── MC Console: browser sent a command to run ──────────────────────
                        case "console_command": {
                            String command = extractJson(body, "command");
                            if (command != null && !command.isBlank()) {
                                // Strip leading slash, re-add for dispatch
                                String cmd = command.trim().replaceFirst("^/+", "");
                                plugin.getLogger().info("MC Console command dispatched: /" + cmd + " (session " + token.substring(0, 8) + "...)");
                                final String tok = token;
                                Bukkit.getScheduler().runTask(plugin, () -> {
                                    plugin.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd);
                                    // The command output will appear naturally in server logs and get forwarded
                                    // via ConsoleLogHandler → pushConsoleLogs
                                });
                            }
                            break;
                        }
                    }
                } catch (Exception ignored) {}
            }
        }, 60L, 60L); // 60 ticks = 3 seconds
    }

    // ─── File helpers ─────────────────────────────────────────────────────────

    private List<Map<String, String>> buildFileList() {
        List<Map<String, String>> files = new ArrayList<>();
        File coderDir = plugin.getDataFolder();
        collectFiles(coderDir, coderDir, files);
        return files;
    }

    private void collectFiles(File root, File dir, List<Map<String, String>> out) {
        File[] children = dir.listFiles();
        if (children == null) return;

        for (File f : children) {
            if (f.isDirectory()) {
                String name = f.getName();
                // Skip backups, compiled classes, and the secure folder
                if (name.equals("backups") || name.equals("JavaClasses") || name.equals(".gwi")) continue;
                collectFiles(root, f, out);
            } else {
                try {
                    String rel = root.toPath().relativize(f.toPath()).toString().replace("\\", "/");
                    String content = Files.readString(f.toPath(), StandardCharsets.UTF_8);
                    Map<String, String> entry = new HashMap<>();
                    entry.put("name", rel);
                    entry.put("content", content);
                    out.add(entry);
                } catch (Exception ignored) {}
            }
        }
    }

    private boolean isAllowedFile(String fileName) {
        return !fileName.contains("..") && !fileName.startsWith("/") && !fileName.startsWith(".gwi");
    }

    private void saveFile(String fileName, String content) {
        try {
            File target = new File(plugin.getDataFolder(), fileName);
            if (!target.getCanonicalPath().startsWith(plugin.getDataFolder().getCanonicalPath())) return;
            target.getParentFile().mkdirs();
            Files.writeString(target.toPath(), content, StandardCharsets.UTF_8);
        } catch (Exception e) {
            plugin.getLogger().warning("Editor failed to save file: " + fileName + " - " + e.getMessage());
        }
    }

    private void createFile(String fileName) {
        try {
            File target = new File(plugin.getDataFolder(), fileName);
            if (!target.getCanonicalPath().startsWith(plugin.getDataFolder().getCanonicalPath())) return;
            if (target.exists()) return;
            target.getParentFile().mkdirs();
            target.createNewFile();
        } catch (Exception e) {
            plugin.getLogger().warning("Editor failed to create file: " + fileName + " - " + e.getMessage());
        }
    }

    private void createFolder(String folderName) {
        try {
            File target = new File(plugin.getDataFolder(), folderName);
            if (!target.getCanonicalPath().startsWith(plugin.getDataFolder().getCanonicalPath())) return;
            target.mkdirs();
        } catch (Exception e) {
            plugin.getLogger().warning("Editor failed to create folder: " + folderName + " - " + e.getMessage());
        }
    }

    private void renameFileOrFolder(String oldName, String newName) {
        try {
            File source = new File(plugin.getDataFolder(), oldName);
            File dest   = new File(plugin.getDataFolder(), newName);
            if (!source.getCanonicalPath().startsWith(plugin.getDataFolder().getCanonicalPath())) return;
            if (!dest.getCanonicalPath().startsWith(plugin.getDataFolder().getCanonicalPath()))   return;
            if (!source.exists()) return;
            dest.getParentFile().mkdirs();
            source.renameTo(dest);
        } catch (Exception e) {
            plugin.getLogger().warning("Editor failed to rename: " + oldName + " → " + newName + " - " + e.getMessage());
        }
    }

    private void deleteFileOrFolder(String fileName) {
        try {
            File target = new File(plugin.getDataFolder(), fileName);
            if (!target.getCanonicalPath().startsWith(plugin.getDataFolder().getCanonicalPath())) return;
            deleteRecursively(target);
        } catch (Exception e) {
            plugin.getLogger().warning("Editor failed to delete: " + fileName + " - " + e.getMessage());
        }
    }

    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) deleteRecursively(child);
        }
        file.delete();
    }

    // ─── Worker communication ─────────────────────────────────────────────────

    private void pushAuthStatus(String token, String status) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String payload = "{\"token\":\"" + token + "\",\"status\":\"" + status + "\"}";
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(WORKER_URL + "/api/auth/respond"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload))
                        .build();
                http.send(req, HttpResponse.BodyHandlers.ofString());
            } catch (Exception ignored) {}
        });
    }

    /**
     * Pushes the result of MC Console auth to the worker.
     * Sets both authStatus and consoleStatus on the Firestore session doc
     * so the browser's poll of /api/auth/status gets data.consoleStatus.
     */
    private void pushConsoleAuthResult(String token, String result) {
        try {
            // "trusted" → consoleStatus = "trusted"
            // "rejected" → consoleStatus = "rejected"
            String payload = "{\"token\":\"" + token
                    + "\",\"status\":\"" + result
                    + "\",\"consoleStatus\":\"" + result + "\"}";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(WORKER_URL + "/api/auth/respond"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            http.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to push console auth result: " + e.getMessage());
        }
    }

    /**
     * Pushes a batch of pre-serialised log JSON objects to the worker's
     * /api/mc-console/logs endpoint (POST).
     * Each entry is already a JSON string like {"level":"INFO","text":"...","ts":12345}.
     */
    private void pushConsoleLogs(String token, List<String> logJsonEntries) {
        if (logJsonEntries.isEmpty()) return;
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"token\":\"").append(token).append("\",\"logs\":[");
            for (int i = 0; i < logJsonEntries.size(); i++) {
                sb.append(logJsonEntries.get(i));
                if (i < logJsonEntries.size() - 1) sb.append(",");
            }
            sb.append("]}");
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(WORKER_URL + "/api/mc-console/logs"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(sb.toString()))
                    .build();
            http.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to push console logs: " + e.getMessage());
        }
    }

    // ─── Server password ──────────────────────────────────────────────────────

    /**
     * Reads the stored serverPassword hash from .gwi/secure/serverPassword.env
     * and compares it against the hash the browser sent.
     */
    private boolean verifyServerPassword(String submittedHash) {
        if (submittedHash == null || submittedHash.isBlank()) return false;
        try {
            File envFile = new File(plugin.getDataFolder(), ".gwi/secure/serverPassword.env");
            if (!envFile.exists()) {
                plugin.getLogger().warning("serverPassword.env not found. Run /coder gen-pass first.");
                return false;
            }
            String contents = Files.readString(envFile.toPath(), StandardCharsets.UTF_8);
            for (String line : contents.split("\n")) {
                line = line.trim();
                if (line.startsWith("serverPassword=")) {
                    String storedHash = line.substring("serverPassword=".length()).trim();
                    return storedHash.equalsIgnoreCase(submittedHash.trim());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to read serverPassword.env: " + e.getMessage());
        }
        return false;
    }

    // ─── Console log forwarding ───────────────────────────────────────────────

    /**
     * Reads /plugins/Coder/.gwi/secure/ConsoleLogSender.yml.
     * Creates it with defaults if missing.
     */
    private void loadConsoleLogConfig() {
        try {
            File secureDir = new File(plugin.getDataFolder(), ".gwi/secure");
            secureDir.mkdirs();
            File configFile = new File(secureDir, "ConsoleLogSender.yml");

            if (!configFile.exists()) {
                String defaultContent =
                    "# Coder \"Secure\" Configuration File\n" +
                    "# Used On GWI Editor\n\n" +
                    "security:\n" +
                    "  # Keep At False Recommended\n" +
                    "  send-player-logs: false\n\n" +
                    "# security.send-player-logs sets if the plugin will send player logs or not.\n" +
                    "# player join logs contains their Public IP!\n";
                Files.writeString(configFile.toPath(), defaultContent, StandardCharsets.UTF_8);
                plugin.getLogger().info("Created ConsoleLogSender.yml with secure defaults.");
            }

            // Simple parse — no full YAML lib needed for one boolean
            String contents = Files.readString(configFile.toPath(), StandardCharsets.UTF_8);
            for (String line : contents.split("\n")) {
                line = line.trim();
                if (line.startsWith("send-player-logs:")) {
                    String val = line.substring("send-player-logs:".length()).trim();
                    sendPlayerLogs = val.equalsIgnoreCase("true");
                }
            }
            plugin.getLogger().info("ConsoleLogSender: send-player-logs=" + sendPlayerLogs);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load ConsoleLogSender.yml: " + e.getMessage());
        }
    }

    /**
     * Attaches a custom java.util.logging.Handler to Bukkit's root logger
     * so we can intercept every server log line and forward it to the worker
     * for any active sessions that have console access trusted.
     */
    private void installConsoleLogHandler() {
        consoleLogHandler = new ConsoleLogHandler();
        Bukkit.getServer().getLogger().addHandler(consoleLogHandler);
        plugin.getLogger().info("Console log handler installed.");
    }

    private void removeConsoleLogHandler() {
        if (consoleLogHandler != null) {
            Bukkit.getServer().getLogger().removeHandler(consoleLogHandler);
            consoleLogHandler = null;
        }
    }

    /**
     * A java.util.logging.Handler that intercepts server log records
     * and queues them for forwarding to the worker.
     */
    private class ConsoleLogHandler extends Handler {

        // Player-join detection patterns (rough, covers Bukkit/Paper/Spigot formats)
        private static final String[] PLAYER_LOG_KEYWORDS = {
            "logged in with entity id",
            "left the game",
            "lost connection",
            "UUID of player",
            "GameProfile",
        };

        @Override
        public void publish(LogRecord record) {
            if (record == null) return;
            String message = record.getMessage();
            if (message == null || message.isBlank()) return;

            // Filter player logs if configured to do so
            if (!sendPlayerLogs) {
                String msgLower = message.toLowerCase();
                for (String keyword : PLAYER_LOG_KEYWORDS) {
                    if (msgLower.contains(keyword)) return;
                }
            }

            // Skip our own forwarding logs to prevent feedback loops
            if (message.contains("[Coder] Failed to push console logs")) return;

            String level = record.getLevel().getName(); // INFO, WARNING, SEVERE etc.
            long ts = System.currentTimeMillis();

            // Forward to all sessions that have console trusted
            for (Map.Entry<String, EditorSession> entry : sessions.entrySet()) {
                // We only forward if the session has been console-trusted
                // (tracked via the consoleStatus we pushed to the worker)
                // Since we don't mirror consoleStatus locally, we forward to all
                // TRUSTED editor sessions — the worker will gate the read side.
                if (entry.getValue().status == EditorSession.EditorStatus.TRUSTED) {
                    String tok = entry.getKey();
                    String logEntry = "{\"level\":\"" + escJson(level)
                            + "\",\"text\":\"" + escJson(message)
                            + "\",\"ts\":" + ts + "}";
                    plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () ->
                            pushConsoleLogs(tok, Collections.singletonList(logEntry)));
                }
            }
        }

        @Override public void flush() {}
        @Override public void close() {}
    }

    // ─── Cleanup ──────────────────────────────────────────────────────────────

    private void invalidateSession(String token, String playerName) {
        sessions.remove(token);
        playerTokens.remove(playerName.toLowerCase());
    }

    private void cleanup(String playerName) {
        String token = playerTokens.remove(playerName.toLowerCase());
        if (token != null) sessions.remove(token);
    }

    public void shutdown() {
        if (pollTask != null) pollTask.cancel();
        removeConsoleLogHandler();
        for (String token : sessions.keySet()) {
            try {
                String payload = "{\"token\":\"" + token + "\"}";
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(WORKER_URL + "/api/session/stop"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload))
                        .build();
                http.send(req, HttpResponse.BodyHandlers.ofString());
            } catch (Exception ignored) {}
        }
        sessions.clear();
        playerTokens.clear();
    }

    // ─── Utilities ────────────────────────────────────────────────────────────

    private String generateToken(String playerName) {
        try {
            String raw = playerName + ":" + UUID.randomUUID() + ":" + System.currentTimeMillis();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return UUID.randomUUID().toString().replace("-", "");
        }
    }

    private String buildStartPayload(String token, String playerName, List<Map<String, String>> files) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"token\":\"").append(token).append("\",");
        sb.append("\"playerName\":\"").append(escJson(playerName)).append("\",");
        sb.append("\"files\":[");
        for (int i = 0; i < files.size(); i++) {
            Map<String, String> f = files.get(i);
            sb.append("{\"name\":\"").append(escJson(f.get("name"))).append("\",");
            sb.append("\"content\":\"").append(escJson(f.get("content"))).append("\"}");
            if (i < files.size() - 1) sb.append(",");
        }
        sb.append("]}");
        return sb.toString();
    }

    /** Fetch the current content of a single file from the worker */
    private String fetchFileContent(String token, String fileName) {
        try {
            String encodedName = java.net.URLEncoder.encode(fileName, StandardCharsets.UTF_8);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(WORKER_URL + "/api/file?token=" + token + "&name=" + encodedName))
                    .GET()
                    .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200) return null;
            return extractJson(res.body(), "content");
        } catch (Exception e) {
            plugin.getLogger().warning("Editor failed to fetch file content: " + fileName + " - " + e.getMessage());
            return null;
        }
    }

    /** JSON string value extractor that correctly handles escape sequences */
    private String extractJson(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return null;
        start += search.length();

        StringBuilder result = new StringBuilder();
        boolean escaped = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                if      (c == 'n')  result.append('\n');
                else if (c == 'r')  result.append('\r');
                else if (c == 't')  result.append('\t');
                else                result.append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                break;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    private String escJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}