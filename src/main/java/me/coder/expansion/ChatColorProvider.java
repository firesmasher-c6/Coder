package me.coder.expansion;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.ChatColor;
import com.google.gson.JsonObject;

public class ChatColorProvider {
    
    public static String applyColors(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
    
    public static String removeColors(String text) {
        return ChatColor.stripColor(text);
    }
    
    public static String getColorCode(String colorName) {
        try {
            ChatColor color = ChatColor.valueOf(colorName.toUpperCase());
            return color.toString();
        } catch (IllegalArgumentException e) {
            return "";
        }
    }
    
    public static JsonObject getAvailableColors() {
        JsonObject colors = new JsonObject();
        colors.addProperty("BLACK", "0");
        colors.addProperty("DARK_BLUE", "1");
        colors.addProperty("DARK_GREEN", "2");
        colors.addProperty("DARK_AQUA", "3");
        colors.addProperty("DARK_RED", "4");
        colors.addProperty("DARK_PURPLE", "5");
        colors.addProperty("GOLD", "6");
        colors.addProperty("GRAY", "7");
        colors.addProperty("DARK_GRAY", "8");
        colors.addProperty("BLUE", "9");
        colors.addProperty("GREEN", "a");
        colors.addProperty("AQUA", "b");
        colors.addProperty("RED", "c");
        colors.addProperty("LIGHT_PURPLE", "d");
        colors.addProperty("YELLOW", "e");
        colors.addProperty("WHITE", "f");
        return colors;
    }
    
    public static JsonObject getAvailableFormats() {
        JsonObject formats = new JsonObject();
        formats.addProperty("BOLD", "l");
        formats.addProperty("STRIKETHROUGH", "m");
        formats.addProperty("UNDERLINE", "n");
        formats.addProperty("ITALIC", "o");
        formats.addProperty("RESET", "r");
        return formats;
    }
    
    public static String colorize(String text) {
        return applyColors(text);
    }
    
    public static String reset() {
        return ChatColor.RESET.toString();
    }
    
    public static String gold() {
        return ChatColor.GOLD.toString();
    }
    
    public static String red() {
        return ChatColor.RED.toString();
    }
    
    public static String green() {
        return ChatColor.GREEN.toString();
    }
    
    public static String yellow() {
        return ChatColor.YELLOW.toString();
    }
    
    public static String blue() {
        return ChatColor.BLUE.toString();
    }
    
    public static String aqua() {
        return ChatColor.AQUA.toString();
    }
    
    public static String light_purple() {
        return ChatColor.LIGHT_PURPLE.toString();
    }
    
    public static String bold() {
        return ChatColor.BOLD.toString();
    }
    
    public static String italic() {
        return ChatColor.ITALIC.toString();
    }
    
    public static String underline() {
        return ChatColor.UNDERLINE.toString();
    }
    
    public static String strikethrough() {
        return ChatColor.STRIKETHROUGH.toString();
    }
}