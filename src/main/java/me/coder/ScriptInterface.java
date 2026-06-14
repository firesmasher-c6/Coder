package me.coder;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Interface that all Java scripts must implement
 * Provides access to the command sender and basic Bukkit functionality
 */
public interface ScriptInterface {
    /**
     * Called when the script is executed
     * @param sender The CommandSender who ran the script (console or player)
     */
    void run(CommandSender sender);

    /**
     * Check if the sender is a player
     * @param sender The CommandSender to check
     * @return true if sender is a Player, false otherwise
     */
    default boolean isPlayer(CommandSender sender) {
        return sender instanceof Player;
    }

    /**
     * Send a formatted message to the sender
     * @param sender The CommandSender to send to
     * @param message The message to send (supports color codes with §)
     */
    default void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(message);
    }

    /**
     * Send an error message (red colored)
     * @param sender The CommandSender to send to
     * @param message The error message
     */
    default void sendError(CommandSender sender, String message) {
        sender.sendMessage("§c" + message);
    }

    /**
     * Send a success message (green colored)
     * @param sender The CommandSender to send to
     * @param message The success message
     */
    default void sendSuccess(CommandSender sender, String message) {
        sender.sendMessage("§a" + message);
    }

    /**
     * Send an info message (blue colored)
     * @param sender The CommandSender to send to
     * @param message The info message
     */
    default void sendInfo(CommandSender sender, String message) {
        sender.sendMessage("§9" + message);
    }

    /**
     * Send a warning message (yellow colored)
     * @param sender The CommandSender to send to
     * @param message The warning message
     */
    default void sendWarning(CommandSender sender, String message) {
        sender.sendMessage("§e" + message);
    }
}