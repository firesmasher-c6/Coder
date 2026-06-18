package me.coder.codedsl.manager;

import me.coder.api.CoderAPI;
import me.coder.codedsl.placeholders.Placeholders;
import org.bukkit.Bukkit;
import java.util.Map;

/**
 * Manages placeholder replacements for CodeDSL scripts
 */
public class PlaceholderManager {

    private final CoderAPI api;
    private final Map<String, String> variables;
    private final Map<String, String> obfuscatedVars;
    private final Placeholders placeholders;

    public PlaceholderManager(CoderAPI api, Map<String, String> variables, Map<String, String> obfuscatedVars) {
        this.api = api;
        this.variables = variables;
        this.obfuscatedVars = obfuscatedVars;
        this.placeholders = new Placeholders();
    }

    /**
     * Replace all placeholders in text
     */
    public String replacePlaceholders(String text) {
        // Replace variable placeholders: {var(name)}
        text = replaceVariablePlaceholders(text);
        
        // Replace obfuscated variable placeholders: {obfvar(name)}
        text = replaceObfuscatedPlaceholders(text);
        
        // Replace server placeholders: {placeholder(...)}
        text = placeholders.replaceServerPlaceholders(text);
        
        return text;
    }

    /**
     * Replace variable placeholders: {var(name)}
     */
    private String replaceVariablePlaceholders(String text) {
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            text = text.replace("{var(" + entry.getKey() + ")}", entry.getValue());
            text = text.replace("{placeholder(var_" + entry.getKey() + ")}", entry.getValue());
        }
        return text;
    }

    /**
     * Replace obfuscated variable placeholders: {obfvar(name)}
     */
    private String replaceObfuscatedPlaceholders(String text) {
        for (Map.Entry<String, String> entry : obfuscatedVars.entrySet()) {
            try {
                String decoded = new String(java.util.Base64.getDecoder().decode(entry.getValue()));
                text = text.replace("{obfvar(" + entry.getKey() + ")}", decoded);
                text = text.replace("{placeholder(obfvar_" + entry.getKey() + ")}", decoded);
            } catch (Exception e) {
                // Ignore decode errors
            }
        }
        return text;
    }
}