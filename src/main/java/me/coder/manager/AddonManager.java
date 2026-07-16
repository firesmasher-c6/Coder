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
    private final List<String> rejectedAddons = new ArrayList<>();

    public AddonManager(CoderPlugin plugin) {
        this.plugin = plugin;
        this.addonsFolder = new File(plugin.getServer().getWorldContainer().getParentFile(), "plugins");
    }

    /**
     * Load all addons from the plugins folder
     */
    public void loadAddons() {
        // Addon loading disabled - addon security system has been removed
    }

    /**
     * Load a single verified addon JAR file
     */
    private void loadAddon(File addonFile) throws Exception {
        URL[] urls = { addonFile.toURI().toURL() };
        try (URLClassLoader classLoader = new URLClassLoader(urls, this.getClass().getClassLoader())) {
            String addonName = addonFile.getName().replace(".jar", "");
            
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
                plugin.getLogger().warning("Could not find CoderAddon implementation in: " + addonName);
                return;
            }

            if (CoderAddon.class.isAssignableFrom(addonClass)) {
                CoderAddon addon = (CoderAddon) addonClass.getDeclaredConstructor().newInstance();
                
                try {
                    addon.onEnable();
                    loadedAddons.put(addonName, addon);
                    plugin.getLogger().info("✓ Addon loaded: " + addon.getName() + " v" + addon.getVersion() + " by " + addon.getAuthor());
                } catch (Exception e) {
                    plugin.getLogger().severe("Error enabling addon: " + addon.getName());
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
                plugin.getLogger().severe("Error disabling addon: " + addon.getName());
                e.printStackTrace();
            }
        }
        loadedAddons.clear();
    }

    /**
     * Send addon list to player/console
     */
    public void sendAddonList(CommandSender sender) {
        sender.sendMessage("§6========================================");
        sender.sendMessage("§6=== Coder Addons ===");
        sender.sendMessage("§6========================================");
        
        if (loadedAddons.isEmpty()) {
            sender.sendMessage("§cLoaded: 0");
        } else {
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
        }
        
        if (!rejectedAddons.isEmpty()) {
            sender.sendMessage("§cRejected Addons: §f" + rejectedAddons.size());
            sender.sendMessage("");
            int i = 1;
            for (String addon : rejectedAddons) {
                sender.sendMessage("§c" + i + ". " + addon + " §8(Not Verified)");
                i++;
            }
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