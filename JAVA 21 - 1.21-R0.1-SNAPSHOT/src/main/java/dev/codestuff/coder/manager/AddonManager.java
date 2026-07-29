package dev.codestuff.coder.manager;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import dev.codestuff.coder.CoderPlugin;
import dev.codestuff.coder.api.CoderAddon;
import dev.codestuff.coder.api.CoderAPI;

import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class AddonManager implements Listener {

    private final CoderPlugin plugin;
    private final Map<String, CoderAddon> loadedAddons = new LinkedHashMap<>();
    private final Set<String> warnedLegacyPlugins = new HashSet<>();
    private final Map<String, BukkitTask> tickTasks = new HashMap<>();
    private final Map<String, BukkitTask> saveTasks = new HashMap<>();

    public AddonManager(CoderPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        CoderAPI.setHostPlugin(plugin);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        for (CoderAddon addon : new ArrayList<>(loadedAddons.values())) {
            try { addon.onPlayerJoin(player); } catch (Exception e) {
                plugin.getLogger().warning("[AddonManager] " + addon.getName() + " threw on onPlayerJoin: " + e.getMessage());
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        for (CoderAddon addon : new ArrayList<>(loadedAddons.values())) {
            try { addon.onPlayerLeave(player); } catch (Exception e) {
                plugin.getLogger().warning("[AddonManager] " + addon.getName() + " threw on onPlayerLeave: " + e.getMessage());
            }
        }
    }

    private Set<String> scanJarsForCoderDependents() {
        Set<String> names = new HashSet<>();
        File pluginsDir = plugin.getDataFolder().getParentFile();
        if (pluginsDir == null || !pluginsDir.isDirectory()) return names;
        File[] jars = pluginsDir.listFiles(f -> f.isFile() && f.getName().endsWith(".jar"));
        if (jars == null) return names;
        for (File jar : jars) {
            try (JarFile jf = new JarFile(jar)) {
                String name = extractCoderDependentName(jf);
                if (name != null) names.add(name);
            } catch (Exception e) {
                plugin.getLogger().warning("[AddonManager] Could not scan jar " + jar.getName() + ": " + e.getMessage());
            }
        }
        return names;
    }

    private String extractCoderDependentName(JarFile jf) throws Exception {
        JarEntry paperEntry = jf.getJarEntry("paper-plugin.yml");
        if (paperEntry != null) {
            try (InputStream is = jf.getInputStream(paperEntry)) {
                String yaml = new String(is.readAllBytes());
                if (hasPaperCoderDependency(yaml)) return extractYamlField(yaml, "name");
            }
        }
        JarEntry pluginEntry = jf.getJarEntry("plugin.yml");
        if (pluginEntry != null) {
            try (InputStream is = jf.getInputStream(pluginEntry)) {
                String yaml = new String(is.readAllBytes());
                if (hasClassicCoderDependency(yaml)) return extractYamlField(yaml, "name");
            }
        }
        return null;
    }

    private boolean usesLegacyApiPath(JarFile jf) throws Exception {
        byte[] needle = "me/coder/api/".getBytes();
        Enumeration<JarEntry> entries = jf.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (!entry.getName().endsWith(".class")) continue;
            try (InputStream is = jf.getInputStream(entry)) {
                if (containsSequence(is.readAllBytes(), needle)) return true;
            }
        }
        return false;
    }

    private boolean containsSequence(byte[] haystack, byte[] needle) {
        outer:
        for (int i = 0; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) continue outer;
            }
            return true;
        }
        return false;
    }

    private Set<String> scanJarsForLegacyApi() {
        Set<String> names = new HashSet<>();
        File pluginsDir = plugin.getDataFolder().getParentFile();
        if (pluginsDir == null || !pluginsDir.isDirectory()) return names;
        File[] jars = pluginsDir.listFiles(f -> f.isFile() && f.getName().endsWith(".jar"));
        if (jars == null) return names;
        for (File jar : jars) {
            try (JarFile jf = new JarFile(jar)) {
                String name = extractPluginName(jf);
                if (plugin.getName().equals(name)) continue;
                if (!usesLegacyApiPath(jf)) continue;
                if (name != null) names.add(name);
            } catch (Exception e) {
                plugin.getLogger().warning("[AddonManager] Could not scan jar " + jar.getName() + " for legacy API: " + e.getMessage());
            }
        }
        return names;
    }

    private String extractPluginName(JarFile jf) throws Exception {
        for (String descriptor : new String[]{"paper-plugin.yml", "plugin.yml"}) {
            JarEntry entry = jf.getJarEntry(descriptor);
            if (entry == null) continue;
            try (InputStream is = jf.getInputStream(entry)) {
                String name = extractYamlField(new String(is.readAllBytes()), "name");
                if (name != null) return name;
            }
        }
        return null;
    }

    private boolean hasPaperCoderDependency(String yaml) {
        boolean inDependencies = false, inServer = false;
        int dependenciesIndent = -1, serverIndent = -1;
        for (String raw : yaml.split("\n")) {
            String line = raw.replace("\r", "");
            if (line.trim().isEmpty() || line.trim().startsWith("#")) continue;
            int indent = leadingSpaces(line);
            String trimmed = line.trim();
            if (!inDependencies) {
                if (trimmed.equals("dependencies:") || trimmed.startsWith("dependencies:")) {
                    inDependencies = true;
                    dependenciesIndent = indent;
                }
                continue;
            }
            if (indent <= dependenciesIndent && !trimmed.startsWith("dependencies")) {
                inDependencies = false;
                inServer = false;
                continue;
            }
            if (!inServer) {
                if (trimmed.equals("server:") || trimmed.startsWith("server:")) {
                    inServer = true;
                    serverIndent = indent;
                }
                continue;
            }
            if (indent <= serverIndent) { inServer = false; continue; }
            if (indent == serverIndent + 2 || (serverIndent == -1 && indent > 0)) {
                if (trimmed.replace(":", "").trim().equals("Coder")) return true;
            }
        }
        return false;
    }

    private boolean hasClassicCoderDependency(String yaml) {
        boolean inDepend = false;
        for (String raw : yaml.split("\n")) {
            String line = raw.replace("\r", "");
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            if (trimmed.startsWith("depend:") || trimmed.startsWith("soft-depend:")) {
                int bracket = trimmed.indexOf('[');
                if (bracket != -1) {
                    String list = trimmed.substring(bracket + 1, trimmed.lastIndexOf(']'));
                    for (String entry : list.split(",")) {
                        if (entry.trim().equals("Coder")) return true;
                    }
                    inDepend = false;
                } else {
                    inDepend = true;
                }
                continue;
            }
            if (inDepend) {
                if (trimmed.startsWith("-")) {
                    if (trimmed.substring(1).trim().equals("Coder")) return true;
                } else {
                    inDepend = false;
                }
            }
        }
        return false;
    }

    private String extractYamlField(String yaml, String field) {
        for (String raw : yaml.split("\n")) {
            String line = raw.replace("\r", "").trim();
            if (line.startsWith(field + ":")) {
                return line.substring(field.length() + 1).trim().replaceAll("^\"|\"$|^'|'$", "");
            }
        }
        return null;
    }

    private int leadingSpaces(String line) {
        int count = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') count++;
            else break;
        }
        return count;
    }

    private boolean meetsMinVersion(String running, String required) {
        if (required == null || required.isEmpty() || required.equals("0.0.0")) return true;
        String[] r = running.split("[^0-9]+");
        String[] q = required.split("[^0-9]+");
        int len = Math.max(r.length, q.length);
        for (int i = 0; i < len; i++) {
            int rv = i < r.length ? parseIntSafe(r[i]) : 0;
            int qv = i < q.length ? parseIntSafe(q[i]) : 0;
            if (rv != qv) return rv > qv;
        }
        return true;
    }

    private int parseIntSafe(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }

    public boolean isCoderAddon(Plugin p) {
        List<String> depend = p.getDescription().getDepend();
        List<String> softDepend = p.getDescription().getSoftDepend();
        return (depend != null && depend.contains("Coder"))
            || (softDepend != null && softDepend.contains("Coder"));
    }

    public List<Plugin> getAllCoderDependents() {
        Set<String> jarNames = scanJarsForCoderDependents();
        List<Plugin> result = new ArrayList<>();
        for (Plugin p : plugin.getServer().getPluginManager().getPlugins()) {
            if (isCoderAddon(p) || jarNames.contains(p.getName())) result.add(p);
        }
        return result;
    }

    public void loadAddons() {
        Set<String> jarDependentNames = scanJarsForCoderDependents();
        CoderAPI api = CoderAPI.getInstance();
        String runningVersion = plugin.getPluginMeta().getVersion();

        for (String addonName : scanJarsForLegacyApi()) {
            if (warnedLegacyPlugins.add(addonName)) {
                plugin.getLogger().warning("Potential Bad Coder Addon API Interaction.");
                plugin.getLogger().warning("at " + addonName);
                plugin.getLogger().warning("Api Path: dev.codestuff.coder.api");
                plugin.getLogger().warning("Addon Api Path: me.coder.api");
                plugin.getLogger().warning("If you're the publisher of this addon please use the correct api path.");
                plugin.getLogger().warning("Api Path \"me.coder.api\" is open. Please use \"dev.codestuff.coder.api\" as soon as possible.");
            }
        }

        for (Plugin p : plugin.getServer().getPluginManager().getPlugins()) {
            if (!(isCoderAddon(p) || jarDependentNames.contains(p.getName()))) continue;
            if (!p.isEnabled()) continue;
            if (!(p instanceof CoderAddon addon)) continue;

            String name = addon.getName();
            if (loadedAddons.containsKey(name)) continue;

            String minVer = addon.getMinCoderVersion();
            if (minVer != null && !minVer.isEmpty() && !meetsMinVersion(runningVersion, minVer)) {
                plugin.getLogger().warning("[AddonManager] Skipping \"" + name + "\": requires Coder >= " + minVer + " (running " + runningVersion + ")");
                continue;
            }

            boolean depsMet = true;
            for (String dep : addon.getDependencies()) {
                if (!loadedAddons.containsKey(dep)) {
                    plugin.getLogger().warning("[AddonManager] Skipping \"" + name + "\": missing addon dependency \"" + dep + "\"");
                    depsMet = false;
                    break;
                }
            }
            if (!depsMet) continue;

            try {
                for (String lang : new String[]{"java", "python", "lua"}) {
                    if (addon.getScriptPreprocessor(lang) != null)
                        api.registerPreprocessor(lang, addon.getScriptPreprocessor(lang));
                    if (addon.getScriptPostprocessor(lang) != null)
                        api.registerPostprocessor(lang, addon.getScriptPostprocessor(lang));
                }

                if (addon.getCustomJavaHandler()   != null) api.registerJavaHandler(addon.getCustomJavaHandler());
                if (addon.getCustomPythonHandler() != null) api.registerPythonHandler(addon.getCustomPythonHandler());
                if (addon.getCustomLuaHandler()    != null) api.registerLuaHandler(addon.getCustomLuaHandler());

                addon.registerCustomCommands(api);
                if (addon.getEventListener() != null) api.registerEventListener(addon.getEventListener());
                if (addon.getTabCompleter()  != null) api.registerTabCompleter(addon.getTabCompleter());

                api.registerAddon(addon);
                addon.onLoad();
                loadedAddons.put(name, addon);
                plugin.getLogger().info("Coder addon registered: " + name + " v" + addon.getVersion() + " by " + addon.getAuthor());

                api.fireAddonLoad(name);

                if (addon.wantsTick()) {
                    BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
                        try { addon.onTick(); } catch (Exception e) {
                            plugin.getLogger().warning("[AddonManager] " + name + " threw on onTick: " + e.getMessage());
                        }
                    }, 1L, 1L);
                    tickTasks.put(name, task);
                }

                if (addon.shouldAutoSave()) {
                    long intervalTicks = addon.getAutoSaveInterval() * 20L;
                    BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
                        try { addon.onSave(); } catch (Exception e) {
                            plugin.getLogger().warning("[AddonManager] " + name + " threw on onSave: " + e.getMessage());
                        }
                    }, intervalTicks, intervalTicks);
                    saveTasks.put(name, task);
                }

            } catch (Exception e) {
                plugin.getLogger().severe("Failed to register addon: " + name + " - " + e.getMessage());
            }
        }

        for (CoderAddon addon : loadedAddons.values()) {
            try { addon.onAllAddonsLoaded(); } catch (Exception ignored) {}
        }
    }

    public void disableAddons() {
        CoderAPI api = CoderAPI.getInstance();

        tickTasks.values().forEach(BukkitTask::cancel);
        saveTasks.values().forEach(BukkitTask::cancel);
        tickTasks.clear();
        saveTasks.clear();

        for (CoderAddon addon : loadedAddons.values()) {
            String name = addon.getName();
            try {
                addon.onBeforeDisable();
                addon.onSave();
                addon.unregisterCustomCommands(api);
                addon.onDisable();
                api.fireAddonUnload(name);
                api.unregisterAddon(name);
            } catch (Exception e) {
                plugin.getLogger().severe("Error disabling addon: " + name + " - " + e.getMessage());
            }
        }
        loadedAddons.clear();
    }

    public void sendAddonList(CommandSender sender) {
        List<Plugin> dependents = getAllCoderDependents();
        if (dependents.isEmpty()) return;
        sender.sendMessage("§bℹ §fCoder Addons (" + dependents.size() + "):");
        for (Plugin p : dependents) {
            sender.sendMessage("§7 - §a" + p.getName());
        }
    }

    public Map<String, CoderAddon> getLoadedAddons() {
        return Collections.unmodifiableMap(loadedAddons);
    }
}