package me.coder.codedsl.libraries;

import me.coder.api.CoderAPI;
import java.util.*;

/**
 * Registry for CodeDSL libraries and imports
 */
public class LibraryRegistry {

    private final Map<String, CodeDSLLibrary> libraries = new HashMap<>();
    private final CoderAPI api;

    public LibraryRegistry(CoderAPI api) {
        this.api = api;
        registerDefaultLibraries();
    }

    /**
     * Register default CodeDSL libraries
     */
    private void registerDefaultLibraries() {
        registerLibrary("bukkit", new BukkitLibrary());
        registerLibrary("async", new AsyncLibrary());
        registerLibrary("connection", new ConnectionLibrary());
    }

    /**
     * Register a custom library
     */
    public void registerLibrary(String name, CodeDSLLibrary library) {
        libraries.put(name.toLowerCase(), library);
        api.log("[CodeDSL] Library registered: " + name);
    }

    /**
     * Get a library by name
     */
    public CodeDSLLibrary getLibrary(String name) {
        return libraries.get(name.toLowerCase());
    }

    /**
     * Check if library is registered
     */
    public boolean hasLibrary(String name) {
        return libraries.containsKey(name.toLowerCase());
    }

    /**
     * Get all registered libraries
     */
    public Collection<CodeDSLLibrary> getLibraries() {
        return libraries.values();
    }

    /**
     * Library interface
     */
    public interface CodeDSLLibrary {
        String getName();
        String getVersion();
        void onLoad();
        void onUnload();
    }
}