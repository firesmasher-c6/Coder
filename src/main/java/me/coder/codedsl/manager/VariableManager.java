package me.coder.codedsl.manager;

import me.coder.api.CoderAPI;
import java.io.File;
import java.nio.file.Files;
import java.util.*;

/**
 * Manages variable storage and persistence for CodeDSL
 * Handles both plain and obfuscated variables
 */
public class VariableManager {

    private final File variablesFolder;
    private final File variablesStorageFile;
    private final File variablesObfFile;
    private final CoderAPI api;
    
    private final Map<String, String> variables = new HashMap<>();
    private final Map<String, String> obfuscatedVariables = new HashMap<>();

    public VariableManager(File dataFolder, CoderAPI api) {
        this.variablesFolder = new File(dataFolder, "variables");
        this.variablesStorageFile = new File(variablesFolder, "variables.storage");
        this.variablesObfFile = new File(variablesFolder, "variables.obf");
        this.api = api;

        if (!variablesFolder.exists()) {
            variablesFolder.mkdirs();
        }

        loadVariables();
    }

    /**
     * Store a plain variable
     */
    public void setVariable(String name, String value) {
        variables.put(name, value);
        saveVariables();
        api.log("[CodeDSL] Variable stored: " + name + " = " + value);
    }

    /**
     * Get a plain variable
     */
    public String getVariable(String name) {
        return variables.get(name);
    }

    /**
     * Check if plain variable exists
     */
    public boolean hasVariable(String name) {
        return variables.containsKey(name);
    }

    /**
     * Delete a plain variable
     */
    public void deleteVariable(String name) {
        if (variables.remove(name) != null) {
            saveVariables();
            api.log("[CodeDSL] Variable deleted: " + name);
        }
    }

    /**
     * Store an obfuscated variable (Base64 encoded)
     */
    public void setObfuscatedVariable(String name, String value) {
        String encoded = Base64.getEncoder().encodeToString(value.getBytes());
        obfuscatedVariables.put(name, encoded);
        saveObfuscatedVariables();
        api.log("[CodeDSL] Obfuscated variable stored: " + name);
    }

    /**
     * Get an obfuscated variable (decoded)
     */
    public String getObfuscatedVariable(String name) {
        String encoded = obfuscatedVariables.get(name);
        if (encoded != null) {
            try {
                return new String(Base64.getDecoder().decode(encoded));
            } catch (IllegalArgumentException e) {
                api.logError("Failed to decode obfuscated variable: " + name);
                return null;
            }
        }
        return null;
    }

    /**
     * Check if obfuscated variable exists
     */
    public boolean hasObfuscatedVariable(String name) {
        return obfuscatedVariables.containsKey(name);
    }

    /**
     * Delete an obfuscated variable
     */
    public void deleteObfuscatedVariable(String name) {
        if (obfuscatedVariables.remove(name) != null) {
            saveObfuscatedVariables();
            api.log("[CodeDSL] Obfuscated variable deleted: " + name);
        }
    }

    /**
     * Load all variables from storage files
     */
    private void loadVariables() {
        // Load plain variables
        if (variablesStorageFile.exists()) {
            try {
                List<String> lines = Files.readAllLines(variablesStorageFile.toPath());
                for (String line : lines) {
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        variables.put(parts[0].trim(), parts[1].trim());
                    }
                }
                api.log("[CodeDSL] Loaded " + variables.size() + " plain variables");
            } catch (Exception e) {
                api.logError("Error loading plain variables: " + e.getMessage());
            }
        }

        // Load obfuscated variables
        if (variablesObfFile.exists()) {
            try {
                List<String> lines = Files.readAllLines(variablesObfFile.toPath());
                for (String line : lines) {
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        obfuscatedVariables.put(parts[0].trim(), parts[1].trim());
                    }
                }
                api.log("[CodeDSL] Loaded " + obfuscatedVariables.size() + " obfuscated variables");
            } catch (Exception e) {
                api.logError("Error loading obfuscated variables: " + e.getMessage());
            }
        }
    }

    /**
     * Save all plain variables to storage file
     */
    private void saveVariables() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("# CodeDSL Plain Variables Storage\n");
            sb.append("# Format: name=value\n\n");
            
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                sb.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
            }
            
            Files.write(variablesStorageFile.toPath(), sb.toString().getBytes());
        } catch (Exception e) {
            api.logError("Error saving plain variables: " + e.getMessage());
        }
    }

    /**
     * Save all obfuscated variables to storage file
     */
    private void saveObfuscatedVariables() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("# CodeDSL Obfuscated Variables Storage (Base64)\n");
            sb.append("# Format: name=encoded_value\n\n");
            
            for (Map.Entry<String, String> entry : obfuscatedVariables.entrySet()) {
                sb.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
            }
            
            Files.write(variablesObfFile.toPath(), sb.toString().getBytes());
        } catch (Exception e) {
            api.logError("Error saving obfuscated variables: " + e.getMessage());
        }
    }

    /**
     * Replace variable placeholders in text: {var(name)} or {obfvar(name)}
     */
    public String replacePlaceholders(String text) {
        // Replace plain variables: {var(name)}
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            text = text.replace("{var(" + entry.getKey() + ")}", entry.getValue());
        }

        // Replace obfuscated variables: {obfvar(name)}
        for (String name : obfuscatedVariables.keySet()) {
            String decoded = getObfuscatedVariable(name);
            if (decoded != null) {
                text = text.replace("{obfvar(" + name + ")}", decoded);
            }
        }

        return text;
    }

    /**
     * Get all variables
     */
    public Map<String, String> getAllVariables() {
        return new HashMap<>(variables);
    }

    /**
     * Clear all variables
     */
    public void clearAll() {
        variables.clear();
        obfuscatedVariables.clear();
        saveVariables();
        saveObfuscatedVariables();
        api.log("[CodeDSL] All variables cleared");
    }
}