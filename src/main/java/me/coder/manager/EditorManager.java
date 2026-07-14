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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class EditorManager {

    private static final String WORKER_URL = "https://codestuff-coder.darreltampus39.workers.dev";

    private final CoderPlugin plugin;
    private final HttpClient http = HttpClient.newHttpClient();

    // token -> session
    private final Map<String, EditorSession> sessions = new ConcurrentHashMap<>();
    // playerName (lowercase) -> token  (one session per player)
    private final Map<String, String> playerTokens = new ConcurrentHashMap<>();

    private BukkitTask pollTask;

    public EditorManager(CoderPlugin plugin) {
        this.plugin = plugin;
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

        // Build file list to send to worker
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

    // ─── Polling ──────────────────────────────────────────────────────────────

    private void startPolling() {
        // Poll every 3 seconds
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

                    // Parse minimal JSON manually to avoid needing a dependency
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
                                    player.sendMessage("§7Run §f/coder editor trust " + name + " §7to allow or §f/coder editor doNotTrust " + name + " §7to reject.");
                                });
                            }
                            break;
                        }
                        case "save_file": {
                            String fileName = extractJson(body, "fileName");
                            String content = extractJson(body, "content");
                            if (fileName != null && content != null && isAllowedFile(fileName)) {
                                saveFile(fileName, content);
                                Bukkit.getScheduler().runTask(plugin, () -> {
                                    CommandSender player = Bukkit.getPlayerExact(session.playerName);
                                    if (player != null)
                                        player.sendMessage("§a[Coder] §f" + session.browserUser + " §asaved §f" + fileName);
                                });
                            }
                            break;
                        }
                        case "session_closed": {
                            String pName = session.playerName;
                            invalidateSession(token, pName);
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                CommandSender player = Bukkit.getPlayerExact(pName);
                                if (player != null)
                                    player.sendMessage("§e[Coder] Editor session was closed from the browser.");
                            });
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
                // Skip backups and compiled class output
                String name = f.getName();
                if (name.equals("backups") || name.equals("JavaClasses")) continue;
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
        // Must not escape the plugin folder
        return !fileName.contains("..") && !fileName.startsWith("/");
    }

    private void saveFile(String fileName, String content) {
        try {
            File target = new File(plugin.getDataFolder(), fileName);
            // Safety: must be inside plugin data folder
            if (!target.getCanonicalPath().startsWith(plugin.getDataFolder().getCanonicalPath())) return;
            target.getParentFile().mkdirs();
            Files.writeString(target.toPath(), content, StandardCharsets.UTF_8);
        } catch (Exception e) {
            plugin.getLogger().warning("[Coder] Editor failed to save file: " + fileName + " - " + e.getMessage());
        }
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
        // Tell worker all sessions are dead
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

    /** Very minimal JSON string value extractor — no library needed */
    private String extractJson(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return null;
        start += search.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return null;
        return json.substring(start, end).replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private String escJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}