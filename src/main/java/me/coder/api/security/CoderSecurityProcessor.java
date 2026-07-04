package me.coder.api.security;

import me.coder.api.CoderAddonSecurity;
import org.bukkit.plugin.Plugin;
import java.io.File;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class CoderSecurityProcessor {
    
    private static final String[] MALWARE_PATTERNS = {
        "Runtime.getRuntime().exec", "ProcessBuilder", "java.lang.Process",
        "System.exit", "System.setProperty", "System.setSecurityManager",
        "reflection.Method.invoke", "ClassLoader.defineClass", "Unsafe.putObject",
        "FileOutputStream", "FileWriter", "RandomAccessFile",
        "Socket", "ServerSocket", "URLConnection", "HTTP", "FTP", "ssh", "telnet"
    };
    
    public static boolean processAddonSecurity(CoderAddonSecurity addon, File addonFile) {
        try {
            if (!addon.isVerified()) {
                System.out.println("[Coder Security] Addon not verified, applying standard checks");
            }
            
            if (!validateAddonFile(addonFile)) {
                return false;
            }
            
            String malwareScan = addon.scanForMalware(addonFile);
            if (malwareScan != null && !malwareScan.isEmpty()) {
                System.out.println("[Coder Security] ⚠️ Malware scan detected: " + malwareScan);
                return addon.getThreatLevel() < 3;
            }
            
            if (!addon.validateSecurity(addonFile)) {
                System.out.println("[Coder Security] ❌ Security validation failed");
                return false;
            }
            
            if (addon.getThreatLevel() >= 3) {
                System.out.println("[Coder Security] 🚫 CRITICAL threat blocked");
                return false;
            }
            
            return true;
        } catch (Exception e) {
            System.err.println("[Coder Security] Error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public static boolean shouldBypassUserExecutionControl(CoderAddonSecurity addon) {
        if (!addon.isVerified() || addon.getSecurityLevel() < 2 || addon.getThreatLevel() > 0) {
            return false;
        }
        return addon.bypassUserExecutionControl();
    }
    
    private static boolean validateAddonFile(File addonFile) {
        if (!addonFile.exists() || !addonFile.getName().endsWith(".jar")) {
            System.out.println("[Coder Security] Invalid addon file");
            return false;
        }
        
        try {
            JarFile jar = new JarFile(addonFile);
            if (jar.getEntry("plugin.yml") == null && jar.getEntry("META-INF/MANIFEST.MF") == null) {
                jar.close();
                return false;
            }
            jar.close();
            return true;
        } catch (Exception e) {
            System.out.println("[Coder Security] JAR validation failed: " + e.getMessage());
            return false;
        }
    }
    
    public static String scanAddonForMalware(File addonFile) {
        StringBuilder report = new StringBuilder();
        try {
            JarFile jar = new JarFile(addonFile);
            java.util.Enumeration<JarEntry> entries = jar.entries();
            
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    String classContent = readJarEntry(jar, entry);
                    for (String pattern : MALWARE_PATTERNS) {
                        if (classContent.contains(pattern)) {
                            report.append("Found: ").append(pattern).append(" in ").append(entry.getName()).append("\n");
                        }
                    }
                }
            }
            jar.close();
        } catch (Exception e) {
            report.append("Scan error: ").append(e.getMessage());
        }
        return report.toString();
    }
    
    private static String readJarEntry(JarFile jar, JarEntry entry) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(jar.getInputStream(entry)));
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
            reader.close();
            return content.toString();
        } catch (Exception e) {
            return "";
        }
    }
    
    public static int getAddonPermissionLevel(CoderAddonSecurity addon) {
        int level = 0;
        if (addon.allowDangerousImports()) level += 1;
        if (addon.allowTerminalCommands()) level += 2;
        if (addon.allowFileSystemAccess()) level += 4;
        if (addon.allowReflection()) level += 8;
        if (addon.requiresElevatedPermissions()) level += 16;
        return level;
    }
    
    public static String generateSecurityReport(CoderAddonSecurity addon, File addonFile) {
        StringBuilder report = new StringBuilder();
        report.append("=== Coder Addon Security Report ===\n");
        report.append("Verified: ").append(addon.isVerified() ? "✓ Yes" : "✗ No").append("\n");
        report.append("Security Level: ").append(getSecurityLevelName(addon.getSecurityLevel())).append("\n");
        report.append("Threat Level: ").append(getThreatLevelName(addon.getThreatLevel())).append("\n");
        report.append("Elevated Permissions: ").append(addon.requiresElevatedPermissions() ? "Yes" : "No").append("\n");
        report.append("\nPermissions:\n");
        report.append("  - Dangerous Imports: ").append(addon.allowDangerousImports() ? "✓" : "✗").append("\n");
        report.append("  - Terminal Commands: ").append(addon.allowTerminalCommands() ? "✓" : "✗").append("\n");
        report.append("  - File System: ").append(addon.allowFileSystemAccess() ? "✓" : "✗").append("\n");
        report.append("  - Reflection: ").append(addon.allowReflection() ? "✓" : "✗").append("\n");
        report.append("\nBypass UserExecutionControl: ").append(shouldBypassUserExecutionControl(addon) ? "✓ Yes" : "✗ No").append("\n");
        report.append("====================================\n");
        return report.toString();
    }
    
    private static String getSecurityLevelName(int level) {
        switch (level) {
            case 0: return "RESTRICTED";
            case 1: return "STANDARD";
            case 2: return "TRUSTED";
            case 3: return "UNRESTRICTED";
            default: return "UNKNOWN";
        }
    }
    
    private static String getThreatLevelName(int level) {
        switch (level) {
            case 0: return "LOW";
            case 1: return "MEDIUM";
            case 2: return "HIGH";
            case 3: return "CRITICAL";
            default: return "UNKNOWN";
        }
    }
}