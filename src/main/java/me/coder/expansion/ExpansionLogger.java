package me.coder.expansion;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class ExpansionLogger {
    
    private final Plugin plugin;
    private final String expansionName;
    
    public ExpansionLogger(Plugin plugin, String expansionName) {
        this.plugin = plugin;
        this.expansionName = expansionName;
    }
    
    public void logInfo(String message) {
        plugin.getLogger().info("[" + expansionName + "] " + message);
    }
    
    public void logWarning(String message) {
        plugin.getLogger().warning("[" + expansionName + "] " + message);
    }
    
    public void logError(String message) {
        plugin.getLogger().severe("[" + expansionName + "] " + message);
    }
    
    public void logDebug(String message) {
        plugin.getLogger().fine("[" + expansionName + "] DEBUG: " + message);
    }
    
    public void sendMessage(CommandSender sender, String message) {
        if (sender != null) {
            sender.sendMessage("§f[" + expansionName + "] §a" + message);
        }
    }
    
    public void sendError(CommandSender sender, String message) {
        if (sender != null) {
            sender.sendMessage("§f[" + expansionName + "] §c" + message);
        }
    }
    
    public void sendWarning(CommandSender sender, String message) {
        if (sender != null) {
            sender.sendMessage("§f[" + expansionName + "] §e" + message);
        }
    }
    
    public void broadcastMessage(String message) {
        Bukkit.broadcastMessage("§f[" + expansionName + "] §a" + message);
    }
    
    public void sendToPlayer(Player player, String message) {
        if (player != null && player.isOnline()) {
            player.sendMessage("§f[" + expansionName + "] §a" + message);
        }
    }
    
    public void sendToPlayerError(Player player, String message) {
        if (player != null && player.isOnline()) {
            player.sendMessage("§f[" + expansionName + "] §c" + message);
        }
    }
    
    public void sendToPlayerWarning(Player player, String message) {
        if (player != null && player.isOnline()) {
            player.sendMessage("§f[" + expansionName + "] §e" + message);
        }
    }
    
    public String getExpansionName() {
        return expansionName;
    }
}