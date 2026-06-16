package me.coder.codedsl.libraries;

import java.util.*;

/**
 * Connection library for CodeDSL
 * Allows scripts to communicate with each other
 */
public class ConnectionLibrary implements LibraryRegistry.CodeDSLLibrary {

    private static final Map<String, String> scriptConnections = new HashMap<>();

    @Override
    public String getName() {
        return "connection";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public void onLoad() {
        // Initialize connection library
    }

    @Override
    public void onUnload() {
        // Cleanup connections
        scriptConnections.clear();
    }

    /**
     * Connect to another script file
     */
    public static boolean connectToFile(String fileName) {
        scriptConnections.put(fileName, "connected");
        return true;
    }

    /**
     * Check if file is connected
     */
    public static boolean isConnected(String fileName) {
        return scriptConnections.containsKey(fileName);
    }

    /**
     * Disconnect from script file
     */
    public static void disconnect(String fileName) {
        scriptConnections.remove(fileName);
    }

    /**
     * Send message to another script
     */
    public static void sendMessage(String fileName, String message) {
        if (isConnected(fileName)) {
            // Message would be stored and retrieved by target script
            scriptConnections.put(fileName + ":message", message);
        }
    }

    /**
     * Receive message from another script
     */
    public static String receiveMessage(String fileName) {
        return scriptConnections.remove(fileName + ":message");
    }

    /**
     * Get all connected scripts
     */
    public static Collection<String> getConnectedScripts() {
        List<String> connected = new ArrayList<>();
        for (String key : scriptConnections.keySet()) {
            if (!key.contains(":")) {
                connected.add(key);
            }
        }
        return connected;
    }
}