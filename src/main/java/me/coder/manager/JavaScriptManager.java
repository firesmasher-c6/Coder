package me.coder.manager;

import org.bukkit.plugin.java.JavaPlugin;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

public class JavaScriptManager {
    private final JavaPlugin plugin;
    private final Set<String> blockedPatterns;
    private final Set<String> blockedImports;
    private final Set<String> dangerousAPICalls;

    public JavaScriptManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.blockedPatterns = new HashSet<>();
        this.blockedImports = new HashSet<>();
        this.dangerousAPICalls = new HashSet<>();
        
        initializeSecurityPatterns();
    }

    /**
     * Initialize security patterns and blocked items
     */
    private void initializeSecurityPatterns() {
        // Blocked imports
        blockedImports.addAll(Arrays.asList(
            "java.io.RandomAccessFile",
            "java.nio.file.Files",
            "java.lang.Runtime",
            "java.lang.ProcessBuilder",
            "java.lang.System",
            "java.lang.reflect.Method",
            "java.lang.reflect.Field",
            "java.net.Socket",
            "java.net.ServerSocket"
        ));

        // Blocked API patterns
        dangerousAPICalls.addAll(Arrays.asList(
            "Runtime.getRuntime()",
            "ProcessBuilder",
            "System.exit",
            "System.setProperty",
            "System.getenv",
            "File.delete",
            "File.deleteOnExit",
            "RandomAccessFile"
        ));

        // Regex patterns for code obfuscation detection
        blockedPatterns.add(".*\\beval\\b.*");
        blockedPatterns.add(".*\\bgetDeclaredMethod\\b.*");
        blockedPatterns.add(".*\\bsetAccessible\\b.*");
        blockedPatterns.add(".*\\binvoke\\b.*");
    }

    /**
     * Scan a Java script line by line and validate it
     * Returns true if script is safe to execute
     */
    public boolean validateScript(File scriptFile) throws IOException {
        List<String> lines = readScriptLines(scriptFile);
        return analyzeScript(lines);
    }

    /**
     * Read script file into lines
     */
    private List<String> readScriptLines(File scriptFile) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(scriptFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line.trim());
            }
        }
        return lines;
    }

    /**
     * Analyze script for dangerous patterns
     */
    private boolean analyzeScript(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);

            // Skip comments
            if (line.startsWith("//") || line.startsWith("*")) {
                continue;
            }

            // Check for blocked imports
            if (line.contains("import ")) {
                if (containsBlockedImport(line)) {
                    plugin.getLogger().warning("Blocked import detected at line " + (i + 1) + ": " + line);
                    return false;
                }
            }

            // Check for dangerous API calls
            if (containsDangerousAPI(line)) {
                plugin.getLogger().warning("Dangerous API call detected at line " + (i + 1) + ": " + line);
                return false;
            }

            // Check for obfuscation patterns
            if (containsObfuscationPattern(line)) {
                plugin.getLogger().warning("Potential obfuscation pattern detected at line " + (i + 1) + ": " + line);
                return false;
            }

            // Check for string concatenation bypass attempts (basic detection)
            if (detectConcatenationBypass(line)) {
                plugin.getLogger().warning("Potential bypass attempt via concatenation at line " + (i + 1) + ": " + line);
                return false;
            }
        }

        return true;
    }

    /**
     * Check if line contains blocked imports
     */
    private boolean containsBlockedImport(String line) {
        for (String blockedImport : blockedImports) {
            if (line.contains(blockedImport)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check for dangerous API calls
     */
    private boolean containsDangerousAPI(String line) {
        for (String apiCall : dangerousAPICalls) {
            if (line.contains(apiCall)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check for obfuscation patterns using regex
     */
    private boolean containsObfuscationPattern(String line) {
        for (String pattern : blockedPatterns) {
            if (Pattern.matches(pattern, line)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Detect attempts to bypass restrictions via string concatenation
     * Example: "Sys" + "tem.exit" should be detected
     */
    private boolean detectConcatenationBypass(String line) {
        // Check for suspicious string concatenation patterns
        if (line.contains("\"") && line.contains("+") && line.contains("tem")) {
            return true; // Simple heuristic for "System" bypass
        }

        if (line.contains("\"") && line.contains("+") && line.contains("Runtime")) {
            return true;
        }

        if (line.contains("\"") && line.contains("+") && line.contains("ProcessBuilder")) {
            return true;
        }

        // Check for getClass() bypass patterns
        if (line.contains("getClass()") && line.contains("getMethod")) {
            return true;
        }

        return false;
    }

    /**
     * Get a summary of why a script was blocked
     */
    public String getBlockReason(File scriptFile) {
        try {
            List<String> lines = readScriptLines(scriptFile);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);

                if (line.contains("import ") && containsBlockedImport(line)) {
                    return "Blocked import at line " + (i + 1) + ": " + line;
                }

                if (containsDangerousAPI(line)) {
                    return "Dangerous API call at line " + (i + 1) + ": " + line;
                }

                if (containsObfuscationPattern(line)) {
                    return "Obfuscation pattern at line " + (i + 1) + ": " + line;
                }
            }
        } catch (IOException e) {
            return "Error reading script: " + e.getMessage();
        }

        return "Unknown reason";
    }
}