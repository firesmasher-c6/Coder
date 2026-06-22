package me.coder.javafixer;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.StringJoiner;

public class JavaCompiler {

    private final Plugin plugin;
    private final File javaClassesFolder;

    public JavaCompiler(Plugin plugin, File javaClassesFolder) {
        this.plugin = plugin;
        this.javaClassesFolder = javaClassesFolder;
    }

    /**
     * Compile and execute a Java file
     */
    public boolean compileAndExecute(File javaFile, CommandSender executor) {
        // Security check
        if (!JavaExecutionControl.isExecutionSafe(javaFile, executor)) {
            return false;
        }

        executor.sendMessage("§a[JavaFixer] Compiling " + javaFile.getName() + "...");
        long startTime = System.currentTimeMillis();

        if (compile(javaFile, executor)) {
            long duration = System.currentTimeMillis() - startTime;
            executor.sendMessage("§a[JavaFixer] Compilation successful (" + duration + "ms)");
            
            String className = javaFile.getName().replace(".java", "");
            loadAndRunClass(className, executor);
            return true;
        }
        return false;
    }

    /**
     * Compile a Java file to bytecode
     */
    private boolean compile(File javaFile, CommandSender executor) {
        javax.tools.JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            executor.sendMessage("§c[JavaFixer] Java Compiler not available on this system");
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
            "-d", javaClassesFolder.getAbsolutePath(),
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
            
            executor.sendMessage("§c❌ [JavaFixer] Compilation failed!");
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
    private void loadAndRunClass(String className, CommandSender executor) {
        try {
            URL[] urls = new URL[]{javaClassesFolder.toURI().toURL()};
            try (URLClassLoader classLoader = new URLClassLoader(urls, plugin.getClass().getClassLoader())) {
                Class<?> loadedClass = Class.forName(className, true, classLoader);
                
                // Try main method first
                try {
                    java.lang.reflect.Method mainMethod = loadedClass.getMethod("main", String[].class);
                    mainMethod.invoke(null, (Object) new String[]{});
                    executor.sendMessage("§a[JavaFixer] Execution complete");
                    return; 
                } catch (NoSuchMethodException ignored) {}

                // Try no-arg constructor
                try {
                    loadedClass.getDeclaredConstructor().newInstance();
                    executor.sendMessage("§a[JavaFixer] Execution complete");
                } catch (NoSuchMethodException e) {
                    executor.sendMessage("§a[JavaFixer] Class loaded (no main method or constructor)");
                }
            }
        } catch (Exception e) {
            executor.sendMessage("§c[JavaFixer] Execution error: " + e.getMessage());
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
}