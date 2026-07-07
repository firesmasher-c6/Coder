package me.coder;

import me.coder.manager.JavaScriptManager;
import me.coder.manager.BukkitInteractionHandler;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

public class JavaCompiler {

    private final Plugin plugin;
    private final File javaClassesFolder;
    private final File loadedFolder;
    private final File runtimeFolder;
    private final Map<String, Class<?>> loadedClasses = new HashMap<>();
    private final JavaScriptManager scriptManager;
    private final BukkitInteractionHandler bukkitHandler;

    public JavaCompiler(Plugin plugin, File javaClassesFolder) {
        this.plugin = plugin;
        this.javaClassesFolder = javaClassesFolder;
        this.loadedFolder = new File(javaClassesFolder, "Loaded");
        this.runtimeFolder = new File(javaClassesFolder, "Runtime");
        this.scriptManager = new JavaScriptManager((org.bukkit.plugin.java.JavaPlugin) plugin);
        this.bukkitHandler = new BukkitInteractionHandler((org.bukkit.plugin.java.JavaPlugin) plugin);
        
        // Create folders if they don't exist
        if (!loadedFolder.exists()) {
            loadedFolder.mkdirs();
        }
        if (!runtimeFolder.exists()) {
            runtimeFolder.mkdirs();
        }
    }

    /**
     * Compile and execute a Java file (stores in Runtime folder)
     */
    public boolean compileAndExecute(File javaFile, CommandSender executor) {
        // Security check using new JavaScriptManager (JSM)
        try {
            if (!scriptManager.validateScript(javaFile)) {
                executor.sendMessage("§c[Coder] Script validation failed: " + scriptManager.getBlockReason(javaFile));
                return false;
            }
        } catch (Exception e) {
            executor.sendMessage("§c[Coder] Error validating script: " + e.getMessage());
            return false;
        }

        executor.sendMessage("§a[Coder] Compiling " + javaFile.getName() + "...");
        long startTime = System.currentTimeMillis();

        if (compile(javaFile, executor, runtimeFolder)) {
            long duration = System.currentTimeMillis() - startTime;
            executor.sendMessage("§a[Coder] Compilation successful (" + duration + "ms)");
            
            String className = javaFile.getName().replace(".java", "");
            loadAndRunClass(className, executor, runtimeFolder);
            return true;
        }
        return false;
    }

    /**
     * Compile and load a Java file to memory (stores in Loaded folder)
     */
    public boolean compileAndLoad(File javaFile, CommandSender executor) {
        // Security check using new JavaScriptManager (JSM)
        try {
            if (!scriptManager.validateScript(javaFile)) {
                executor.sendMessage("§c[Coder] Script validation failed: " + scriptManager.getBlockReason(javaFile));
                return false;
            }
        } catch (Exception e) {
            executor.sendMessage("§c[Coder] Error validating script: " + e.getMessage());
            return false;
        }

        executor.sendMessage("§a[Coder] Compiling " + javaFile.getName() + "...");
        long startTime = System.currentTimeMillis();
        String className = javaFile.getName().replace(".java", "");

        if (compile(javaFile, executor, loadedFolder)) {
            long duration = System.currentTimeMillis() - startTime;
            executor.sendMessage("§a[Coder] Compilation successful (" + duration + "ms)");
            executor.sendMessage("§a[Coder] Class stored in Loaded folder");
            
            // Also register with BukkitInteractionHandler for Bukkit integration
            File classFile = new File(loadedFolder, className + ".class");
            if (classFile.exists()) {
                bukkitHandler.loadClassFile(classFile);
                executor.sendMessage("§a[Coder] Class registered for Bukkit interaction");
            }
            
            loadAndRunClass(className, executor, loadedFolder);
            return true;
        }
        return false;
    }

    /**
     * Unload a previously loaded Java class
     */
    public void unloadClass(String className, CommandSender executor) {
        if (loadedClasses.remove(className) != null) {
            executor.sendMessage("§a[Coder] Class unloaded: " + className);
            plugin.getLogger().info("[Coder] Unloaded class: " + className);
        } else {
            executor.sendMessage("§c[Coder] Class not found in memory: " + className);
        }
    }

    /**
     * List all loaded classes
     */
    public void listLoadedClasses(CommandSender executor) {
        if (loadedClasses.isEmpty()) {
            executor.sendMessage("§c[Coder] No classes currently loaded");
            return;
        }

        executor.sendMessage("§a[Coder] ========== Loaded Classes ==========");
        int count = 1;
        for (String className : loadedClasses.keySet()) {
            executor.sendMessage("§a  " + count + ". " + className);
            count++;
        }
        executor.sendMessage("§a[Coder] =====================================");
    }

    /**
     * Compile a Java file to bytecode
     */
    private boolean compile(File javaFile, CommandSender executor, File outputFolder) {
        javax.tools.JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            executor.sendMessage("§c[Coder] Java Compiler not available on this system");
            return false;
        }

        // Build classpath
        StringJoiner classPath = new StringJoiner(File.pathSeparator);
        
        String systemClassPath = System.getProperty("java.class.path");
        if (systemClassPath != null && !systemClassPath.isEmpty()) {
            classPath.add(systemClassPath);
        }

        // Add Bukkit/Paper API
        try {
            File serverJar = new File(Bukkit.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            classPath.add(serverJar.getAbsolutePath());
        } catch (Exception ignored) {}

        // Add all plugin JARs
        for (org.bukkit.plugin.Plugin runningPlugin : Bukkit.getPluginManager().getPlugins()) {
            try {
                File pluginJar = new File(runningPlugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
                classPath.add(pluginJar.getAbsolutePath());
            } catch (Exception ignored) {}
        }

        // Compile
        String[] args = {
            "-d", outputFolder.getAbsolutePath(),
            "-cp", classPath.toString(), 
            "--release", "21",
            javaFile.getAbsolutePath()
        };

        ByteArrayOutputStream errorBuffer = new ByteArrayOutputStream();
        int result = compiler.run(null, null, errorBuffer, args);

        if (result == 0) {
            cleanStrayClasses();
            return true;
        } else {
            String errorOutput = errorBuffer.toString();
            ErrorLogging.logCompileError(plugin, javaFile.getName(), errorOutput);
            
            executor.sendMessage("§c❌ [Coder] Compilation failed!");
            for (String line : errorOutput.split("\n")) {
                if (!line.trim().isEmpty()) {
                    executor.sendMessage("§4║ §c" + line.replace("\r", ""));
                }
            }
            return false;
        }
    }

    /**
     * Load and execute a compiled Java class
     */
    private void loadAndRunClass(String className, CommandSender executor, File classFolder) {
        try {
            URL[] urls = new URL[]{classFolder.toURI().toURL()};
            try (URLClassLoader classLoader = new URLClassLoader(urls, plugin.getClass().getClassLoader())) {
                Class<?> loadedClass = Class.forName(className, true, classLoader);
                loadedClasses.put(className, loadedClass);
                
                // Try main method first
                try {
                    java.lang.reflect.Method mainMethod = loadedClass.getMethod("main", String[].class);
                    mainMethod.invoke(null, (Object) new String[]{});
                    executor.sendMessage("§a[Coder] Execution complete");
                    return; 
                } catch (NoSuchMethodException ignored) {}

                // Try no-arg constructor
                try {
                    loadedClass.getDeclaredConstructor().newInstance();
                    executor.sendMessage("§a[Coder] Execution complete");
                } catch (NoSuchMethodException e) {
                    executor.sendMessage("§a[Coder] Class loaded (no main method or constructor)");
                }
            }
        } catch (Exception e) {
            executor.sendMessage("§c[Coder] Execution error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Clean up stray .class files left in scripts folder
     */
    private void cleanStrayClasses() {
        File scriptsFolder = new File(plugin.getDataFolder(), "scripts");
        File[] strayClasses = scriptsFolder.listFiles((dir, name) -> name.endsWith(".class"));
        if (strayClasses != null) {
            for (File stray : strayClasses) {
                stray.delete();
            }
        }
    }

    /**
     * Clear build cache on disable
     */
    public void clearCache() {
        loadedClasses.clear();
        File[] files = javaClassesFolder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    clearDirectoryRecursive(file);
                } else {
                    file.delete();
                }
            }
        }
    }

    private void clearDirectoryRecursive(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    clearDirectoryRecursive(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }

    /**
     * Get the BukkitInteractionHandler for Java class Bukkit integration
     */
    public BukkitInteractionHandler getBukkitHandler() {
        return bukkitHandler;
    }

    /**
     * Get the JavaScriptManager for strict Java security validation
     */
    public JavaScriptManager getJavaScriptManager() {
        return scriptManager;
    }

    /**
     * Cleanup handlers on disable
     */
    public void cleanup() {
        bukkitHandler.cleanup();
    }
}