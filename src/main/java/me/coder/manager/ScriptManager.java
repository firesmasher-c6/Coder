package me.coder.manager;

import me.coder.CoderPlugin;
import me.coder.ScriptInterface;
import me.coder.manager.UserExecutionControl;
import org.bukkit.command.CommandSender;
import org.python.util.PythonInterpreter;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;
import javax.tools.*;
import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.SimpleDateFormat;
import java.util.*;

public class ScriptManager {
    private final CoderPlugin plugin;
    private final Map<String, Object> loadedScripts = new HashMap<>();

    public ScriptManager(CoderPlugin plugin) {
        this.plugin = plugin;
    }

    public void runScript(String fileName, CommandSender sender) {
        File file = new File(plugin.getDataFolder(), "scripts/" + fileName);

        if (!file.exists()) {
            sender.sendMessage("§cScript not found: " + fileName);
            return;
        }

        // Check for terminal/shell commands (instant rejection)
        if (UserExecutionControl.hasTerminalCommands(file)) {
            sender.sendMessage("§c§lError: Error T10!");
            logError(new Exception("Terminal command detected in: " + fileName), fileName);
            return;
        }

        // Check for dangerous imports in Java files
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

    /**
     * Run a script directly without checking for dangerous imports
     * Used after confirmation
     */
    public void runScriptDirect(String fileName, CommandSender sender) {
        File file = new File(plugin.getDataFolder(), "scripts/" + fileName);

        if (!file.exists()) {
            sender.sendMessage("§cScript not found: " + fileName);
            return;
        }

        try {
            if (fileName.endsWith(".py")) {
                runPython(file, sender);
            } else if (fileName.endsWith(".lua")) {
                runLua(file, sender);
            } else if (fileName.endsWith(".java")) {
                compileJava(file, sender);
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

    private void compileJava(File file, CommandSender sender) {
        sender.sendMessage("§d[Coder] Compiling Java: " + file.getName());
        
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            sender.sendMessage("§cError: JDK not found. Use a JDK, not a JRE.");
            return;
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        
        // Build classpath with standard Java classpath
        String classpath = System.getProperty("java.class.path");

        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
            Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjects(file);
            List<String> options = List.of("-classpath", classpath.toString(), "-encoding", "UTF-8");
            
            boolean success = compiler.getTask(null, fileManager, diagnostics, options, null, compilationUnits).call();

            if (!success) {
                sender.sendMessage("§cCompilation Failed!");
                for (Diagnostic<?> diagnostic : diagnostics.getDiagnostics()) {
                    sender.sendMessage("§c" + diagnostic.toString());
                }
                return;
            }
        } catch (IOException e) {
            sender.sendMessage("§cIO Error: " + e.getMessage());
            logError(e, file.getName());
            return;
        }

        try {
            List<URL> urlList = new ArrayList<>();
            urlList.add(file.getParentFile().toURI().toURL());
            
            URL[] urls = urlList.toArray(new URL[0]);
            try (URLClassLoader classLoader = new URLClassLoader(urls, this.getClass().getClassLoader())) {
                String className = file.getName().replace(".java", "");
                Class<?> clazz = classLoader.loadClass(className);
                Object instance = clazz.getDeclaredConstructor().newInstance();

                if (instance instanceof ScriptInterface) {
                    ((ScriptInterface) instance).run(sender);
                    sender.sendMessage("§aJava script executed successfully!");
                    loadedScripts.put(file.getName(), instance);
                } else {
                    sender.sendMessage("§cError: Class must implement ScriptInterface.");
                }
            }
        } catch (Exception e) {
            sender.sendMessage("§cExecution Error: " + e.getMessage());
            logError(e, file.getName());
        }
    }

    public void reloadScript(String fileName, CommandSender sender) {
        loadedScripts.remove(fileName);
        sender.sendMessage("§d[Coder] Reloaded: " + fileName);
    }

    public void loadScript(String fileName, CommandSender sender) {
        File file = new File(plugin.getDataFolder(), "scripts/" + fileName);
        if (!file.exists()) {
            sender.sendMessage("§cScript not found: " + fileName);
            return;
        }
        
        if (fileName.endsWith(".java")) {
            compileJava(file, sender);
            sender.sendMessage("§aScript loaded to memory.");
        } else {
            sender.sendMessage("§cOnly Java scripts can be preloaded.");
        }
    }

    public void unloadScript(String fileName, CommandSender sender) {
        if (loadedScripts.remove(fileName) != null) {
            sender.sendMessage("§aScript unloaded from memory.");
        } else {
            sender.sendMessage("§cScript not found in memory.");
        }
    }

    private void logError(Exception e, String fileName) {
        try {
            File logsDir = new File(plugin.getDataFolder(), "Logs/Error-Logs");
            
            // Ensure directory exists
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
                writer.println();
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