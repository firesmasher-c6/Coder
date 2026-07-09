package me.coder.expansion;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ExpansionArchiveValidator {
    
    public static class ValidationResult {
        public boolean isValid;
        public String errorMessage;
        
        public ValidationResult(boolean isValid, String errorMessage) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
        }
    }
    
    public static ValidationResult validateArchive(File jsZipFile) {
        if (!jsZipFile.exists()) {
            return new ValidationResult(false, "ERROR: Archive file does not exist.");
        }
        
        if (!jsZipFile.isFile()) {
            return new ValidationResult(false, "ERROR: Path is not a file.");
        }
        
        if (!jsZipFile.getName().endsWith(".jszip")) {
            return new ValidationResult(false, "ERROR: Cannot load regular ZIP ARCHIVES!");
        }
        
        if (!isValidZipFile(jsZipFile)) {
            return new ValidationResult(false, "ERROR: CANNOT LOAD EXPANSION AS IT APPEARS TO BE CORRUPTED! (Only if the archive is actually a ex. txt file and renamed into .jszip)");
        }
        
        try (ZipFile zipFile = new ZipFile(jsZipFile)) {
            ZipEntry manifestEntry = zipFile.getEntry("META-INF/MANIFEST.JSON");
            if (manifestEntry == null) {
                return new ValidationResult(false, "ERROR: Archive MISSING A MANIFEST FILE!");
            }
            
            ZipEntry pluginConfigEntry = zipFile.getEntry("coder-expansion.plug.json");
            if (pluginConfigEntry == null) {
                return new ValidationResult(false, "ERROR: Expansion MISSING coder-expansion.plug.json!");
            }
            
            boolean hasSrcDir = false;
            boolean hasJsonInSrc = false;
            boolean hasJavaClass = false;
            
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                
                if (name.startsWith("SRC/")) {
                    hasSrcDir = true;
                    if (name.endsWith(".json") && !entry.isDirectory()) {
                        hasJsonInSrc = true;
                    }
                }
                
                if (name.endsWith(".class")) {
                    hasJavaClass = true;
                }
            }
            
            if (!hasSrcDir) {
                return new ValidationResult(false, "ERROR: Misconfigured Archive Structure. Missing SRC/ directory.");
            }
            
            if (!hasJsonInSrc) {
                return new ValidationResult(false, "ERROR: Misconfigured Archive Structure. SRC/ directory must contain .json files.");
            }
            
            if (hasJavaClass) {
                return new ValidationResult(false, "ERROR: Expansion are JAVA INSTEAD OF JSON!");
            }
            
            return new ValidationResult(true, null);
        } catch (IOException e) {
            return new ValidationResult(false, "ERROR: CANNOT LOAD EXPANSION AS IT APPEARS TO BE CORRUPTED! " + e.getMessage());
        }
    }
    
    private static boolean isValidZipFile(File file) {
        try (ZipFile zipFile = new ZipFile(file)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}