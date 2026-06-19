package me.coder.listener;

import me.coder.manager.VersionManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Listens to player join events and notifies operators of available updates
 */
public class PlayerJoinListener implements Listener {
    
    private final VersionManager versionManager;
    
    public PlayerJoinListener(VersionManager versionManager) {
        this.versionManager = versionManager;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Check if the player is an operator
        if (player.isOp()) {
            // Notify them if an update is available
            versionManager.notifyOperatorIfUpdateAvailable(player);
        }
    }
}