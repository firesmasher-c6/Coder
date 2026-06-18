package me.coder.codedsl.manager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CommandHandlerStorage {
    
    private static final Map<String, Object> COMMAND_HANDLERS = new ConcurrentHashMap<>();
    private static final Map<String, List<String>> SCRIPT_COMMAND_MAP = new ConcurrentHashMap<>();
    
    public static void registerCommandHandler(String commandName, Object handler, String scriptFile) {
        String key = commandName.toLowerCase();
        COMMAND_HANDLERS.put(key, handler);
        SCRIPT_COMMAND_MAP.computeIfAbsent(scriptFile, k -> new ArrayList<>()).add(key);
        System.out.println("[CodeDSL] Registered command handler: /" + key);
    }
    
    public static boolean executeCommandHandler(String commandName, org.bukkit.command.CommandSender sender, 
                                               me.coder.codedsl.CodeDSLProcessor processor) {
        String key = commandName.toLowerCase();
        Object handler = COMMAND_HANDLERS.get(key);
        
        if (handler == null) {
            sender.sendMessage("§cCommand handler not found: /" + commandName);
            return false;
        }
        
        try {
            if (handler instanceof java.io.File) {
                processor.executeScript((java.io.File) handler, sender);
            }
            return true;
        } catch (Exception e) {
            sender.sendMessage("§cError executing command /" + commandName + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public static void unloadScriptHandlers(String scriptFile) {
        List<String> commands = SCRIPT_COMMAND_MAP.remove(scriptFile);
        if (commands != null) {
            for (String cmd : commands) {
                COMMAND_HANDLERS.remove(cmd);
                System.out.println("[CodeDSL] Unregistered command handler: /" + cmd);
            }
        }
    }
    
    public static boolean hasCommandHandler(String commandName) {
        return COMMAND_HANDLERS.containsKey(commandName.toLowerCase());
    }
    
    public static Set<String> getRegisteredCommands() {
        return new HashSet<>(COMMAND_HANDLERS.keySet());
    }
    
    public static Object getCommandHandler(String commandName) {
        return COMMAND_HANDLERS.get(commandName.toLowerCase());
    }
    
    public static void clearAll() {
        COMMAND_HANDLERS.clear();
        SCRIPT_COMMAND_MAP.clear();
        System.out.println("[CodeDSL] All command handlers cleared");
    }
    
    public static int getRegisteredCommandCount() {
        return COMMAND_HANDLERS.size();
    }
}