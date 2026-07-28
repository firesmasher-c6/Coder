package dev.codestuff.coder.commands;

import dev.codestuff.coder.JavaCompiler;
import dev.codestuff.coder.CoderPlugin;
import dev.codestuff.coder.manager.ConfigManager;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CodercCommand implements CommandExecutor, TabCompleter {

    private static final String PREFIX = "§f[§bCoder§f] ";
    private static final String ERR    = "§c[Coder] ";

    private final CoderPlugin plugin;
    private final JavaCompiler javaCompiler;
    private final ConfigManager configManager;

    private final File scriptsFolder;
    private final File runtimeFolder;

    public CodercCommand(CoderPlugin plugin, JavaCompiler javaCompiler) {
        this.plugin = plugin;
        this.javaCompiler = javaCompiler;
        this.configManager = plugin.getConfigManager();
        this.scriptsFolder = new File(plugin.getDataFolder(), "scripts");
        this.runtimeFolder = new File(new File(plugin.getDataFolder(), "JavaClasses"), "Runtime");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage(ERR + "You must be an OP to use this command!");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "compile" -> {
                if (!configManager.isCodercCommandEnabled("compile")) {
                    sender.sendMessage(ERR + "The compile command is disabled in config.yml");
                } else {
                    handleCompile(sender, args);
                }
            }
            case "run" -> {
                if (!configManager.isCodercCommandEnabled("run")) {
                    sender.sendMessage(ERR + "The run command is disabled in config.yml");
                } else {
                    handleRun(sender, args);
                }
            }
            default -> sendHelp(sender, label);
        }
        return true;
    }

    // ── /coderc compile <filename.java> ───────────────────────────────────────

    private void handleCompile(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ERR + "Usage: /coderc compile <filename.java>");
            return;
        }

        String filename = args[1];
        if (!filename.endsWith(".java")) filename += ".java";

        File javaFile = new File(scriptsFolder, filename);
        if (!javaFile.exists()) {
            sender.sendMessage(ERR + "File not found in scripts/: §f" + filename);
            return;
        }

        // Validate the file is actually inside scripts/ (no path traversal)
        if (!javaFile.getAbsolutePath().startsWith(scriptsFolder.getAbsolutePath())) {
            sender.sendMessage(ERR + "Invalid file path.");
            return;
        }

        sender.sendMessage(PREFIX + "Compiling §e" + filename + "§f to JavaClasses/Runtime/...");
        long start = System.currentTimeMillis();

        boolean ok = compileOnly(javaFile, sender);
        if (ok) {
            long ms = System.currentTimeMillis() - start;
            sender.sendMessage(PREFIX + "§aCompilation successful §7(" + ms + "ms)§f. " +
                    "Output: §eJavaClasses/Runtime/" + filename.replace(".java", ".class"));
        }
    }

    // ── /coderc run <classname> ───────────────────────────────────────────────

    private void handleRun(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ERR + "Usage: /coderc run <classname>");
            return;
        }

        String className = args[1].replace(".class", "");
        File classFile = new File(runtimeFolder, className + ".class");

        if (!classFile.exists()) {
            sender.sendMessage(ERR + "Class not found in JavaClasses/Runtime/: §f" + className + ".class");
            return;
        }

        if (!classFile.getAbsolutePath().startsWith(runtimeFolder.getAbsolutePath())) {
            sender.sendMessage(ERR + "Invalid class path.");
            return;
        }

        sender.sendMessage(PREFIX + "Running §e" + className + "§f...");
        runClass(className, sender);
    }

    // ── Compile-only (no execution) ───────────────────────────────────────────

    private boolean compileOnly(File javaFile, CommandSender realSender) {
        CommandSender silenced = new SilencedSender(realSender,
                "[Coder] Execution", "[Coder] Class loaded");

        return javaCompiler.compileAndExecute(javaFile, silenced);
    }

    // ── Run a .class from Runtime/ ────────────────────────────────────────────

    private void runClass(String className, CommandSender sender) {
        try {
            URL[] urls = new URL[]{runtimeFolder.toURI().toURL()};
            try (URLClassLoader loader = new URLClassLoader(urls, plugin.getClass().getClassLoader())) {
                Class<?> cls = Class.forName(className, true, loader);

                try {
                    java.lang.reflect.Method main = cls.getMethod("main", String[].class);
                    main.invoke(null, (Object) new String[]{});
                    sender.sendMessage(PREFIX + "§aExecution complete.");
                    return;
                } catch (NoSuchMethodException ignored) {}

                try {
                    cls.getDeclaredConstructor().newInstance();
                    sender.sendMessage(PREFIX + "§aExecution complete.");
                } catch (NoSuchMethodException e) {
                    sender.sendMessage(PREFIX + "§eClass loaded (no main method or no-arg constructor found).");
                }
            }
        } catch (ClassNotFoundException e) {
            sender.sendMessage(ERR + "Could not load class §f" + className +
                    "§c. Make sure it was compiled first.");
        } catch (Exception e) {
            sender.sendMessage(ERR + "Execution error: " + e.getMessage());
            plugin.getLogger().severe("[coderc run] " + e.getMessage());
        }
    }

    // ── Help ──────────────────────────────────────────────────────────────────

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage("§6══════════════════════════════════════");
        sender.sendMessage("§6  Coder Java Compiler  §8(" + label + ")");
        sender.sendMessage("§6══════════════════════════════════════");
        sender.sendMessage("§e/" + label + " compile §f<file.java>");
        sender.sendMessage("§7  Compiles a .java from §fscripts/§7 → §fJavaClasses/Runtime/");
        sender.sendMessage("§e/" + label + " run §f<classname>");
        sender.sendMessage("§7  Runs a .class from §fJavaClasses/Runtime/");
        sender.sendMessage("§6══════════════════════════════════════");
    }

    // ── Tab completion ────────────────────────────────────────────────────────

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) return List.of();

        if (args.length == 1) {
            return filterPrefix(List.of("compile", "run"), args[0]);
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("compile")) {
                // List .java files in scripts/
                List<String> files = new ArrayList<>();
                File[] javaFiles = scriptsFolder.listFiles((d, n) -> n.endsWith(".java"));
                if (javaFiles != null) for (File f : javaFiles) files.add(f.getName());
                return filterPrefix(files, args[1]);
            }
            if (args[0].equalsIgnoreCase("run")) {
                // List .class files in Runtime/
                List<String> classes = new ArrayList<>();
                File[] classFiles = runtimeFolder.listFiles((d, n) -> n.endsWith(".class"));
                if (classFiles != null) for (File f : classFiles) classes.add(f.getName().replace(".class", ""));
                return filterPrefix(classes, args[1]);
            }
        }

        return List.of();
    }

    private List<String> filterPrefix(List<String> options, String prefix) {
        List<String> result = new ArrayList<>();
        for (String s : options) {
            if (s.toLowerCase().startsWith(prefix.toLowerCase())) result.add(s);
        }
        return result;
    }

    // ── Sender wrapper to suppress execution noise during compile-only ────────

    private static class SilencedSender implements CommandSender {
        private final CommandSender delegate;
        private final String[] suppressPrefixes;

        SilencedSender(CommandSender delegate, String... suppressPrefixes) {
            this.delegate = delegate;
            this.suppressPrefixes = suppressPrefixes;
        }

        private boolean suppressed(String message) {
            for (String prefix : suppressPrefixes) {
                if (message.contains(prefix)) return true;
            }
            return false;
        }

        @Override public void sendMessage(String message)      { if (!suppressed(message)) delegate.sendMessage(message); }
        @Override public void sendMessage(String... messages)  { for (String m : messages) sendMessage(m); }
        @Override public void sendMessage(java.util.UUID uuid, String message)     { if (!suppressed(message)) delegate.sendMessage(uuid, message); }
        @Override public void sendMessage(java.util.UUID uuid, String... messages) { for (String m : messages) sendMessage(uuid, m); }

        @Override public void sendMessage(net.kyori.adventure.text.Component message) { delegate.sendMessage(message); }

        @Override public org.bukkit.Server getServer()  { return delegate.getServer(); }
        @Override public String getName()               { return delegate.getName(); }
        @Override public net.kyori.adventure.text.Component name() { return delegate.name(); }

        @Override public boolean isPermissionSet(String name)                            { return delegate.isPermissionSet(name); }
        @Override public boolean isPermissionSet(org.bukkit.permissions.Permission perm) { return delegate.isPermissionSet(perm); }
        @Override public boolean hasPermission(String name)                              { return delegate.hasPermission(name); }
        @Override public boolean hasPermission(org.bukkit.permissions.Permission perm)   { return delegate.hasPermission(perm); }
        @Override public org.bukkit.permissions.PermissionAttachment addAttachment(org.bukkit.plugin.Plugin plugin, String name, boolean value) { return delegate.addAttachment(plugin, name, value); }
        @Override public org.bukkit.permissions.PermissionAttachment addAttachment(org.bukkit.plugin.Plugin plugin)                            { return delegate.addAttachment(plugin); }
        @Override public org.bukkit.permissions.PermissionAttachment addAttachment(org.bukkit.plugin.Plugin plugin, String name, boolean value, int ticks) { return delegate.addAttachment(plugin, name, value, ticks); }
        @Override public org.bukkit.permissions.PermissionAttachment addAttachment(org.bukkit.plugin.Plugin plugin, int ticks)                 { return delegate.addAttachment(plugin, ticks); }
        @Override public void removeAttachment(org.bukkit.permissions.PermissionAttachment attachment) { delegate.removeAttachment(attachment); }
        @Override public void recalculatePermissions() { delegate.recalculatePermissions(); }
        @Override public java.util.Set<org.bukkit.permissions.PermissionAttachmentInfo> getEffectivePermissions() { return delegate.getEffectivePermissions(); }
        @Override public boolean isOp()                { return delegate.isOp(); }
        @Override public void setOp(boolean value)     { delegate.setOp(value); }
        @Override public CommandSender.Spigot spigot() { return delegate.spigot(); }
    }
}