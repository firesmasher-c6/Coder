package me.coder.codedsl.syntaxes;

import me.coder.api.CoderAPI;
import java.util.*;
import java.util.regex.*;

/**
 * Validates CodeDSL syntax before execution
 */
public class SyntaxValidator {

    private final CoderAPI api;
    private final Map<String, Pattern> syntaxPatterns = new HashMap<>();

    public SyntaxValidator(CoderAPI api) {
        this.api = api;
        initializeSyntaxPatterns();
    }

    /**
     * Initialize all syntax patterns
     */
    private void initializeSyntaxPatterns() {
        // Variable: var name = value
        syntaxPatterns.put("variable", Pattern.compile("^var\\s+([a-zA-Z0-9_]+)\\s*=\\s*(.+)$"));
        
        // Obfuscated Variable: obfuscatedVAR name = value
        syntaxPatterns.put("obfvar", Pattern.compile("^obfuscatedVAR\\s+([a-zA-Z0-9_]+)\\s*=\\s*(.+)$"));
        
        // Broadcast: broadcast "message"
        syntaxPatterns.put("broadcast", Pattern.compile("^broadcast\\s+\"(.*)\"$"));
        
        // Send: send "message" to player_name
        syntaxPatterns.put("send", Pattern.compile("^send\\s+\"(.*)\"\\s+to\\s+(.+)$"));
        
        // Console log: console.log "message" or console.log { ... }
        syntaxPatterns.put("console_log", Pattern.compile("^console\\.log\\s+(.*)$"));
        
        // File Read: fileRead /path/to/file
        syntaxPatterns.put("file_read", Pattern.compile("^fileRead\\s+(.+)$"));
        
        // File Write: fileWrite /path/to/file { ... }
        syntaxPatterns.put("file_write", Pattern.compile("^fileWrite\\s+([^\\{]+)\\{(.*)\\}$", Pattern.DOTALL));
        
        // File Delete: fileDel /path/to/file
        syntaxPatterns.put("file_del", Pattern.compile("^fileDel\\s+(.+)$"));
        
        // Variable Read: varRead "name"
        syntaxPatterns.put("var_read", Pattern.compile("^varRead\\s+\"([a-zA-Z0-9_]+)\"$"));
        
        // Delete Variable: deleteVar "name"
        syntaxPatterns.put("delete_var", Pattern.compile("^deleteVar\\s+\"([a-zA-Z0-9_]+)\"$"));
        
        // Delete Obfuscated Var: deleteObfVar "name"
        syntaxPatterns.put("delete_obfvar", Pattern.compile("^deleteObfVar\\s+\"([a-zA-Z0-9_]+)\"$"));
        
        // Wait: wait 1s, wait 5m, wait 10t
        syntaxPatterns.put("wait", Pattern.compile("^wait\\s+(\\d+)([smth])$"));
        
        // Delay: delay 20 (in ticks)
        syntaxPatterns.put("delay", Pattern.compile("^delay\\s+(\\d+)$"));
        
        // Execute Command: execute command "cmd" in console/player
        syntaxPatterns.put("execute", Pattern.compile("^execute\\s+command\\s+\"(.*)\"\\s+in\\s+(console|player)$"));
        
        // If Statement: if var "name" = value:
        syntaxPatterns.put("if_statement", Pattern.compile("^if\\s+var\\s+\"([a-zA-Z0-9_]+)\"\\s*=\\s*(.+):$"));
        
        // Command: command /name [<args>]:
        syntaxPatterns.put("command", Pattern.compile("^command\\s+(/\\S+).*:$"));
        
        // Every: every 5s { ... }
        syntaxPatterns.put("every", Pattern.compile("^every\\s+(\\d+)([smh])\\s*\\{(.*)\\}", Pattern.DOTALL));
        
        // Import: import async, import Connection, etc
        syntaxPatterns.put("import", Pattern.compile("^import\\s+(async|Connection|bukkit|[a-zA-Z0-9_]+)$"));
    }

    /**
     * Validate a single line of CodeDSL
     */
    public SyntaxResult validateLine(String line) {
        line = line.trim();

        // Skip empty lines and comments
        if (line.isEmpty() || line.startsWith("#")) {
            return new SyntaxResult(true, "COMMENT", null);
        }

        // Check against all syntax patterns
        for (Map.Entry<String, Pattern> entry : syntaxPatterns.entrySet()) {
            Pattern pattern = entry.getValue();
            Matcher matcher = pattern.matcher(line);
            
            if (matcher.find()) {
                return new SyntaxResult(true, entry.getKey(), matcher);
            }
        }

        // Unknown syntax
        return new SyntaxResult(false, "UNKNOWN", null, "Unknown CodeDSL syntax: " + line);
    }

    /**
     * Syntax validation result
     */
    public static class SyntaxResult {
        public boolean valid;
        public String syntaxType;
        public Matcher matcher;
        public String errorMessage;

        public SyntaxResult(boolean valid, String syntaxType, Matcher matcher) {
            this.valid = valid;
            this.syntaxType = syntaxType;
            this.matcher = matcher;
            this.errorMessage = null;
        }

        public SyntaxResult(boolean valid, String syntaxType, Matcher matcher, String errorMessage) {
            this(valid, syntaxType, matcher);
            this.errorMessage = errorMessage;
        }
    }
}