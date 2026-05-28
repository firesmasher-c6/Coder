package me.coder.manager;

import me.coder.CoderPlugin;
import org.bukkit.Bukkit;
import net.kyori.adventure.text.Component;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class ScriptManager {
    private final CoderPlugin plugin;

    public ScriptManager(CoderPlugin plugin) { this.plugin = plugin; }

    public void runScript(String fileName) {
        File file = new File(plugin.getDataFolder(), "scripts/" + fileName);
        if (!file.exists()) {
            plugin.getLogger().warning("Script not found: " + fileName);
            return;
        }

        if (fileName.endsWith(".py")) executeProcess("python3", file);
        else if (fileName.endsWith(".lua")) executeProcess("lua", file);
        else if (fileName.endsWith(".c")) compileAndRun("gcc", file, "c");
        else if (fileName.endsWith(".cpp")) compileAndRun("g++", file, "cpp");
        else if (fileName.endsWith(".java")) compileJava(file);
        else runCustomSyntax(file);
    }

    private void executeProcess(String command, File file) {
        try {
            new ProcessBuilder(command, file.getAbsolutePath()).start();
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to run " + command + ": " + e.getMessage());
        }
    }

    private void compileAndRun(String compiler, File file, String type) {
        // Compile to a temp binary, then run
        String outputName = file.getAbsolutePath().substring(0, file.getAbsolutePath().lastIndexOf('.'));
        try {
            new ProcessBuilder(compiler, file.getAbsolutePath(), "-o", outputName).start().waitFor();
            new ProcessBuilder(outputName).start();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to compile/run " + type + ": " + e.getMessage());
        }
    }

    private void compileJava(File file) {
        if (ToolProvider.getSystemJavaCompiler() != null) {
            int result = ToolProvider.getSystemJavaCompiler().run(null, null, null, file.getPath());
            if (result == 0) {
                plugin.getLogger().info("Compiled Java: " + file.getName());
            } else {
                plugin.getLogger().severe("Java Compilation Failed!");
            }
        } else {
            plugin.getLogger().severe("JDK not found. Please install a full JDK.");
        }
    }

    private void runCustomSyntax(File file) {
        try {
            List<String> lines = Files.readAllLines(file.toPath());
            for (String line : lines) {
                if (line.startsWith("print(")) {
                    String msg = line.substring(line.indexOf("\"")+1, line.lastIndexOf("\""));
                    Bukkit.broadcast(Component.text(msg));
                }
            }
        } catch (IOException e) { plugin.getLogger().severe("Error: " + e.getMessage()); }
    }

    public void reloadScript(String fileName) {
        plugin.getLogger().info("Reloaded script: " + fileName);
    }
}