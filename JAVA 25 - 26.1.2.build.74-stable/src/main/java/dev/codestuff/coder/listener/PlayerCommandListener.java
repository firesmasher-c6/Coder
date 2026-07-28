package dev.codestuff.coder.listener;

import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

import dev.codestuff.coder.CoderPlugin;
import dev.codestuff.coder.manager.AddonManager;

public class PlayerCommandListener implements Listener {

    private final CoderPlugin plugin;
    private final AddonManager addonManager;

    public PlayerCommandListener(CoderPlugin plugin, AddonManager addonManager) {
        this.plugin = plugin;
        this.addonManager = addonManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (!event.getPlayer().isOp()) return;
        if (!isPluginsCommand(event.getMessage())) return;

        CommandSender sender = event.getPlayer();
        // Delay by 1 tick so vanilla /pl output prints first
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            addonManager.loadAddons();
            addonManager.sendAddonList(sender);
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onServerCommand(ServerCommandEvent event) {
        // Strip a leading '/' — some console implementations send it, some don't
        String cmd = event.getCommand().trim();
        if (cmd.startsWith("/")) cmd = cmd.substring(1);
        if (!cmd.equalsIgnoreCase("pl")
                && !cmd.equalsIgnoreCase("plugins")
                && !cmd.equalsIgnoreCase("bukkit:pl")
                && !cmd.equalsIgnoreCase("bukkit:plugins")) return;

        CommandSender sender = event.getSender();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            addonManager.loadAddons();
            addonManager.sendAddonList(sender);
        }, 1L);
    }

    private boolean isPluginsCommand(String message) {
        String msg = message.trim();
        return msg.equalsIgnoreCase("/pl")
            || msg.equalsIgnoreCase("/plugins")
            || msg.equalsIgnoreCase("/bukkit:pl")
            || msg.equalsIgnoreCase("/bukkit:plugins");
    }
}