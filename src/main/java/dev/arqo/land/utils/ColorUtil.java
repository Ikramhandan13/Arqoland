package dev.arqo.land.utils;

import org.bukkit.ChatColor;

public class ColorUtil {
    
    public static String translate(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }
    
    public static String stripColor(String text) {
        if (text == null) return "";
        return ChatColor.stripColor(text);
    }
}
