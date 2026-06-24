package me.coder;

import me.coder.manager.ConfigManager;
import org.bukkit.plugin.Plugin;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ErrorLogging {

    /**
     * Log a Java compilation error if logging is enabled
     */
    public static void logCompileError(Plugin plugin, String scriptName, String errorContent) {
        // Get ConfigManager from plugin instance
        ConfigManager configManager = null;
        if (plugin instanceof CoderPlugin) {
            configManager = ((CoderPlugin) plugin).getConfigManager();
        }
        
        // Check if compile error logging is enabled
        if (configManager != null && !configManager.isCompileErrorLoggingEnabled()) {
            return; // Logging disabled, don't log
        }

        File logFolder = new File(plugin.getDataFolder(), "Logs/JavaCompile-Errors");
        
        if (!logFolder.exists()) {
            logFolder.mkdirs();
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String currentDateString = dateFormat.format(new Date());

        int fileIndexNumber = 1;
        File logFile;

        while (true) {
            String fileName = "javafixer-compile-" + currentDateString + "-" + fileIndexNumber + ".txt";
            logFile = new File(logFolder, fileName);
            
            if (!logFile.exists()) {
                break;
            }
            fileIndexNumber++;
        }

        try (FileWriter fileWriter = new FileWriter(logFile);
             PrintWriter printWriter = new PrintWriter(fileWriter)) {

            printWriter.println("=========================================================================");
            printWriter.println(" CODER JAVA COMPILATION ERROR LOG");
            printWriter.println("=========================================================================");
            printWriter.println("Date Stamp:        " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            printWriter.println("Target Asset File: " + scriptName);
            printWriter.println("Assigned File Log: " + logFile.getName());
            printWriter.println("=========================================================================");
            printWriter.println();
            printWriter.println("COMPILER ERROR DETAILS:");
            printWriter.println("-------------------------------------------------------------------------");
            printWriter.println(errorContent);
            printWriter.println("-------------------------------------------------------------------------");
            printWriter.println("END OF COMPILATION ERROR LOG");

            plugin.getLogger().warning("💾 Saved compilation error log to: Logs/JavaCompile-Errors/" + logFile.getName());

        } catch (IOException e) {
            plugin.getLogger().severe("❌ Failed to write error log to disk!");
            e.printStackTrace();
        }
    }

    /**
     * Log a general error if error logging is enabled
     */
    public static void logError(Plugin plugin, String errorMessage, Exception e) {
        // Get ConfigManager from plugin instance
        ConfigManager configManager = null;
        if (plugin instanceof CoderPlugin) {
            configManager = ((CoderPlugin) plugin).getConfigManager();
        }
        
        // Check if error logging is enabled
        if (configManager != null && !configManager.isErrorLoggingEnabled()) {
            return; // Logging disabled, don't log
        }

        File logFolder = new File(plugin.getDataFolder(), "Logs/Error-Logs");
        
        if (!logFolder.exists()) {
            logFolder.mkdirs();
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String currentDateString = dateFormat.format(new Date());

        int fileIndexNumber = 1;
        File logFile;

        while (true) {
            String fileName = "error-" + currentDateString + "-" + fileIndexNumber + ".txt";
            logFile = new File(logFolder, fileName);
            
            if (!logFile.exists()) {
                break;
            }
            fileIndexNumber++;
        }

        try (FileWriter fileWriter = new FileWriter(logFile);
             PrintWriter printWriter = new PrintWriter(fileWriter)) {

            printWriter.println("=========================================================================");
            printWriter.println(" CODER ERROR LOG");
            printWriter.println("=========================================================================");
            printWriter.println("Date Stamp:        " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            printWriter.println("Assigned File Log: " + logFile.getName());
            printWriter.println("=========================================================================");
            printWriter.println();
            printWriter.println("ERROR DETAILS:");
            printWriter.println("-------------------------------------------------------------------------");
            printWriter.println(errorMessage);
            printWriter.println("-------------------------------------------------------------------------");
            if (e != null) {
                printWriter.println();
                printWriter.println("EXCEPTION:");
                printWriter.println("-------------------------------------------------------------------------");
                e.printStackTrace(printWriter);
                printWriter.println("-------------------------------------------------------------------------");
            }
            printWriter.println("END OF ERROR LOG");

            plugin.getLogger().warning("💾 Saved error log to: Logs/Error-Logs/" + logFile.getName());

        } catch (IOException ex) {
            plugin.getLogger().severe("❌ Failed to write error log to disk!");
            ex.printStackTrace();
        }
    }
}