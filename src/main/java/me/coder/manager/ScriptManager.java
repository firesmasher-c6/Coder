package me.coder.manager;

import me.coder.CoderPlugin;
import org.bukkit.Bukkit;
import net.kyori.adventure.text.Component; // Ensure you use Adventure API for Paper/Spigot 1.16+
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class ScriptManager {
    private final CoderPlugin plugin;

    public ScriptManager(CoderPlugin plugin) { this.plugin = plugin; }

    // 1. PYTHON PARSER (With Exception Handling)
    public void runPython(String fileName) {
        File file = new File(plugin.getDataFolder(), "Python/scripts/" + fileName);
        if (!file.exists()) return;
        try {
            new ProcessBuilder("python3", file.getAbsolutePath()).start();
        } catch (IOException e) {
            plugin.getLogger().severe("Could not execute Python script: " + fileName);
            e.printStackTrace();
        }
    }

    // 2. JAVA PARSER (Dynamic Compiler)
    public void runJava(String fileName) {
        File file = new File(plugin.getDataFolder(), "Java/scripts/" + fileName);
        if (ToolProvider.getSystemJavaCompiler() != null) {
            ToolProvider.getSystemJavaCompiler().run(null, null, null, file.getPath());
            plugin.getLogger().info("Compiled Java: " + fileName);
        } else {
            plugin.getLogger().severe("Java Compiler not found! Ensure you are running on a JDK, not a JRE.");
        }
    }

    // 3. CUSTOM SYNTAX PARSER
    public void runCustomScript(String fileName) {
        File file = new File(plugin.getDataFolder(), "scripts/" + fileName);
        try {
            List<String> lines = Files.readAllLines(file.toPath());
            for (String line : lines) {
                line = line.trim();
                
                if (line.startsWith("print(")) {
                    String msg = line.substring(line.indexOf("\"")+1, line.lastIndexOf("\""));
                    // Use modern Adventure API for broadcasting
                    Bukkit.broadcast(Component.text(msg)); 
                } 
                else if (line.startsWith("cmd /")) {
                    String cmdName = line.substring(5);
                    plugin.getLogger().info("Custom command detected: " + cmdName);
                }
            }
        } catch (IOException e) { 
            plugin.getLogger().severe("Error reading script: " + fileName);
        }
    }

    public void reloadCustomScript(String fileName) {
        plugin.getLogger().info("Reloading custom script: " + fileName);
    }
}