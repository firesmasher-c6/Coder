package me.coder.codedsl.libraries;

import me.coder.api.CoderAPI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scans and processes import statements in CodeDSL scripts
 */
public class CustomImportScanner {

    private final LibraryRegistry libraryRegistry;
    private final CoderAPI api;
    private final Set<String> loadedLibraries = new HashSet<>();

    public CustomImportScanner(LibraryRegistry libraryRegistry, CoderAPI api) {
        this.libraryRegistry = libraryRegistry;
        this.api = api;
    }

    /**
     * Scan script lines for import statements
     */
    public void scanImports(List<String> scriptLines) {
        Pattern importPattern = Pattern.compile("import\\s+([a-zA-Z0-9_]+)");

        for (String line : scriptLines) {
            Matcher matcher = importPattern.matcher(line.trim());
            if (matcher.find()) {
                String libraryName = matcher.group(1).toLowerCase();
                loadLibrary(libraryName);
            }
        }
    }

    /**
     * Load a library by name
     */
    private void loadLibrary(String name) {
        if (loadedLibraries.contains(name)) {
            return; // Already loaded
        }

        if (libraryRegistry.hasLibrary(name)) {
            LibraryRegistry.CodeDSLLibrary lib = libraryRegistry.getLibrary(name);
            lib.onLoad();
            loadedLibraries.add(name);
            api.log("[CodeDSL] Library loaded: " + name);
        } else {
            api.logWarning("[CodeDSL] Unknown library: " + name);
        }
    }

    /**
     * Get loaded libraries
     */
    public Set<String> getLoadedLibraries() {
        return new HashSet<>(loadedLibraries);
    }

    /**
     * Unload all loaded libraries
     */
    public void unloadAll() {
        for (String libName : loadedLibraries) {
            LibraryRegistry.CodeDSLLibrary lib = libraryRegistry.getLibrary(libName);
            if (lib != null) {
                lib.onUnload();
            }
        }
        loadedLibraries.clear();
    }

    /**
     * Check if a library is imported
     */
    public boolean isImported(String name) {
        return loadedLibraries.contains(name.toLowerCase());
    }
}