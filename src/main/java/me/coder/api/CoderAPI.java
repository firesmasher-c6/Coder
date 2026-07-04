package me.coder.api;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import java.util.*;

public class CoderAPI {
    
    private static CoderAPI instance;
    private static final String PREFIX = "§f[§bCoder§f] ";
    
    private static Map<String, CoderCommandHandler> commandHandlers = new HashMap<>();
    private static JavaExecutionHandler javaHandler;
    private static ScriptExecutionHandler pythonHandler;
    private static ScriptExecutionHandler luaHandler;
    private static Map<String, ScriptPreprocessor> preprocessors = new HashMap<>();
    private static Map<String, ScriptPostprocessor> postprocessors = new HashMap<>();
    private static List<CoderEventListener> eventListeners = new ArrayList<>();
    private static List<CoderTabCompleter> tabCompleters = new ArrayList<>();
    
    private CoderAPI() {
    }
    
    public static CoderAPI getInstance() {
        if (instance == null) {
            instance = new CoderAPI();
        }
        return instance;
    }
    
    // ==================== MESSAGE METHODS ====================
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
        Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(msg));
        Bukkit.getConsoleSender().sendMessage(msg);
    }
    
    // ==================== PLAYER METHODS ====================
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
        double health = player.getHealth() + amount;
        double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
        if (health > maxHealth) {
            health = maxHealth;
        }
        player.setHealth(health);
    }
    
    public void setPlayerHealth(Player player, double health) {
        double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue();
        if (health > maxHealth) {
            health = maxHealth;
        }
        player.setHealth(Math.max(0, health));
    }
    
    public double getPlayerHealth(Player player) {
        return player.getHealth();
    }
    
    // ==================== WORLD METHODS ====================
    public World getWorld(String name) {
        return Bukkit.getWorld(name);
    }
    
    public World[] getWorlds() {
        return Bukkit.getWorlds().toArray(new World[0]);
    }
    
    public String[] getWorldNames() {
        return Bukkit.getWorlds().stream().map(World::getName).toArray(String[]::new);
    }
    
    // ==================== COMMAND EXECUTION ====================
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
    
    // ==================== LOGGING METHODS ====================
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
    
    // ==================== PLUGIN METHODS ====================
    public Plugin getPlugin(String name) {
        return Bukkit.getPluginManager().getPlugin(name);
    }
    
    public Plugin[] getPlugins() {
        return Bukkit.getPluginManager().getPlugins();
    }
    
    public boolean isPluginEnabled(String name) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(name);
        return plugin != null && plugin.isEnabled();
    }
    
    // ==================== BROADCAST METHODS ====================
    public void broadcast(String message) {
        broadcastMessage(message);
    }
    
    public void broadcastSuccess(String message) {
        String msg = PREFIX + "§a✓ " + message;
        Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(msg));
        Bukkit.getConsoleSender().sendMessage(msg);
    }
    
    public void broadcastWarning(String message) {
        String msg = PREFIX + "§e⚠ " + message;
        Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(msg));
        Bukkit.getConsoleSender().sendMessage(msg);
    }
    
    public void broadcastError(String message) {
        String msg = PREFIX + "§c✗ " + message;
        Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(msg));
        Bukkit.getConsoleSender().sendMessage(msg);
    }
    
    // ==================== CODER ENGINE INTEGRATION ====================
    
    /**
     * Register custom /coder subcommand handler
     */
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