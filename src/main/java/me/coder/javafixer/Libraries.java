package me.coder.javafixer;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import java.io.File;
import java.util.StringJoiner;

public class Libraries {

    /**
     * Gathers and generates a comprehensive classpath string including the core server
     * environment and all running dependencies to eliminate missing import errors.
     */
    public static String getCompilerClasspath() {
        StringJoiner classpath = new StringJoiner(File.pathSeparator);

        // 1. Core System Classpath (Baseline JVM targets)
        String sysPath = System.getProperty("java.class.path");
        if (sysPath != null && !sysPath.isEmpty()) {
            classpath.add(sysPath);
        }

        // 2. Locate the running Paper/Spigot core API library file
        try {
            File bukkitJar = new File(Bukkit.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            classpath.add(bukkitJar.getAbsolutePath());
        } catch (Exception ignored) {}

        // 3. Inject all other active plugin libraries (like Coder itself)
        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            try {
                File pluginJar = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
                classpath.add(pluginJar.getAbsolutePath());
            } catch (Exception ignored) {}
        }

        return classpath.toString();
    }
}