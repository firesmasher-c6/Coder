package me.coder.codedsl.placeholders;

import org.bukkit.Bukkit;

/**
 * Handles all server-side placeholders for CodeDSL
 * Replaces {placeholder(...)} tags with server information
 */
public class Placeholders {

    /**
     * Replace all server placeholders
     */
    public String replaceServerPlaceholders(String text) {
        // Time placeholder
        if (text.contains("{placeholder(time)}")) {
            org.bukkit.World world = Bukkit.getWorld("world");
            if (world != null) {
                text = text.replace("{placeholder(time)}", String.valueOf(world.getTime()));
            }
        }
        
        // Online players
        if (text.contains("{placeholder(online)}")) {
            text = text.replace("{placeholder(online)}", String.valueOf(Bukkit.getOnlinePlayers().size()));
        }
        
        // Max players
        if (text.contains("{placeholder(online_max)}")) {
            text = text.replace("{placeholder(online_max)}", String.valueOf(Bukkit.getMaxPlayers()));
        }
        
        // World online players
        if (text.contains("{placeholder(worldonline)}")) {
            text = text.replace("{placeholder(worldonline)}", String.valueOf(Bukkit.getOnlinePlayers().size()));
        }
        
        // Server uptime
        if (text.contains("{placeholder(server_uptime)}")) {
            text = text.replace("{placeholder(server_uptime)}", getServerUptime());
        }
        
        // MSPT
        if (text.contains("{placeholder(mspt)}")) {
            text = text.replace("{placeholder(mspt)}", String.format("%.2f", getMSPT()));
        }
        
        // TPS
        if (text.contains("{placeholder(tps)}")) {
            text = text.replace("{placeholder(tps)}", String.format("%.2f", getTPS()));
        }
        
        // Loaded plugins
        if (text.contains("{placeholder(plugins)}")) {
            text = text.replace("{placeholder(plugins)}", getLoadedPlugins());
        }
        
        return text;
    }

    /**
     * Get server uptime in human-readable format
     */
    private String getServerUptime() {
        long uptime = System.currentTimeMillis() / 1000;
        long days = uptime / 86400;
        long hours = (uptime % 86400) / 3600;
        long minutes = (uptime % 3600) / 60;
        long seconds = uptime % 60;
        
        if (days > 0) {
            return days + "d " + hours + "h " + minutes + "m";
        } else if (hours > 0) {
            return hours + "h " + minutes + "m " + seconds + "s";
        } else if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        } else {
            return seconds + "s";
        }
    }

    /**
     * Get MSPT (milliseconds per tick)
     * Lower is better, should be <50ms for smooth gameplay
     */
    private double getMSPT() {
        // Simplified implementation
        // In a real scenario, you'd track actual tick times
        return 0.0;
    }

    /**
     * Get TPS (ticks per second)
     * Should be 20.0 for normal operation
     */
    private double getTPS() {
        // Simplified implementation
        // In a real scenario, you'd calculate from recent tick times
        return 20.0;
    }

    /**
     * Get loaded plugins list
     */
    private String getLoadedPlugins() {
        StringBuilder plugins = new StringBuilder();
        for (org.bukkit.plugin.Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            if (plugins.length() > 0) plugins.append(", ");
            plugins.append(plugin.getName());
        }
        return plugins.toString();
    }
}