package me.coder.manager;

import me.coder.CoderPlugin;
import org.bukkit.command.CommandSender;
import org.python.util.PythonInterpreter;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class ScriptManager {
    private final CoderPlugin plugin;
    private final Map<String, Object> loadedScripts = new HashMap<>();
    private final ConfigManager configManager;

    public ScriptManager(CoderPlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }

    public void runScript(String fileName, CommandSender sender) {
        File file = new File(plugin.getDataFolder(), "scripts/" + fileName);

        if (!file.exists()) {
            sender.sendMessage("§cScript not found: " + fileName);
            return;
        }

        // Check if language is enabled
        if (fileName.endsWith(".py") && !configManager.isPythonEnabled()) {
            sender.sendMessage("§c[Coder] Python scripts are disabled in config.yml");
            return;
        }
        if (fileName.endsWith(".lua") && !configManager.isLuaEnabled()) {
            sender.sendMessage("§c[Coder] Lua scripts are disabled in config.yml");
            return;
        }
        if (fileName.endsWith(".java") && !configManager.isJavaEnabled()) {
            sender.sendMessage("§c[Coder] Java scripts are disabled in config.yml");
            return;
        }

        if (UserExecutionControl.hasTerminalCommands(file)) {
            sender.sendMessage("§c§lError: Error T10!");
            logError(new Exception("Terminal command detected in: " + fileName), fileName);
            return;
        }

        if (fileName.endsWith(".java")) {
            List<String> dangerousImports = UserExecutionControl.checkDangerousImports(file);
            if (!dangerousImports.isEmpty()) {
                UserExecutionControl.addPendingScript(sender, fileName, dangerousImports);
                UserExecutionControl.sendExecutionWarning(sender, dangerousImports);
                return;
            }
        }

        runScriptDirect(fileName, sender);
    }

    public void runScriptDirect(String fileName, CommandSender sender) {
        File file = new File(plugin.getDataFolder(), "scripts/" + fileName);

        if (!file.exists()) {
            sender.sendMessage("§cScript not found: " + fileName);
            return;
        }

        // Check if language is enabled (double-check)
        if (fileName.endsWith(".py") && !configManager.isPythonEnabled()) {
            sender.sendMessage("§c[Coder] Python scripts are disabled in config.yml");
            return;
        }
        if (fileName.endsWith(".lua") && !configManager.isLuaEnabled()) {
            sender.sendMessage("§c[Coder] Lua scripts are disabled in config.yml");
            return;
        }
        if (fileName.endsWith(".java") && !configManager.isJavaEnabled()) {
            sender.sendMessage("§c[Coder] Java scripts are disabled in config.yml");
            return;
        }

        try {
            if (fileName.endsWith(".py")) {
                runPython(file, sender);
            } else if (fileName.endsWith(".lua")) {
                runLua(file, sender);
            } else if (fileName.endsWith(".java")) {
                sender.sendMessage("§cUse /coder run <file.java> to execute Java files");
            } else {
                sender.sendMessage("§cUnsupported file type. Supported: .py, .lua, .java");
            }
        } catch (Exception e) {
            logError(e, fileName);
            sender.sendMessage("§cError executing script. Check error logs.");
        }
    }

    private void runPython(File file, CommandSender sender) {
        sender.sendMessage("§d[Coder] Executing Python: " + file.getName());
        try (PythonInterpreter pyInterp = new PythonInterpreter()) {
            OutputStream os = new OutputStream() {
                private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                @Override
                public void write(int b) {
                    if (b == '\n') {
                        String line = buffer.toString();
                        if (!line.isEmpty()) {
                            sender.sendMessage("§7[Python] " + line.trim());
                        }
                        buffer.reset();
                    } else {
                        buffer.write(b);
                    }
                }
            };
            pyInterp.setOut(new PrintStream(os));
            pyInterp.set("sender", sender);
            pyInterp.execfile(new FileInputStream(file));
            sender.sendMessage("§aScript executed successfully!");
        } catch (Exception e) {
            sender.sendMessage("§cPython Error: " + e.getMessage());
            logError(e, file.getName());
        }
    }

    private void runLua(File file, CommandSender sender) {
        sender.sendMessage("§d[Coder] Executing Lua: " + file.getName());
        try {
            Globals globals = JsePlatform.standardGlobals();
            globals.set("print", new VarArgFunction() {
                @Override
                public Varargs invoke(Varargs args) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 1; i <= args.narg(); i++) {
                        sb.append(args.arg(i).tojstring()).append(" ");
                    }
                    sender.sendMessage("§7[Lua] " + sb.toString().trim());
                    return LuaValue.NIL;
                }
            });
            globals.set("sender", org.luaj.vm2.lib.jse.CoerceJavaToLua.coerce(sender));
            globals.loadfile(file.getAbsolutePath()).call();
            sender.sendMessage("§aScript executed successfully!");
        } catch (Exception e) {
            sender.sendMessage("§cLua Error: " + e.getMessage());
            logError(e, file.getName());
        }
    }

    public void reloadScript(String fileName, CommandSender sender) {
        loadedScripts.remove(fileName);
        sender.sendMessage("§d[Coder] Reloaded: " + fileName);
    }

    private void logError(Exception e, String fileName) {
        // Check if error logging is enabled
        if (!configManager.isErrorLoggingEnabled()) {
            return; // Error logging disabled
        }

        try {
            File logsDir = new File(plugin.getDataFolder(), "Logs/Error-Logs");
            if (!logsDir.exists()) {
                if (!logsDir.mkdirs()) {
                    plugin.getLogger().severe("Failed to create error logs directory!");
                    return;
                }
            }
            
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            int errorCount = getErrorLogCount(logsDir) + 1;
            File errorLog = new File(logsDir, "Error-" + timestamp + "-" + errorCount + ".txt");
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(errorLog))) {
                writer.println("=== Coder Plugin Error Log ===");
                writer.println("Error in script: " + fileName);
                writer.println("Timestamp: " + new Date());
                writer.println("=====================================================");
                writer.println();
                e.printStackTrace(writer);
                writer.println("=====================================================");
            }
            
            plugin.getLogger().warning("Error logged to: " + errorLog.getAbsolutePath());
        } catch (IOException ex) {
            plugin.getLogger().severe("Failed to log error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private int getErrorLogCount(File logsDir) {
        File[] files = logsDir.listFiles((dir, name) -> name.startsWith("Error-") && name.endsWith(".txt"));
        return (files != null) ? files.length : 0;
    }
}