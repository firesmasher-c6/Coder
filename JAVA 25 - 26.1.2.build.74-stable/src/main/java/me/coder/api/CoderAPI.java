package me.coder.api;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import dev.codestuff.coder.api.CoderCommandHandler;
import dev.codestuff.coder.api.CoderEventListener;
import dev.codestuff.coder.api.CoderTabCompleter;
import dev.codestuff.coder.api.JavaExecutionHandler;
import dev.codestuff.coder.api.ScriptExecutionHandler;
import dev.codestuff.coder.api.ScriptPostprocessor;
import dev.codestuff.coder.api.ScriptPreprocessor;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Legacy API shim for me.coder.api.
 *
 * This class exists solely for backwards compatibility with addons that were
 * compiled against the old "me.coder.api" package path. Every method delegates
 * directly to {@link dev.codestuff.coder.api.CoderAPI} — no logic lives here.
 *
 * @deprecated Use {@link dev.codestuff.coder.api.CoderAPI} instead.
 */
@Deprecated
public class CoderAPI {

    private static CoderAPI instance;

    private CoderAPI() {}

    public static CoderAPI getInstance() {
        if (instance == null) instance = new CoderAPI();
        return instance;
    }

    private dev.codestuff.coder.api.CoderAPI real() {
        return dev.codestuff.coder.api.CoderAPI.getInstance();
    }

    // ── Messages ──────────────────────────────────────────────────────────────

    public void sendMessage(CommandSender sender, String message)  { real().sendMessage(sender, message); }
    public void sendSuccess(CommandSender sender, String message)  { real().sendSuccess(sender, message); }
    public void sendError(CommandSender sender, String message)    { real().sendError(sender, message); }
    public void sendInfo(CommandSender sender, String message)     { real().sendInfo(sender, message); }
    public void sendWarning(CommandSender sender, String message)  { real().sendWarning(sender, message); }
    public void sendDebug(CommandSender sender, String message)    { real().sendDebug(sender, message); }
    public void sendRaw(CommandSender sender, String message)      { real().sendRaw(sender, message); }
    public void broadcastMessage(String message)                   { real().broadcastMessage(message); }

    // ── Players ───────────────────────────────────────────────────────────────

    public boolean isPlayer(CommandSender sender)                  { return real().isPlayer(sender); }
    public Player  getPlayer(String name)                          { return real().getPlayer(name); }
    public Player  getPlayerByUUID(UUID uuid)                      { return real().getPlayerByUUID(uuid); }
    public Player[] getOnlinePlayers()                             { return real().getOnlinePlayers(); }
    public int     getOnlinePlayerCount()                          { return real().getOnlinePlayerCount(); }
    public void    teleportPlayer(Player player, Location loc)     { real().teleportPlayer(player, loc); }
    public void    damagePlayer(Player player, double damage)      { real().damagePlayer(player, damage); }
    public void    healPlayer(Player player, double amount)        { real().healPlayer(player, amount); }
    public void    setPlayerHealth(Player player, double health)   { real().setPlayerHealth(player, health); }
    public double  getPlayerHealth(Player player)                  { return real().getPlayerHealth(player); }

    // ── Worlds ────────────────────────────────────────────────────────────────

    public World   getWorld(String name)                           { return real().getWorld(name); }
    public World[] getWorlds()                                     { return real().getWorlds(); }
    public String[] getWorldNames()                                { return real().getWorldNames(); }

    // ── Command execution ─────────────────────────────────────────────────────

    public boolean executeCommand(String command)                          { return real().executeCommand(command); }
    public boolean executeCommandAsPlayer(Player player, String command)   { return real().executeCommandAsPlayer(player, command); }
    public boolean executeCommandAsConsole(String command)                 { return real().executeCommandAsConsole(command); }

    // ── Logging ───────────────────────────────────────────────────────────────

    public void log(String message)        { real().log(message); }
    public void logWarning(String message) { real().logWarning(message); }
    public void logError(String message)   { real().logError(message); }
    public void logDebug(String message)   { real().logDebug(message); }

    // ── Server info ───────────────────────────────────────────────────────────

    public String  getServerMotd()         { return real().getServerMotd(); }
    public String  getBukkitVersion()      { return real().getBukkitVersion(); }
    public String  getMinecraftVersion()   { return real().getMinecraftVersion(); }
    public int     getMaxPlayers()         { return real().getMaxPlayers(); }
    public boolean isServerRunning()       { return real().isServerRunning(); }
    public long    getServerTicks()        { return real().getServerTicks(); }

    // ── Plugins ───────────────────────────────────────────────────────────────

    public Plugin  getPlugin(String name)          { return real().getPlugin(name); }
    public Plugin[] getPlugins()                   { return real().getPlugins(); }
    public boolean isPluginEnabled(String name)    { return real().isPluginEnabled(name); }

    // ── Broadcast ─────────────────────────────────────────────────────────────

    public void broadcast(String message)        { real().broadcast(message); }
    public void broadcastSuccess(String message) { real().broadcastSuccess(message); }
    public void broadcastWarning(String message) { real().broadcastWarning(message); }
    public void broadcastError(String message)   { real().broadcastError(message); }

    // ── Engine integration ────────────────────────────────────────────────────

    public void registerCoderCommand(String subcommand, CoderCommandHandler handler) { real().registerCoderCommand(subcommand, handler); }
    public void unregisterCoderCommand(String subcommand)                            { real().unregisterCoderCommand(subcommand); }
    public Set<String> getRegisteredCoderCommands()                                  { return real().getRegisteredCoderCommands(); }
    public CoderCommandHandler getCoderCommand(String subcommand)                    { return real().getCoderCommand(subcommand); }

    public void registerJavaHandler(JavaExecutionHandler handler)       { real().registerJavaHandler(handler); }
    public void registerPythonHandler(ScriptExecutionHandler handler)   { real().registerPythonHandler(handler); }
    public void registerLuaHandler(ScriptExecutionHandler handler)      { real().registerLuaHandler(handler); }
    public JavaExecutionHandler    getJavaHandler()                     { return real().getJavaHandler(); }
    public ScriptExecutionHandler  getPythonHandler()                   { return real().getPythonHandler(); }
    public ScriptExecutionHandler  getLuaHandler()                      { return real().getLuaHandler(); }

    public void registerPreprocessor(String language, ScriptPreprocessor preprocessor)   { real().registerPreprocessor(language, preprocessor); }
    public void registerPostprocessor(String language, ScriptPostprocessor postprocessor){ real().registerPostprocessor(language, postprocessor); }
    public ScriptPreprocessor  getPreprocessor(String language)                          { return real().getPreprocessor(language); }
    public ScriptPostprocessor getPostprocessor(String language)                         { return real().getPostprocessor(language); }

    public void registerEventListener(CoderEventListener listener)   { real().registerEventListener(listener); }
    public void unregisterEventListener(CoderEventListener listener) { real().unregisterEventListener(listener); }
    public List<CoderEventListener> getEventListeners()              { return real().getEventListeners(); }

    public void registerTabCompleter(CoderTabCompleter completer)    { real().registerTabCompleter(completer); }
    public List<CoderTabCompleter>  getTabCompleters()               { return real().getTabCompleters(); }
}