package dev.codestuff.coder.manager;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import dev.codestuff.coder.CoderPlugin;
import dev.codestuff.coder.api.CoderAddon;
import dev.codestuff.coder.api.CoderAPI;

import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.Enumeration;

public class AddonManager {

    private final CoderPlugin plugin;
    private final Map<String, CoderAddon> loadedAddons = new LinkedHashMap<>();
    private final Set<String> warnedLegacyPlugins = new HashSet<>();

    public AddonManager(CoderPlugin plugin) {
        this.plugin = plugin;
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
                if (hasPaperCoderDependency(yaml)) {
                    return extractYamlField(yaml, "name");
                }
            }
        }

        JarEntry pluginEntry = jf.getJarEntry("plugin.yml");
        if (pluginEntry != null) {
            try (InputStream is = jf.getInputStream(pluginEntry)) {
                String yaml = new String(is.readAllBytes());
                if (hasClassicCoderDependency(yaml)) {
                    return extractYamlField(yaml, "name");
                }
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
                byte[] bytes = is.readAllBytes();
                if (containsSequence(bytes, needle)) return true;
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
        boolean inDependencies = false;
        boolean inServer = false;
        int dependenciesIndent = -1;
        int serverIndent = -1;

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

            if (indent <= serverIndent) {
                inServer = false;
                continue;
            }

            if (indent == serverIndent + 2 || (serverIndent == -1 && indent > 0)) {
                String key = trimmed.replace(":", "").trim();
                if (key.equals("Coder")) return true;
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
                    String val = trimmed.substring(1).trim();
                    if (val.equals("Coder")) return true;
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
            if (isCoderAddon(p) || jarNames.contains(p.getName())) {
                result.add(p);
            }
        }
        return result;
    }

    public void loadAddons() {
        Set<String> jarDependentNames = scanJarsForCoderDependents();

        Set<String> legacyApiUsers = scanJarsForLegacyApi();
        for (String addonName : legacyApiUsers) {
            if (warnedLegacyPlugins.contains(addonName)) continue;
            warnedLegacyPlugins.add(addonName);
            plugin.getLogger().warning("Potential Bad Coder Addon API Interaction.");
            plugin.getLogger().warning("at " + addonName);
            plugin.getLogger().warning("Api Path: dev.codestuff.coder.api");
            plugin.getLogger().warning("Addon Api Path: me.coder.api");
            plugin.getLogger().warning("If you're the publisher of this addon please use the correct api path.");
            plugin.getLogger().warning("Api Path \"me.coder.api\" is open. Please use \"dev.codestuff.coder.api\" as soon as possible.");
        }

        for (Plugin p : plugin.getServer().getPluginManager().getPlugins()) {
            boolean dependsOnCoder = isCoderAddon(p) || jarDependentNames.contains(p.getName());
            if (!dependsOnCoder || !p.isEnabled()) continue;

            if (!(p instanceof CoderAddon)) continue;

            CoderAddon addon = (CoderAddon) p;
            String name = addon.getName();
            if (loadedAddons.containsKey(name)) continue;

            try {
                addon.registerCustomCommands(CoderAPI.getInstance());
                if (addon.getEventListener() != null)
                    CoderAPI.getInstance().registerEventListener(addon.getEventListener());
                if (addon.getTabCompleter() != null)
                    CoderAPI.getInstance().registerTabCompleter(addon.getTabCompleter());
                if (addon.getCustomJavaHandler() != null)
                    CoderAPI.getInstance().registerJavaHandler(addon.getCustomJavaHandler());
                if (addon.getCustomPythonHandler() != null)
                    CoderAPI.getInstance().registerPythonHandler(addon.getCustomPythonHandler());
                if (addon.getCustomLuaHandler() != null)
                    CoderAPI.getInstance().registerLuaHandler(addon.getCustomLuaHandler());
                loadedAddons.put(name, addon);
                plugin.getLogger().info("Coder addon registered: " + name
                        + " v" + addon.getVersion() + " by " + addon.getAuthor());
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to register addon: " + name + " - " + e.getMessage());
            }
        }

        for (CoderAddon addon : loadedAddons.values()) {
            try { addon.onAllAddonsLoaded(); } catch (Exception ignored) {}
        }
    }

    public void disableAddons() {
        for (CoderAddon addon : loadedAddons.values()) {
            try {
                addon.onBeforeDisable();
                addon.unregisterCustomCommands(CoderAPI.getInstance());
                addon.onDisable();
            } catch (Exception e) {
                plugin.getLogger().severe("Error disabling addon: " + addon.getName() + " - " + e.getMessage());
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