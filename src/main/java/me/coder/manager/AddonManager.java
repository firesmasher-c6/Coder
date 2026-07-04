package me.coder.manager;

import me.coder.CoderPlugin;
import me.coder.api.CoderAddon;
import me.coder.api.VerifyGenerator;
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
     * Load all addons from the plugins folder with verification
     */
    public void loadAddons() {
        if (!addonsFolder.exists()) {
            addonsFolder.mkdirs();
        }

        File[] files = addonsFolder.listFiles((dir, name) -> name.endsWith(".jar") && !name.equalsIgnoreCase("Coder.jar"));
        
        if (files == null || files.length == 0) {
            plugin.getLogger().info("[Coder] No addon JARs found in plugins folder.");
            return;
        }

        plugin.getLogger().info("[Coder] ========================================");
        plugin.getLogger().info("[Coder] Scanning " + files.length + " addon(s)...");
        plugin.getLogger().info("[Coder] ========================================");

        for (File addonFile : files) {
            plugin.getLogger().info("[Coder] Scanning: " + addonFile.getName());
            
            try {
                if (!VerifyGenerator.isAddonVerified(addonFile)) {
                    plugin.getLogger().warning("[Coder] ✗ REJECTED (Not Verified): " + addonFile.getName());
                    rejectedAddons.add(addonFile.getName());
                    continue;
                }
                
                plugin.getLogger().info("[Coder] ✓ Verified: " + addonFile.getName());
                loadAddon(addonFile);
                
            } catch (Exception e) {
                plugin.getLogger().severe("[Coder] ✗ REJECTED (Error): " + addonFile.getName());
                rejectedAddons.add(addonFile.getName());
                e.printStackTrace();
            }
        }
        
        plugin.getLogger().info("[Coder] ========================================");
        plugin.getLogger().info("[Coder] Scan complete. Loaded: " + loadedAddons.size() + " | Rejected: " + rejectedAddons.size());
        plugin.getLogger().info("[Coder] ========================================");
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
                plugin.getLogger().warning("[Coder] Could not find CoderAddon implementation in: " + addonName);
                return;
            }

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
     * Get rejected addon count
     */
    public int getRejectedCount() {
        return rejectedAddons.size();
    }

    /**
     * Get rejected addon list
     */
    public List<String> getRejectedAddons() {
        return new ArrayList<>(rejectedAddons);
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
                if (VerifyGenerator.isAddonVerified(addonFile)) {
                    loadAddon(addonFile);
                    sender.sendMessage("§aAddon reloaded: " + name);
                } else {
                    sender.sendMessage("§cAddon verification failed: " + name);
                }
            } else {
                sender.sendMessage("§cAddon file not found: " + name + ".jar");
            }
        } catch (Exception e) {
            sender.sendMessage("§cError reloading addon: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Verify and generate VERIFIED.vf for an addon
     */
    public void verifyAddon(String addonName, CommandSender sender) {
        File addonFile = new File(addonsFolder, addonName + ".jar");
        
        if (!addonFile.exists()) {
            sender.sendMessage("§cAddon file not found: " + addonName + ".jar");
            return;
        }
        
        if (VerifyGenerator.verifyAddon(addonFile)) {
            sender.sendMessage("§a✓ Addon verified and signed: " + addonName);
            plugin.getLogger().info("[Coder] ✓ Verification generated for: " + addonName);
        } else {
            sender.sendMessage("§c✗ Addon verification failed (must implement CoderAddonSecurity): " + addonName);
            plugin.getLogger().warning("[Coder] ✗ Verification failed for: " + addonName);
        }
    }
}