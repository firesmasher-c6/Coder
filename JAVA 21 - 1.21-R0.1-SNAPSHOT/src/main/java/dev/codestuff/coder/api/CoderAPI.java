package dev.codestuff.coder.api;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CoderAPI {

    private static CoderAPI instance;
    private static final String PREFIX = "§f[§bCoder§f] ";

    private static final Map<String, CoderCommandHandler> commandHandlers = new HashMap<>();
    private static JavaExecutionHandler javaHandler;
    private static ScriptExecutionHandler pythonHandler;
    private static ScriptExecutionHandler luaHandler;
    private static final Map<String, ScriptPreprocessor> preprocessors = new HashMap<>();
    private static final Map<String, ScriptPostprocessor> postprocessors = new HashMap<>();
    private static final List<CoderEventListener> eventListeners = new ArrayList<>();
    private static final List<CoderTabCompleter> tabCompleters = new ArrayList<>();
    private static final Map<String, CoderAddon> addonRegistry = new LinkedHashMap<>();
    private static final Map<String, Map<String, Object>> addonDataStore = new ConcurrentHashMap<>();
    private static Plugin hostPlugin;

    private CoderAPI() {}

    public static CoderAPI getInstance() {
        if (instance == null) instance = new CoderAPI();
        return instance;
    }

    public static void setHostPlugin(Plugin plugin) {
        hostPlugin = plugin;
    }

    // ==================== MESSAGES ====================

    public void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(PREFIX + message);
    }

    public void sendSuccess(CommandSender sender, String message) {
        sender.sendMessage(PREFIX + "§a✓ " + message);
    }

    public void sendError(CommandSender sender, String message) {
        sender.sendMessage(PREFIX + "§c✗ " + message);
    }

    public void sendInfo(CommandSender sender, String message) {
        sender.sendMessage(PREFIX + "§9ℹ " + message);
    }

    public void sendWarning(CommandSender sender, String message) {
        sender.sendMessage(PREFIX + "§e⚠ " + message);
    }

    public void sendDebug(CommandSender sender, String message) {
        sender.sendMessage(PREFIX + "§8[DEBUG] " + message);
    }

    public void sendRaw(CommandSender sender, String message) {
        sender.sendMessage(message);
    }

    public void broadcastMessage(String message) {
        String msg = PREFIX + message;
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(msg));
        Bukkit.getConsoleSender().sendMessage(msg);
    }

    public void broadcast(String message) {
        broadcastMessage(message);
    }

    public void broadcastSuccess(String message) {
        String msg = PREFIX + "§a✓ " + message;
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(msg));
        Bukkit.getConsoleSender().sendMessage(msg);
    }

    public void broadcastWarning(String message) {
        String msg = PREFIX + "§e⚠ " + message;
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(msg));
        Bukkit.getConsoleSender().sendMessage(msg);
    }

    public void broadcastError(String message) {
        String msg = PREFIX + "§c✗ " + message;
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(msg));
        Bukkit.getConsoleSender().sendMessage(msg);
    }

    // ==================== PLAYERS ====================

    public boolean isPlayer(CommandSender sender) {
        return sender instanceof Player;
    }

    public Player getPlayer(String name) {
        return Bukkit.getPlayer(name);
    }

    public Player getPlayerByUUID(UUID uuid) {
        return Bukkit.getPlayer(uuid);
    }

    public Player[] getOnlinePlayers() {
        return Bukkit.getOnlinePlayers().toArray(new Player[0]);
    }

    public int getOnlinePlayerCount() {
        return Bukkit.getOnlinePlayers().size();
    }

    public void teleportPlayer(Player player, Location location) {
        player.teleport(location);
    }

    public void damagePlayer(Player player, double damage) {
        player.damage(damage);
    }

    public void healPlayer(Player player, double amount) {
        double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
        player.setHealth(Math.min(player.getHealth() + amount, maxHealth));
    }

    public void setPlayerHealth(Player player, double health) {
        double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
        player.setHealth(Math.min(Math.max(0, health), maxHealth));
    }

    public double getPlayerHealth(Player player) {
        return player.getHealth();
    }

    public int getPlayerLevel(Player player) {
        return player.getLevel();
    }

    public void setPlayerLevel(Player player, int level) {
        player.setLevel(level);
    }

    public void addPlayerExp(Player player, int amount) {
        player.giveExp(amount);
    }

    public int getPlayerFoodLevel(Player player) {
        return player.getFoodLevel();
    }

    public void setPlayerFoodLevel(Player player, int level) {
        player.setFoodLevel(Math.min(Math.max(0, level), 20));
    }

    public boolean isPlayerOp(Player player) {
        return player.isOp();
    }

    public boolean hasPermission(Player player, String permission) {
        return player.hasPermission(permission);
    }

    public void kickPlayer(Player player, String reason) {
        player.kickPlayer(reason);
    }

    public String getPlayerGameMode(Player player) {
        return player.getGameMode().name();
    }

    public void setPlayerGameMode(Player player, String gameMode) {
        try {
            player.setGameMode(org.bukkit.GameMode.valueOf(gameMode.toUpperCase()));
        } catch (IllegalArgumentException ignored) {}
    }

    public Location getPlayerLocation(Player player) {
        return player.getLocation();
    }

    public String getPlayerWorldName(Player player) {
        return player.getWorld().getName();
    }

    // ==================== INVENTORY / ITEMS ====================

    public boolean giveItem(Player player, String materialName, int amount) {
        Material mat = Material.matchMaterial(materialName);
        if (mat == null || mat == Material.AIR) return false;
        player.getInventory().addItem(new ItemStack(mat, Math.max(1, amount)));
        return true;
    }

    public void giveItem(Player player, ItemStack item) {
        player.getInventory().addItem(item.clone());
    }

    public int removeItem(Player player, String materialName, int amount) {
        Material mat = Material.matchMaterial(materialName);
        if (mat == null) return 0;
        int remaining = amount;
        for (ItemStack slot : player.getInventory().getContents()) {
            if (remaining <= 0) break;
            if (slot == null || slot.getType() != mat) continue;
            int take = Math.min(slot.getAmount(), remaining);
            slot.setAmount(slot.getAmount() - take);
            remaining -= take;
        }
        return amount - remaining;
    }

    public void clearInventory(Player player) {
        player.getInventory().clear();
    }

    public int countItem(Player player, String materialName) {
        Material mat = Material.matchMaterial(materialName);
        if (mat == null) return 0;
        int count = 0;
        for (ItemStack slot : player.getInventory().getContents()) {
            if (slot != null && slot.getType() == mat) count += slot.getAmount();
        }
        return count;
    }

    public boolean hasItem(Player player, String materialName, int amount) {
        return countItem(player, materialName) >= amount;
    }

    // ==================== EFFECTS ====================

    public boolean playSound(Player player, String soundName, float volume, float pitch) {
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, volume, pitch);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    public boolean spawnParticle(Location location, String particleName, int count) {
        try {
            Particle particle = Particle.valueOf(particleName.toUpperCase());
            if (location.getWorld() == null) return false;
            location.getWorld().spawnParticle(particle, location, count);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    public void sendActionBar(Player player, String message) {
        player.sendActionBar(net.kyori.adventure.text.Component.text(message));
    }

    public void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }

    // ==================== SCOREBOARD ====================

    public void setSidebar(Player player, String title, List<String> lines) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("coder_sidebar", "dummy", title);
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        int score = Math.min(lines.size(), 15);
        for (String line : lines) {
            obj.getScore(line).setScore(score--);
        }
        player.setScoreboard(board);
    }

    public void clearSidebar(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    // ==================== SCHEDULER ====================

    public BukkitTask runTask(Runnable task) {
        return Bukkit.getScheduler().runTask(hostPlugin, task);
    }

    public BukkitTask runTaskLater(Runnable task, long delayTicks) {
        return Bukkit.getScheduler().runTaskLater(hostPlugin, task, delayTicks);
    }

    public BukkitTask runTaskAsync(Runnable task) {
        return Bukkit.getScheduler().runTaskAsynchronously(hostPlugin, task);
    }

    public BukkitTask runTaskTimer(Runnable task, long delayTicks, long periodTicks) {
        return Bukkit.getScheduler().runTaskTimer(hostPlugin, task, delayTicks, periodTicks);
    }

    // ==================== WORLDS ====================

    public World getWorld(String name) {
        return Bukkit.getWorld(name);
    }

    public World[] getWorlds() {
        return Bukkit.getWorlds().toArray(new World[0]);
    }

    public String[] getWorldNames() {
        return Bukkit.getWorlds().stream().map(World::getName).toArray(String[]::new);
    }

    // ==================== COMMANDS ====================

    public boolean executeCommand(String command) {
        try {
            return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean executeCommandAsPlayer(Player player, String command) {
        try {
            return Bukkit.dispatchCommand(player, command);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean executeCommandAsConsole(String command) {
        return executeCommand(command);
    }

    // ==================== LOGGING ====================

    public void log(String message) {
        Bukkit.getLogger().info("[Coder] " + message);
    }

    public void logWarning(String message) {
        Bukkit.getLogger().warning("[Coder] " + message);
    }

    public void logError(String message) {
        Bukkit.getLogger().severe("[Coder] " + message);
    }

    public void logDebug(String message) {
        Bukkit.getLogger().info("[Coder-DEBUG] " + message);
    }

    // ==================== SERVER INFO ====================

    public String getServerMotd() {
        return Bukkit.getMotd();
    }

    public String getBukkitVersion() {
        return Bukkit.getVersion();
    }

    public String getMinecraftVersion() {
        return Bukkit.getServer().getVersion();
    }

    public int getMaxPlayers() {
        return Bukkit.getMaxPlayers();
    }

    public boolean isServerRunning() {
        return !Bukkit.getServer().isStopping();
    }

    public long getServerTicks() {
        return Bukkit.getServer().getCurrentTick();
    }

    public double getServerTPS() {
        return Bukkit.getTPS()[0];
    }

    public double getServerMSPT() {
        return Bukkit.getAverageTickTime();
    }

    // ==================== PLUGINS ====================

    public Plugin getPlugin(String name) {
        return Bukkit.getPluginManager().getPlugin(name);
    }

    public Plugin[] getPlugins() {
        return Bukkit.getPluginManager().getPlugins();
    }

    public boolean isPluginEnabled(String name) {
        Plugin p = Bukkit.getPluginManager().getPlugin(name);
        return p != null && p.isEnabled();
    }

    // ==================== ADDON REGISTRY ====================

    public void registerAddon(CoderAddon addon) {
        addonRegistry.put(addon.getName(), addon);
    }

    public void unregisterAddon(String addonName) {
        addonRegistry.remove(addonName);
        addonDataStore.remove(addonName);
    }

    public CoderAddon getAddon(String name) {
        return addonRegistry.get(name);
    }

    public Map<String, CoderAddon> getAddons() {
        return Collections.unmodifiableMap(addonRegistry);
    }

    public boolean isAddonLoaded(String name) {
        return addonRegistry.containsKey(name);
    }

    // ==================== ADDON DATA STORE ====================

    public void setAddonData(String addonName, String key, Object value) {
        addonDataStore.computeIfAbsent(addonName, k -> new ConcurrentHashMap<>()).put(key, value);
    }

    public Object getAddonData(String addonName, String key) {
        Map<String, Object> data = addonDataStore.get(addonName);
        return data != null ? data.get(key) : null;
    }

    public Object getAddonData(String addonName, String key, Object defaultValue) {
        Object val = getAddonData(addonName, key);
        return val != null ? val : defaultValue;
    }

    public void removeAddonData(String addonName, String key) {
        Map<String, Object> data = addonDataStore.get(addonName);
        if (data != null) data.remove(key);
    }

    public Map<String, Object> getAllAddonData(String addonName) {
        return Collections.unmodifiableMap(addonDataStore.getOrDefault(addonName, Collections.emptyMap()));
    }

    // ==================== EVENT FIRING ====================

    public void fireScriptStart(String scriptName, String language) {
        for (CoderEventListener l : new ArrayList<>(eventListeners)) {
            try { l.onScriptStart(scriptName, language); } catch (Exception ignored) {}
        }
        for (CoderAddon addon : addonRegistry.values()) {
            try { addon.onScriptRun(scriptName, language); } catch (Exception ignored) {}
        }
    }

    public void fireScriptEnd(String scriptName, String language, boolean success) {
        for (CoderEventListener l : new ArrayList<>(eventListeners)) {
            try { l.onScriptEnd(scriptName, language, success); } catch (Exception ignored) {}
        }
    }

    public void fireScriptError(String scriptName, String language, Throwable error) {
        for (CoderEventListener l : new ArrayList<>(eventListeners)) {
            try { l.onScriptError(scriptName, language, error); } catch (Exception ignored) {}
        }
    }

    public void fireAddonLoad(String addonName) {
        for (CoderEventListener l : new ArrayList<>(eventListeners)) {
            try { l.onAddonLoad(addonName); } catch (Exception ignored) {}
        }
        for (CoderAddon addon : addonRegistry.values()) {
            try { addon.onAddonLoaded(addonName); } catch (Exception ignored) {}
        }
    }

    public void fireAddonUnload(String addonName) {
        for (CoderEventListener l : new ArrayList<>(eventListeners)) {
            try { l.onAddonUnload(addonName); } catch (Exception ignored) {}
        }
        for (CoderAddon addon : addonRegistry.values()) {
            try { addon.onAddonUnloaded(addonName); } catch (Exception ignored) {}
        }
    }

    public void fireCommandExecute(String subcommand, String[] args) {
        for (CoderEventListener l : new ArrayList<>(eventListeners)) {
            try { l.onCommandExecute(subcommand, args); } catch (Exception ignored) {}
        }
    }

    // ==================== ENGINE INTEGRATION ====================

    public void registerCoderCommand(String subcommand, CoderCommandHandler handler) {
        commandHandlers.put(subcommand, handler);
    }

    public void unregisterCoderCommand(String subcommand) {
        commandHandlers.remove(subcommand);
    }

    public Set<String> getRegisteredCoderCommands() {
        return new HashSet<>(commandHandlers.keySet());
    }

    public CoderCommandHandler getCoderCommand(String subcommand) {
        return commandHandlers.get(subcommand);
    }

    public void registerJavaHandler(JavaExecutionHandler handler) {
        javaHandler = handler;
    }

    public void registerPythonHandler(ScriptExecutionHandler handler) {
        pythonHandler = handler;
    }

    public void registerLuaHandler(ScriptExecutionHandler handler) {
        luaHandler = handler;
    }

    public JavaExecutionHandler getJavaHandler() {
        return javaHandler;
    }

    public ScriptExecutionHandler getPythonHandler() {
        return pythonHandler;
    }

    public ScriptExecutionHandler getLuaHandler() {
        return luaHandler;
    }

    public void registerPreprocessor(String language, ScriptPreprocessor preprocessor) {
        preprocessors.put(language, preprocessor);
    }

    public void registerPostprocessor(String language, ScriptPostprocessor postprocessor) {
        postprocessors.put(language, postprocessor);
    }

    public ScriptPreprocessor getPreprocessor(String language) {
        return preprocessors.get(language);
    }

    public ScriptPostprocessor getPostprocessor(String language) {
        return postprocessors.get(language);
    }

    public void registerEventListener(CoderEventListener listener) {
        eventListeners.add(listener);
    }

    public void unregisterEventListener(CoderEventListener listener) {
        eventListeners.remove(listener);
    }

    public List<CoderEventListener> getEventListeners() {
        return new ArrayList<>(eventListeners);
    }

    public void registerTabCompleter(CoderTabCompleter completer) {
        tabCompleters.add(completer);
    }

    public List<CoderTabCompleter> getTabCompleters() {
        return new ArrayList<>(tabCompleters);
    }
}