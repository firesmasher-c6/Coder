package me.coder.api;

import java.io.*;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class VerifyGenerator {
    
    private static final String VERIFY_FILE = "VERIFIED.vf";
    private static final String VERIFY_CONTENT = "#! Verified\nDependency=Coder\nVerified?=true\n";
    
    /**
     * Verify if addon implements CoderAddonSecurity and generate VERIFIED.vf
     */
    public static boolean verifyAddon(File addonFile) {
        try {
            if (!addonFile.getName().endsWith(".jar")) {
                return false;
            }
            
            JarFile jar = new JarFile(addonFile);
            
            // Check if addon implements CoderAddonSecurity
            boolean implementsSecurity = false;
            java.util.Enumeration<JarEntry> entries = jar.entries();
            
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    String classContent = readJarEntry(jar, entry);
                    if (classContent.contains("implements CoderAddonSecurity") || 
                        classContent.contains("extends CoderAddon")) {
                        implementsSecurity = true;
                        break;
                    }
                }
            }
            
            jar.close();
            
            if (!implementsSecurity) {
                return false;
            }
            
            // Add VERIFIED.vf to JAR
            return addVerificationFile(addonFile);
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Add VERIFIED.vf file to addon JAR
     */
    private static boolean addVerificationFile(File addonFile) {
        try {
            File tempFile = File.createTempFile("coder_verify_", ".jar");
            
            try (JarFile originalJar = new JarFile(addonFile);
                 ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempFile))) {
                
                // Copy existing entries
                java.util.Enumeration<JarEntry> entries = originalJar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (!entry.getName().equals(VERIFY_FILE)) {
                        zos.putNextEntry(new ZipEntry(entry.getName()));
                        InputStream is = originalJar.getInputStream(entry);
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = is.read(buffer)) > 0) {
                            zos.write(buffer, 0, len);
                        }
                        is.close();
                        zos.closeEntry();
                    }
                }
                
                // Add VERIFIED.vf
                ZipEntry verifyEntry = new ZipEntry(VERIFY_FILE);
                zos.putNextEntry(verifyEntry);
                zos.write(VERIFY_CONTENT.getBytes());
                zos.closeEntry();
            }
            
            // Replace original file
            addonFile.delete();
            tempFile.renameTo(addonFile);
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    public static boolean isAddonVerified(File addonFile) {
        try {
            JarFile jar = new JarFile(addonFile);
            ZipEntry verifyEntry = jar.getEntry(VERIFY_FILE);
            
            if (verifyEntry == null) {
                jar.close();
                return false;
            }
            
            String content = readJarEntry(jar, verifyEntry);
            jar.close();
            
            return content.contains("#! Verified") && 
                   content.contains("Dependency=Coder") && 
                   content.contains("Verified?=true");
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Read JAR entry content
     */
    private static String readJarEntry(JarFile jar, ZipEntry entry) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(jar.getInputStream(entry)));
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            reader.close();
            return content.toString();
        } catch (Exception e) {
            return "";
        }
    }
}