package me.coder.expansion;

import org.bukkit.plugin.Plugin;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipFile;

public class ExpansionManager {
    
    private final Plugin plugin;
    private final File expansionsDir;
    private Map<String, ExpansionContext> loadedExpansions;
    private Set<String> disabledExpansions;
    
    public ExpansionManager(Plugin plugin) {
        this.plugin = plugin;
        this.expansionsDir = new File(plugin.getDataFolder(), "expansions");
        this.loadedExpansions = new HashMap<>();
        this.disabledExpansions = new HashSet<>();
        
        if (!expansionsDir.exists()) {
            expansionsDir.mkdirs();
        }
    }
    
    public void loadAllExpansions() {
        File[] jsZipFiles = expansionsDir.listFiles((dir, name) -> name.endsWith(".jszip"));
        
        if (jsZipFiles == null || jsZipFiles.length == 0) {
            plugin.getLogger().info("[Coder] No expansions found.");
            return;
        }
        
        for (File jsZipFile : jsZipFiles) {
            loadExpansion(jsZipFile);
        }
    }
    
    public void loadExpansion(File jsZipFile) {
        String fileName = jsZipFile.getName();
        String expansionName = fileName.replace(".jszip", "");
        
        if (disabledExpansions.contains(expansionName)) {
            plugin.getLogger().info("[Coder] Expansion '" + expansionName + "' is disabled.");
            return;
        }
        
        ExpansionArchiveValidator.ValidationResult validation = 
            ExpansionArchiveValidator.validateArchive(jsZipFile);
        
        if (!validation.isValid) {
            plugin.getLogger().severe(validation.errorMessage);
            return;
        }
        
        try (ZipFile zipFile = new ZipFile(jsZipFile)) {
            String manifestJson = readZipEntryAsString(zipFile, "META-INF/MANIFEST.JSON");
            readZipEntryAsString(zipFile, "coder-expansion.plug.json");
            
            ExpansionMetadata metadata = new ExpansionMetadata(manifestJson);
            ExpansionJsonLoader jsonLoader = new ExpansionJsonLoader(expansionName);
            ExpansionLogger logger = new ExpansionLogger(plugin, expansionName);
            
            jsonLoader.loadFromArchive(zipFile);
            
            ExpansionContext context = new ExpansionContext(expansionName, metadata, jsonLoader, logger);
            loadedExpansions.put(expansionName, context);
            
            plugin.getLogger().info("[Coder] Loaded expansion: " + metadata.getName() + " v" + metadata.getVersion());
        } catch (IOException e) {
            plugin.getLogger().severe("ERROR: CANNOT LOAD EXPANSION AS IT APPEARS TO BE CORRUPTED! " + e.getMessage());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().severe("ERROR: " + e.getMessage());
        }
    }
    
    public void enableExpansion(String expansionName) {
        if (disabledExpansions.remove(expansionName)) {
            File jsZipFile = new File(expansionsDir, expansionName + ".jszip");
            if (jsZipFile.exists()) {
                loadExpansion(jsZipFile);
            }
        }
    }
    
    public void disableExpansion(String expansionName) {
        disabledExpansions.add(expansionName);
        loadedExpansions.remove(expansionName);
    }
    
    public boolean isExpansionLoaded(String expansionName) {
        return loadedExpansions.containsKey(expansionName);
    }
    
    public boolean isExpansionDisabled(String expansionName) {
        return disabledExpansions.contains(expansionName);
    }
    
    public ExpansionContext getExpansion(String expansionName) {
        return loadedExpansions.get(expansionName);
    }
    
    public Set<String> getLoadedExpansionNames() {
        return new HashSet<>(loadedExpansions.keySet());
    }
    
    public Set<String> getDisabledExpansionNames() {
        return new HashSet<>(disabledExpansions);
    }
    
    public Set<String> getAvailableExpansionNames() {
        Set<String> available = new HashSet<>();
        File[] jsZipFiles = expansionsDir.listFiles((dir, name) -> name.endsWith(".jszip"));
        
        if (jsZipFiles != null) {
            for (File file : jsZipFiles) {
                String name = file.getName().replace(".jszip", "");
                available.add(name);
            }
        }
        
        return available;
    }
    
    public Set<String> getInactiveExpansionNames() {
        Set<String> inactive = getAvailableExpansionNames();
        inactive.removeAll(loadedExpansions.keySet());
        return inactive;
    }
    
    public File getExpansionsDirectory() {
        return expansionsDir;
    }
    
    public void unloadAllExpansions() {
        loadedExpansions.clear();
    }
    
    private String readZipEntryAsString(ZipFile zipFile, String entryName) throws IOException {
        try (InputStream input = zipFile.getInputStream(zipFile.getEntry(entryName));
             BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        }
    }
}