package dev.codestuff.coder.api;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public interface CoderAddon {

    String getName();
    String getVersion();
    String getAuthor();
    void onEnable();
    void onDisable();

    default String getDescription() { return "A Coder addon"; }
    default String getWebsite() { return "https://myurlexample.com"; }
    default String[] getAuthors() { return new String[]{getAuthor()}; }
    default String[] getDependencies() { return new String[]{}; }
    default String getMinCoderVersion() { return null; }
    default int getAPIVersion() { return 2; }
    default int getPriority() { return 50; }
    default boolean canBeReloaded() { return true; }
    default boolean supportsConfiguration() { return false; }
    default boolean modifiesCommands() { return false; }
    default boolean modifiesExecution() { return false; }
    default boolean hasFeature(String featureName) { return false; }
    default Object getFeature(String featureName) { return null; }

    default Map<String, String> getMetadata() {
        Map<String, String> metadata = new java.util.HashMap<>();
        metadata.put("name", getName());
        metadata.put("version", getVersion());
        metadata.put("author", getAuthor());
        metadata.put("description", getDescription());
        return metadata;
    }

    default void onReload() { onDisable(); onEnable(); }
    default void onAllAddonsLoaded() {}
    default void onBeforeDisable() {}
    default void onLoad() {}
    default void onSave() {}

    default boolean wantsTick() { return false; }
    default void onTick() {}

    default void onAddonLoaded(String addonName) {}
    default void onAddonUnloaded(String addonName) {}

    default boolean onScriptRun(String scriptName, String language) { return true; }
    default List<String> getScriptInjections(String language) { return null; }

    default ScriptPreprocessor getScriptPreprocessor(String language) { return null; }
    default ScriptPostprocessor getScriptPostprocessor(String language) { return null; }

    default void onPlayerJoin(Player player) {}
    default void onPlayerLeave(Player player) {}

    default void registerCustomCommands(CoderAPI api) {}
    default void unregisterCustomCommands(CoderAPI api) {}
    default boolean onAddonCommand(CommandSender sender, String[] args) { return false; }
    default String getHelpText() { return "No help available for this addon."; }

    default JavaExecutionHandler getCustomJavaHandler() { return null; }
    default ScriptExecutionHandler getCustomPythonHandler() { return null; }
    default ScriptExecutionHandler getCustomLuaHandler() { return null; }
    default CoderEventListener getEventListener() { return null; }
    default CoderTabCompleter getTabCompleter() { return null; }

    default boolean isCompatible(String pluginVersion) { return true; }

    default Map<String, Object> getConfigDefaults() { return new java.util.HashMap<>(); }
    default boolean shouldAutoSave() { return false; }
    default int getAutoSaveInterval() { return 300; }

    default Map<String, String> getAddonPermissions() { return new java.util.HashMap<>(); }
    default Map<String, Object> getScriptGlobals() { return new java.util.HashMap<>(); }
}