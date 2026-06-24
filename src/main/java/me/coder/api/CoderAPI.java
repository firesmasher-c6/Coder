package me.coder.api;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;

// Coder API - Singleton for Addon development
public class CoderAPI {
    
    private static CoderAPI instance;
    
    private CoderAPI() {
    }
    
    // Get singleton instance
    public static CoderAPI getInstance() {
        if (instance == null) {
            instance = new CoderAPI();
        }
        return instance;
    }
    
    // Message Methods
    public void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(message);
    }
    
    public void sendSuccess(CommandSender sender, String message) {
        sender.sendMessage("§a" + message);
    }
    
    public void sendError(CommandSender sender, String message) {
        sender.sendMessage("§c" + message);
    }
    
    public void sendInfo(CommandSender sender, String message) {
        sender.sendMessage("§9" + message);
    }
    
    public void sendWarning(CommandSender sender, String message) {
        sender.sendMessage("§e" + message);
    }
    
    // Player Methods
    public boolean isPlayer(CommandSender sender) {
        return sender instanceof Player;
    }
    
    public Player getPlayer(String name) {
        return Bukkit.getPlayer(name);
    }
    
    public Player[] getOnlinePlayers() {
        return Bukkit.getOnlinePlayers().toArray(new Player[0]);
    }
    
    public int getOnlinePlayerCount() {
        return Bukkit.getOnlinePlayers().size();
    }
    
    // Broadcast to all players
    public void broadcast(String message) {
        Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(message));
        Bukkit.getConsoleSender().sendMessage(message);
    }
    
    // Command Execution
    public boolean executeCommand(String command) {
        try {
            return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        } catch (Exception e) {
            return false;
        }
    }
    
    // Logging Methods
    public void log(String message) {
        Bukkit.getLogger().info("[Coder] " + message);
    }
    
    public void logWarning(String message) {
        Bukkit.getLogger().warning("[Coder] " + message);
    }
    
    public void logError(String message) {
        Bukkit.getLogger().severe("[Coder] " + message);
    }
    
    // Server Info
    public String getServerName() {
        return "Minecraft Server";
    }
    
    public String getBukkitVersion() {
        return Bukkit.getVersion();
    }
}