package me.coder.api;

import java.io.File;

public interface CoderAddonSecurity {
    
    boolean bypassUserExecutionControl();
    
    default int getSecurityLevel() {
        return 1;
    }
    
    boolean isVerified();
    
    default String getSignature() {
        return null;
    }
    
    boolean allowDangerousImports();
    
    boolean allowTerminalCommands();
    
    boolean allowFileSystemAccess();
    
    boolean allowReflection();
    
    default String[] getWhitelistedImports() {
        return new String[]{};
    }
    
    default String[] getWhitelistedCommands() {
        return new String[]{};
    }
    
    default String[] getWhitelistedPaths() {
        return new String[]{};
    }
    
    boolean validateSecurity(File addonFile);
    
    String scanForMalware(File addonFile);
    
    default String getSecurityReport() {
        return "No security warnings";
    }
    
    boolean requiresElevatedPermissions();
    
    default int getThreatLevel() {
        return 0;
    }
    
    default String validateScriptContent(String scriptName, String content, String language) {
        return null;
    }
}