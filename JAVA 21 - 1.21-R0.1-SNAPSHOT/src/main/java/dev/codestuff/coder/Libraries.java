package dev.codestuff.coder;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.StringJoiner;

public class Libraries {

    /**
     * Gathers and generates a comprehensive classpath string including the core server
     * environment and all running dependencies to eliminate missing import errors.
     */
    public static String getCompilerClasspath() {
        StringJoiner classpath = new StringJoiner(File.pathSeparator);

        // 1. Core system classpath (baseline JVM entries)
        String sysPath = System.getProperty("java.class.path");
        if (sysPath != null && !sysPath.isEmpty()) {
            classpath.add(sysPath);
        }

        // 2. Locate the running Paper/Spigot server JAR
        try {
            File bukkitJar = new File(Bukkit.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            classpath.add(bukkitJar.getAbsolutePath());
        } catch (Exception ignored) {}

        // 3. Paper-compatible transitive library extraction via reflection.
        //    Paper 1.18+ uses its own classloader (not URLClassLoader), but it
        //    still exposes getURLs() — we reach it via reflection instead of casting.
        ClassLoader serverLoader = Bukkit.class.getClassLoader();
        try {
            Method getUrls = serverLoader.getClass().getMethod("getURLs");
            URL[] urls = (URL[]) getUrls.invoke(serverLoader);
            for (URL url : urls) {
                try {
                    File jarFile = new File(url.toURI());
                    if (jarFile.exists() && jarFile.getName().endsWith(".jar")) {
                        classpath.add(jarFile.getAbsolutePath());
                    }
                } catch (Exception ignored) {}
            }
        } catch (NoSuchMethodException ignored) {
            // Fallback: try casting to URLClassLoader (older Paper / Spigot builds)
            if (serverLoader instanceof URLClassLoader ucl) {
                for (URL url : ucl.getURLs()) {
                    try {
                        File jarFile = new File(url.toURI());
                        if (jarFile.exists() && jarFile.getName().endsWith(".jar")) {
                            classpath.add(jarFile.getAbsolutePath());
                        }
                    } catch (Exception ignored2) {}
                }
            }
        } catch (Exception ignored) {}

        // 4. All active plugin JARs (includes Coder itself → JetBrains annotations, Gson, etc.)
        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            try {
                File pluginJar = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
                classpath.add(pluginJar.getAbsolutePath());
            } catch (Exception ignored) {}
        }

        return classpath.toString();
    }
}