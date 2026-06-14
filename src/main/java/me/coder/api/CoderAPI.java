package me.coder.api;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;

/**
 * Coder API - Main API class for creating Coder Addons
 * 
 * Usage Example:
 * CoderAPI api = CoderAPI.getInstance();
 * api.sendMessage(sender, "Hello!");
 */
public class CoderAPI {
    
    private static CoderAPI instance;
    
    private CoderAPI() {
    }
    
    /**
     * Get the Coder API instance
     * @return CoderAPI singleton instance
     */
    public static CoderAPI getInstance() {
        if (instance == null) {
            instance = new CoderAPI();
        }
        return instance;
    }
    
    /**
     * Send a colored message to a sender
     * @param sender The CommandSender to send to
     * @param message The message with § color codes
     */
    public void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(message);
    }
    
    /**
     * Send a success message (green)
     * @param sender The CommandSender to send to
     * @param message The message to send
     */
    public void sendSuccess(CommandSender sender, String message) {
        sender.sendMessage("§a" + message);
    }
    
    /**
     * Send an error message (red)
     * @param sender The CommandSender to send to
     * @param message The message to send
     */
    public void sendError(CommandSender sender, String message) {
        sender.sendMessage("§c" + message);
    }
    
    /**
     * Send an info message (blue)
     * @param sender The CommandSender to send to
     * @param message The message to send
     */
    public void sendInfo(CommandSender sender, String message) {
        sender.sendMessage("§9" + message);
    }
    
    /**
     * Send a warning message (yellow)
     * @param sender The CommandSender to send to
     * @param message The message to send
     */
    public void sendWarning(CommandSender sender, String message) {
        sender.sendMessage("§e" + message);
    }
    
    /**
     * Check if a sender is a player
     * @param sender The CommandSender to check
     * @return true if sender is a Player
     */
    public boolean isPlayer(CommandSender sender) {
        return sender instanceof Player;
    }
    
    /**
     * Get a player by name
     * @param name The player name
     * @return The Player object or null if not found
     */
    public Player getPlayer(String name) {
        return Bukkit.getPlayer(name);
    }
    
    /**
     * Get all online players
     * @return Array of all online players
     */
    public Player[] getOnlinePlayers() {
        return Bukkit.getOnlinePlayers().toArray(new Player[0]);
    }
    
    /**
     * Get the number of online players
     * @return Number of online players
     */
    public int getOnlinePlayerCount() {
        return Bukkit.getOnlinePlayers().size();
    }
    
    /**
     * Broadcast a message to all players
     * @param message The message to broadcast
     */
    public void broadcast(String message) {
        Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(message));
        Bukkit.getConsoleSender().sendMessage(message);
    }
    
    /**
     * Execute a console command
     * @param command The command to execute (without /)
     * @return true if executed successfully
     */
    public boolean executeCommand(String command) {
        try {
            return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Log a message to the console
     * @param message The message to log
     */
    public void log(String message) {
        Bukkit.getLogger().info("[Coder] " + message);
    }
    
    /**
     * Log a warning message to the console
     * @param message The message to log
     */
    public void logWarning(String message) {
        Bukkit.getLogger().warning("[Coder] " + message);
    }
    
    /**
     * Log an error message to the console
     * @param message The message to log
     */
    public void logError(String message) {
        Bukkit.getLogger().severe("[Coder] " + message);
    }
    
    /**
     * Get the server name
     * @return The server name
     */
    public String getServerName() {
        return "Minecraft Server";
    }
    
    /**
     * Get the Bukkit version
     * @return The Bukkit version
     */
    public String getBukkitVersion() {
        return Bukkit.getVersion();
    }
}