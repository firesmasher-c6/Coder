package me.coder.javafixer;

import me.coder.api.CoderAddon;
import me.coder.javafixer.UserExecutionControl;
import me.coder.javafixer.ErrorLogging;
import me.coder.javafixer.Libraries;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.Plugin;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

public class JavaFixerAddon implements CoderAddon, CommandExecutor, TabCompleter, Listener {

    public static CommandSender currentExecutor;

    private final JavaFixerPlugin plugin;
    private File scriptsFolder;
    private File javaClassesFolder;

    public JavaFixerAddon(JavaFixerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onEnable() {
        this.scriptsFolder = new File(plugin.getDataFolder().getParentFile(), "Coder/scripts");
        this.javaClassesFolder = plugin.getJavaClassesFolder();

        if (!scriptsFolder.exists()) scriptsFolder.mkdirs();
        if (!javaClassesFolder.exists()) javaClassesFolder.mkdirs();

        // Register packet/event level packet listeners to cleanly intercept BEFORE command executors fire
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Register custom fallback /coderjavafixer & /cjf commands safely
        registerCustomCommandDynamically();
        
        plugin.getLogger().info("JavaFixerAddon active. Listening cleanly to inbound Java pipelines.");
    }

    @Override
    public void onDisable() {
        clearBuildCache(javaClassesFolder);
    }

    /**
     * Intercepts standard player in-game command strings before Bukkit passes them to Coder.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (handleCommandIntercept(event.getPlayer(), event.getMessage())) {
            event.setCancelled(true); // Silence packet processing, letting Coder ignore the timeline entirely!
        }
    }

    /**
     * Intercepts console/terminal command strings before Bukkit passes them to Coder.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onConsoleCommand(ServerCommandEvent event) {
        if (handleCommandIntercept(event.getSender(), "/" + event.getCommand())) {
            event.setCancelled(true); // Cancel completely to stop usage help message
        }
    }

    /**
     * Core filtration parsing matrix to strictly capture only Java operations.
     */
    private boolean handleCommandIntercept(CommandSender sender, String rawMessage) {
        String message = rawMessage.trim();
        if (!message.startsWith("/")) return false;
        
        // Split space markers cleanly
        String[] split = message.substring(1).split("\\s+");
        if (split.length < 3) return false;

        String baseCmd = split[0].toLowerCase();
        String subCmd = split[1].toLowerCase();
        String inputName = split[2];

        // Match /coder run <file> or /code run <file>
        if ((baseCmd.equals("coder") || baseCmd.equals("code")) && (subCmd.equals("run") || subCmd.equals("execute"))) {
            boolean isExplicitJava = inputName.toLowerCase().endsWith(".java");
            boolean hasNoExtension = !inputName.contains(".");
            
            File targetJavaFile = null;

            if (isExplicitJava) {
                targetJavaFile = new File(scriptsFolder, inputName);
            } else if (hasNoExtension) {
                File prospectiveFile = new File(scriptsFolder, inputName + ".java");
                if (prospectiveFile.exists()) {
                    targetJavaFile = prospectiveFile;
                }
            }

            // ONLY intercept if it's explicitly a Java target asset!
            if (targetJavaFile != null && targetJavaFile.exists()) {
                boolean processIsSecure = me.coder.javafixer.UserExecutionControl.isExecutionSafe(targetJavaFile, sender);
                if (!processIsSecure) {
                    plugin.getLogger().warning("Execution halted by safety framework for sender: " + sender.getName());
                    return true; 
                }

                sender.sendMessage("§a[JavaFixer] Intercepted Java execution request. Compiling cleanly...");
                long startTime = System.currentTimeMillis();
                if (overrideAndCompile(targetJavaFile, sender)) {
                    long duration = System.currentTimeMillis() - startTime;
                    sender.sendMessage("§a[JavaFixer] Compilation successful (" + duration + "ms)! Invoking class execution...");
                    loadAndRunClass(targetJavaFile.getName().replace(".java", ""), sender);
                }
                return true; // Tells listener we handled it, cancelling the downstream warning
            }
        }
        return false; // Let alternative extensions (.py, .lua) drop cleanly into Coder's original engine!
    }

    private void registerCustomCommandDynamically() {
        try {
            org.bukkit.Server server = Bukkit.getServer();
            Field commandMapField = server.getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            SimpleCommandMap commandMap = (SimpleCommandMap) commandMapField.get(server);

            Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            constructor.setAccessible(true);
            PluginCommand cjfCommand = constructor.newInstance("coderjavafixer", plugin);
            
            cjfCommand.setAliases(List.of("cjf"));
            cjfCommand.setDescription("Control hub for pure Java execution assets.");
            cjfCommand.setExecutor(this);
            cjfCommand.setTabCompleter(this); 

            commandMap.register("coderjavafixer", cjfCommand);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmdName = command.getName().toLowerCase();

        if (cmdName.equals("coderjavafixer")) {
            if (args.length < 2) {
                sender.sendMessage("§e[CJF] Usage:");
                sender.sendMessage("§7- /cjf compile <FileName.java>");
                sender.sendMessage("§7- /cjf execute-class <ClassName.class>");
                return true;
            }

            String subCommand = args[0].toLowerCase();
            String targetName = args[1];

            if (subCommand.equals("compile")) {
                if (!targetName.endsWith(".java")) targetName += ".java";
                File javaFile = new File(scriptsFolder, targetName);

                if (!javaFile.exists()) {
                    sender.sendMessage("§c[CJF] File not found: /plugins/Coder/scripts/" + targetName);
                    return true;
                }

                if (!me.coder.javafixer.UserExecutionControl.isExecutionSafe(javaFile, sender)) return true;

                sender.sendMessage("§a[CJF] Compiling " + targetName + "...");
                if (overrideAndCompile(javaFile, sender)) {
                    sender.sendMessage("§a[CJF] Successfully compiled and saved to JavaClasses!");
                }
                return true;
            }

            if (subCommand.equals("execute-class")) {
                String className = targetName.replace(".class", "");
                File classFile = new File(javaClassesFolder, className + ".class");

                if (!classFile.exists()) {
                    sender.sendMessage("§c[CJF] Class not found in cache: /JavaClasses/" + className + ".class");
                    return true;
                }

                sender.sendMessage("§a[CJF] Loading and invoking class: " + className);
                loadAndRunClass(className, sender);
                return true;
            }
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String cmdName = command.getName().toLowerCase();
        List<String> completions = new ArrayList<>();

        if (cmdName.equals("coderjavafixer")) {
            if (args.length == 1) {
                if ("compile".startsWith(args[0].toLowerCase())) completions.add("compile");
                if ("execute-class".startsWith(args[0].toLowerCase())) completions.add("execute-class");
                return completions;
            }

            if (args.length == 2) {
                String subCommand = args[0].toLowerCase();
                String input = args[1].toLowerCase();

                if (subCommand.equals("compile")) {
                    File[] files = scriptsFolder.listFiles((dir, name) -> name.endsWith(".java"));
                    if (files != null) {
                        for (File file : files) {
                            if (file.getName().toLowerCase().startsWith(input)) completions.add(file.getName());
                        }
                    }
                }
                if (subCommand.equals("execute-class")) {
                    File[] files = javaClassesFolder.listFiles((dir, name) -> name.endsWith(".class"));
                    if (files != null) {
                        for (File file : files) {
                            if (file.getName().toLowerCase().startsWith(input)) completions.add(file.getName());
                        }
                    }
                }
            }
        }
        return completions;
    }

    private boolean overrideAndCompile(File javaFile, CommandSender sender) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            sender.sendMessage("§c[JavaFixer] System Java Compiler instance could not be initialized.");
            return false;
        }

        StringJoiner classpathJoiner = new StringJoiner(File.pathSeparator);
        String systemClassPath = System.getProperty("java.class.path");
        if (systemClassPath != null && !systemClassPath.isEmpty()) classpathJoiner.add(systemClassPath);

        String secondaryClassPath = me.coder.javafixer.Libraries.getCompilerClasspath();
        if (secondaryClassPath != null && !secondaryClassPath.isEmpty()) classpathJoiner.add(secondaryClassPath);

        try {
            File serverJar = new File(Bukkit.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            classpathJoiner.add(serverJar.getAbsolutePath());
        } catch (Exception ignored) {}

        for (Plugin runningPlugin : Bukkit.getPluginManager().getPlugins()) {
            try {
                File pluginJar = new File(runningPlugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
                classpathJoiner.add(pluginJar.getAbsolutePath());
            } catch (Exception ignored) {}
        }

        String[] args = {
            "-d", javaClassesFolder.getAbsolutePath(),
            "-cp", classpathJoiner.toString(), 
            "--release", "21",
            javaFile.getAbsolutePath()
        };

        ByteArrayOutputStream outputErrBuffer = new ByteArrayOutputStream();
        int result = compiler.run(null, null, outputErrBuffer, args);

        if (result == 0) {
            cleanStrayClasses();
            return true;
        } else {
            String errorLogs = outputErrBuffer.toString();
            me.coder.javafixer.ErrorLogging.logCompileError(plugin, javaFile.getName(), errorLogs);

            sender.sendMessage("§c❌ [JavaFixer] Compilation Failed! Error details below:");
            for (String logLine : errorLogs.split("\n")) {
                if (!logLine.trim().isEmpty()) sender.sendMessage("§4║ §c" + logLine.replace("\r", ""));
            }
            return false;
        }
    }

    private void loadAndRunClass(String className, CommandSender executor) {
        try {
            currentExecutor = executor;
            URL[] urls = new URL[]{javaClassesFolder.toURI().toURL()};
            try (URLClassLoader classLoader = new URLClassLoader(urls, plugin.getClass().getClassLoader())) {
                Class<?> loadedClass = Class.forName(className, true, classLoader);
                
                try {
                    java.lang.reflect.Method mainMethod = loadedClass.getMethod("main", String[].class);
                    mainMethod.invoke(null, (Object) new String[]{});
                    return; 
                } catch (NoSuchMethodException ignored) {}

                try {
                    loadedClass.getDeclaredConstructor().newInstance();
                } catch (NoSuchMethodException e) {
                    // Instantiated via default constructor
                }
            }
        } catch (Exception e) {
            executor.sendMessage("§c[JavaFixer] Execution Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            currentExecutor = null;
        }
    }

    private void cleanStrayClasses() {
        File[] strayClasses = scriptsFolder.listFiles((dir, name) -> name.endsWith(".class"));
        if (strayClasses != null) {
            for (File stray : strayClasses) stray.delete();
        }
    }

    private void clearBuildCache(File directory) {
        File[] cachedFiles = directory.listFiles();
        if (cachedFiles != null) {
            for (File file : cachedFiles) {
                if (file.isDirectory()) clearBuildCache(file);
                file.delete();
            }
        }
    }

    @Override public String getName() { return "CoderJavaFixer"; }
    @Override public String getVersion() { return "1.6.2"; }
    @Override public String getAuthor() { return "Developer"; }
}