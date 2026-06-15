package me.coder.manager;

import me.coder.CoderPlugin;
import me.coder.api.CoderAddon;
import org.bukkit.command.CommandSender;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public class AddonManager {
    
    private final CoderPlugin plugin;
    private final Map<String, CoderAddon> loadedAddons = new HashMap<>();
    private final File addonsFolder;

    public AddonManager(CoderPlugin plugin) {
        this.plugin = plugin;
        this.addonsFolder = new File(plugin.getDataFolder(), "addons");
    }

    /**
     * Load all addons from the addons folder
     */
    public void loadAddons() {
        if (!addonsFolder.exists()) {
            addonsFolder.mkdirs();
        }

        File[] files = addonsFolder.listFiles((dir, name) -> name.endsWith(".jar"));
        
        if (files == null || files.length == 0) {
            plugin.getLogger().info("[Coder] No addons found in addons folder.");
            return;
        }

        plugin.getLogger().info("[Coder] Loading " + files.length + " addon(s)...");

        for (File addonFile : files) {
            try {
                loadAddon(addonFile);
            } catch (Exception e) {
                plugin.getLogger().severe("[Coder] Failed to load addon: " + addonFile.getName());
                e.printStackTrace();
            }
        }
    }

    /**
     * Load a single addon JAR file
     */
    private void loadAddon(File addonFile) throws Exception {
        URL[] urls = { addonFile.toURI().toURL() };
        try (URLClassLoader classLoader = new URLClassLoader(urls, this.getClass().getClassLoader())) {
            // Find CoderAddon implementations in the JAR
            String addonName = addonFile.getName().replace(".jar", "");
            
            // Try common naming patterns
            String[] possibleClasses = {
                addonName,
                addonName + "Addon",
                addonName + "Plugin",
                "Main",
                "Addon"
            };

            Class<?> addonClass = null;
            for (String className : possibleClasses) {
                try {
                    addonClass = classLoader.loadClass(className);
                    if (CoderAddon.class.isAssignableFrom(addonClass)) {
                        break;
                    }
                    addonClass = null;
                } catch (ClassNotFoundException ignored) {
                }
            }

            if (addonClass == null) {
                plugin.getLogger().warning("[Coder] Could not find CoderAddon implementation in: " + addonName);
                return;
            }

            // Instantiate and enable the addon
            if (CoderAddon.class.isAssignableFrom(addonClass)) {
                CoderAddon addon = (CoderAddon) addonClass.getDeclaredConstructor().newInstance();
                
                try {
                    addon.onEnable();
                    loadedAddons.put(addonName, addon);
                    plugin.getLogger().info("[Coder] ✓ Addon loaded: " + addon.getName() + " v" + addon.getVersion() + " by " + addon.getAuthor());
                } catch (Exception e) {
                    plugin.getLogger().severe("[Coder] Error enabling addon: " + addon.getName());
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Disable all addons
     */
    public void disableAddons() {
        for (CoderAddon addon : loadedAddons.values()) {
            try {
                addon.onDisable();
            } catch (Exception e) {
                plugin.getLogger().severe("[Coder] Error disabling addon: " + addon.getName());
                e.printStackTrace();
            }
        }
        loadedAddons.clear();
    }

    /**
     * Get a loaded addon by name
     */
    public CoderAddon getAddon(String name) {
        return loadedAddons.get(name);
    }

    /**
     * Get all loaded addons
     */
    public Collection<CoderAddon> getLoadedAddons() {
        return loadedAddons.values();
    }

    /**
     * Get addon count
     */
    public int getAddonCount() {
        return loadedAddons.size();
    }

    /**
     * Send addon list to player/console
     */
    public void sendAddonList(CommandSender sender) {
        if (loadedAddons.isEmpty()) {
            sender.sendMessage("§c[Coder] No addons loaded.");
            return;
        }

        sender.sendMessage("§6========================================");
        sender.sendMessage("§6=== Coder Addons ===");
        sender.sendMessage("§6========================================");
        sender.sendMessage("§aLoaded Addons: §f" + loadedAddons.size());
        sender.sendMessage("");

        int i = 1;
        for (CoderAddon addon : loadedAddons.values()) {
            sender.sendMessage("§e" + i + ". §a" + addon.getName() + " §8(v" + addon.getVersion() + ")");
            sender.sendMessage("   §7by §f" + addon.getAuthor());
            sender.sendMessage("   §7" + addon.getDescription());
            sender.sendMessage("");
            i++;
        }

        sender.sendMessage("§6========================================");
    }

    /**
     * Reload an addon
     */
    public void reloadAddon(String name, CommandSender sender) {
        if (!loadedAddons.containsKey(name)) {
            sender.sendMessage("§cAddon not found: " + name);
            return;
        }

        try {
            CoderAddon addon = loadedAddons.get(name);
            addon.onDisable();
            loadedAddons.remove(name);
            sender.sendMessage("§aAddon unloaded: " + name);
            
            // Reload from file
            File addonFile = new File(addonsFolder, name + ".jar");
            if (addonFile.exists()) {
                loadAddon(addonFile);
                sender.sendMessage("§aAddon reloaded: " + name);
            } else {
                sender.sendMessage("§cAddon file not found: " + name + ".jar");
            }
        } catch (Exception e) {
            sender.sendMessage("§cError reloading addon: " + e.getMessage());
            e.printStackTrace();
        }
    }
}