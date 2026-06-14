package me.coder.javafixer;

import me.coder.api.CoderAddon;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.command.TabCompleter;
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

public class JavaFixerAddon implements CoderAddon, CommandExecutor, TabCompleter {

    // Thread-safe global reference that running script assets look up to communicate with the command executor
    public static CommandSender currentExecutor;

    private final JavaFixerPlugin plugin;
    private File scriptsFolder;
    private File javaClassesFolder;
    private PluginCommand originalCoderCommand;

    public JavaFixerAddon(JavaFixerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onEnable() {
        this.scriptsFolder = new File(plugin.getDataFolder().getParentFile(), "Coder/scripts");
        this.javaClassesFolder = plugin.getJavaClassesFolder();

        if (!scriptsFolder.exists()) scriptsFolder.mkdirs();
        if (!javaClassesFolder.exists()) javaClassesFolder.mkdirs();

        // 1. Hijack the existing /coder command from the core plugin
        hijackCoderCommand();

        // 2. Programmatically register /coderjavafixer & /cjf warning-free
        registerCustomCommandDynamically();
        
        plugin.getLogger().info("JavaFixerAddon active. All command routing matrices initialized.");
    }

    @Override
    public void onDisable() {
        if (originalCoderCommand != null) {
            originalCoderCommand.setExecutor(originalCoderCommand.getPlugin());
        }
        clearBuildCache(javaClassesFolder);
    }

    private void hijackCoderCommand() {
        try {
            PluginCommand coderCmd = Bukkit.getPluginCommand("coder");
            if (coderCmd != null) {
                this.originalCoderCommand = coderCmd;
                coderCmd.setExecutor(this);
                coderCmd.setTabCompleter(this); // Route existing /coder tab requests here
            }
        } catch (Exception ignored) {}
    }

    /**
     * Extracts the live CommandMap from the CraftServer implementation via reflection
     * to completely bypass deprecated SimplePluginManager fields warning-free.
     */
    private void registerCustomCommandDynamically() {
        try {
            org.bukkit.Server server = Bukkit.getServer();
            
            // Bypass SimplePluginManager deprecation entirely by extracting commandMap directly from the Server implementation
            Field commandMapField = server.getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            SimpleCommandMap commandMap = (SimpleCommandMap) commandMapField.get(server);

            // Create a custom PluginCommand instance using reflection constructors
            Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            constructor.setAccessible(true);
            PluginCommand cjfCommand = constructor.newInstance("coderjavafixer", plugin);
            
            cjfCommand.setAliases(List.of("cjf"));
            cjfCommand.setDescription("Control hub for pure Java execution assets.");
            cjfCommand.setExecutor(this);
            cjfCommand.setTabCompleter(this); // Enable dynamic multi-layered tab completions

            // Register directly into the live server command table
            commandMap.register("coderjavafixer", cjfCommand);
            plugin.getLogger().info("🎯 Successfully registered dynamic command fallback /coderjavafixer (/cjf)");

        } catch (Exception e) {
            plugin.getLogger().severe("❌ Dynamic Paper command registration failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmdName = command.getName().toLowerCase();

        // --- HANDLER 1: Intercepting /coder requests ---
        if (cmdName.equals("coder")) {
            if (args.length >= 2 && (args[0].equalsIgnoreCase("run") || args[0].equalsIgnoreCase("execute"))) {
                String fileName = args[1];
                if (!fileName.endsWith(".java") && !fileName.contains(".")) {
                    fileName += ".java";
                }

                if (fileName.endsWith(".java")) {
                    File targetJavaFile = new File(scriptsFolder, fileName);
                    if (targetJavaFile.exists()) {
                        
                        // 🛡️ User Execution Control Scanning Layer
                        if (!UserExecutionControl.isExecutionSafe(targetJavaFile, sender)) {
                            return true;
                        }

                        sender.sendMessage("§a[JavaFixer] Intercepted Java execution request. Compiling cleanly...");
                        if (overrideAndCompile(targetJavaFile, sender)) {
                            sender.sendMessage("§a[JavaFixer] Compilation successful! Invoking class execution...");
                            loadAndRunClass(fileName.replace(".java", ""), sender);
                        }
                        return true;
                    }
                }
            }
            if (originalCoderCommand != null && originalCoderCommand.getPlugin() instanceof CommandExecutor) {
                return ((CommandExecutor) originalCoderCommand.getPlugin()).onCommand(sender, command, label, args);
            }
            return false;
        }

        // --- HANDLER 2: Processing /cjf subcommands ---
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

                // 🛡️ User Execution Control Scanning Layer
                if (!UserExecutionControl.isExecutionSafe(javaFile, sender)) {
                    return true;
                }

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

    /**
     * DYNAMIC TAB COMPLETION LOOKUP ENGINE
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String cmdName = command.getName().toLowerCase();
        List<String> completions = new ArrayList<>();

        if (cmdName.equals("coder")) {
            if (args.length == 1) {
                completions.add("run");
                completions.add("execute");
                return completions;
            }
            if (args.length == 2 && (args[0].equalsIgnoreCase("run") || args[0].equalsIgnoreCase("execute"))) {
                File[] files = scriptsFolder.listFiles((dir, name) -> name.endsWith(".java"));
                if (files != null) {
                    for (File file : files) {
                        if (file.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                            completions.add(file.getName());
                        }
                    }
                }
                return completions;
            }
            if (originalCoderCommand != null && originalCoderCommand.getPlugin() instanceof TabCompleter) {
                return ((TabCompleter) originalCoderCommand.getPlugin()).onTabComplete(sender, command, alias, args);
            }
            return Collections.emptyList();
        }

        if (cmdName.equals("coderjavafixer")) {
            if (args.length == 1) {
                String input = args[0].toLowerCase();
                if ("compile".startsWith(input)) completions.add("compile");
                if ("execute-class".startsWith(input)) completions.add("execute-class");
                return completions;
            }

            if (args.length == 2) {
                String subCommand = args[0].toLowerCase();
                String input = args[1].toLowerCase();

                if (subCommand.equals("compile")) {
                    File[] files = scriptsFolder.listFiles((dir, name) -> name.endsWith(".java"));
                    if (files != null) {
                        for (File file : files) {
                            if (file.getName().toLowerCase().startsWith(input)) {
                                completions.add(file.getName());
                            }
                        }
                    }
                }
                
                if (subCommand.equals("execute-class")) {
                    File[] files = javaClassesFolder.listFiles((dir, name) -> name.endsWith(".class"));
                    if (files != null) {
                        for (File file : files) {
                            if (file.getName().toLowerCase().startsWith(input)) {
                                completions.add(file.getName());
                            }
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
        classpathJoiner.add(System.getProperty("java.class.path"));

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
            plugin.getLogger().info("⚡ Cached: " + javaFile.getName() + " -> /plugins/Coder/JavaClasses/");
            cleanStrayClasses();
            return true;
        } else {
            String errorLogs = outputErrBuffer.toString();
            sender.sendMessage("§c❌ [JavaFixer] Compilation Failed! Error details below:");
            for (String logLine : errorLogs.split("\n")) {
                if (!logLine.trim().isEmpty()) {
                    sender.sendMessage("§4║ §c" + logLine.replace("\r", ""));
                }
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
                    executor.sendMessage("§7[JavaFixer] Executed script main() context successfully.");
                    return; 
                } catch (NoSuchMethodException ignored) {}

                try {
                    loadedClass.getDeclaredConstructor().newInstance();
                    executor.sendMessage("§7[JavaFixer] Instantiated script constructor successfully.");
                } catch (NoSuchMethodException e) {
                    executor.sendMessage("§7[JavaFixer] Class " + loadedClass.getSimpleName() + " initialized via standard static context.");
                }
            }
        } catch (Exception e) {
            executor.sendMessage("§c[JavaFixer] Execution Error: " + e.getMessage());
            plugin.getLogger().severe("Error executing class " + className);
            e.printStackTrace();
        } finally {
            currentExecutor = null;
        }
    }

    private void cleanStrayClasses() {
        File[] strayClasses = scriptsFolder.listFiles((dir, name) -> name.endsWith(".class"));
        if (strayClasses != null) {
            for (File stray : strayClasses) {
                stray.delete();
            }
        }
    }

    private void clearBuildCache(File directory) {
        File[] cachedFiles = directory.listFiles();
        if (cachedFiles != null) {
            for (File file : cachedFiles) {
                if (file.isDirectory()) {
                    clearBuildCache(file);
                }
                file.delete();
            }
        }
    }

    @Override public String getName() { return "CoderJavaFixer"; }
    @Override public String getVersion() { return "1.0.0"; }
    @Override public String getAuthor() { return "Developer"; }
}