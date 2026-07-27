package me.coder.api;

import dev.codestuff.coder.api.CoderAPI;
import dev.codestuff.coder.api.CoderEventListener;
import dev.codestuff.coder.api.CoderTabCompleter;
import dev.codestuff.coder.api.JavaExecutionHandler;
import dev.codestuff.coder.api.ScriptExecutionHandler;
import dev.codestuff.coder.api.ScriptPostprocessor;
import dev.codestuff.coder.api.ScriptPreprocessor;

import org.bukkit.command.CommandSender;
import java.util.Map;

/**
 * Legacy shim for me.coder.api.CoderAddon.
 * Addons implementing this interface will compile and run correctly —
 * all CoderAPI references use the real dev.codestuff.coder.api.CoderAPI.
 *
 * @deprecated Implement {@link dev.codestuff.coder.api.CoderAddon} instead.
 */
@Deprecated
public interface CoderAddon {

    String getName();
    String getVersion();
    String getAuthor();
    void onEnable();
    void onDisable();

    default String getDescription()          { return "A Coder addon"; }
    default String getWebsite()              { return "https://example.com"; }
    default String[] getAuthors()            { return new String[]{getAuthor()}; }
    default void onReload()                  { onDisable(); onEnable(); }
    default boolean supportsConfiguration()  { return false; }
    default boolean onAddonCommand(CommandSender sender, String[] args) { return false; }
    default String getHelpText()             { return "No help available for this addon."; }
    default boolean isCompatible(String v)   { return true; }
    default int getPriority()                { return 50; }
    default boolean canBeReloaded()          { return true; }
    default String[] getDependencies()       { return new String[]{}; }
    default void onAllAddonsLoaded()         {}
    default void onBeforeDisable()           {}
    default int getAPIVersion()              { return 2; }

    default void registerCustomCommands(CoderAPI api)   {}
    default void unregisterCustomCommands(CoderAPI api) {}

    default JavaExecutionHandler   getCustomJavaHandler()              { return null; }
    default ScriptExecutionHandler getCustomPythonHandler()            { return null; }
    default ScriptExecutionHandler getCustomLuaHandler()               { return null; }
    default ScriptPreprocessor     getScriptPreprocessor(String lang)  { return null; }
    default ScriptPostprocessor    getScriptPostprocessor(String lang) { return null; }
    default CoderEventListener     getEventListener()                  { return null; }
    default CoderTabCompleter      getTabCompleter()                   { return null; }

    default Map<String, Object> getConfigDefaults()    { return new java.util.HashMap<>(); }
    default boolean shouldAutoSave()                   { return true; }
    default int getAutoSaveInterval()                  { return 300; }
    default void onSave()                              {}
    default void onLoad()                              {}
    default void onTick()                              {}
    default Map<String, String> getAddonPermissions()  { return new java.util.HashMap<>(); }
    default void onAddonLoaded(String addonName)       {}
    default void onAddonUnloaded(String addonName)     {}
    default boolean hasFeature(String featureName)     { return false; }
    default Object getFeature(String featureName)      { return null; }
    default Map<String, Object> getScriptGlobals()     { return new java.util.HashMap<>(); }
    default boolean modifiesCommands()                 { return false; }
    default boolean modifiesExecution()                { return false; }

    default Map<String, String> getMetadata() {
        Map<String, String> metadata = new java.util.HashMap<>();
        metadata.put("name",        getName());
        metadata.put("version",     getVersion());
        metadata.put("author",      getAuthor());
        metadata.put("description", getDescription());
        return metadata;
    }
}