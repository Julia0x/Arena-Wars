/*
 * ArenaWarsCTF - Professional CTF Plugin
 * Copyright (c) 2025 ArenaWars Development Team - All Rights Reserved
 */

package com.arenawars.ctf.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Centralized color management system for the ArenaWarsCTF plugin.
 * Handles all color code processing, conversion, and formatting consistently.
 */
public class ColorManager {
    
    private final LegacyComponentSerializer ampersandSerializer;
    private final LegacyComponentSerializer sectionSerializer;
    
    // Hex color pattern for modern Minecraft versions
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    
    public ColorManager() {
        this.ampersandSerializer = LegacyComponentSerializer.legacyAmpersand();
        this.sectionSerializer = LegacyComponentSerializer.legacySection();
    }
    
    /**
     * Converts color codes to Adventure Component (for modern messaging)
     */
    public Component colorize(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        
        // Process hex colors first
        String processedText = processHexColors(text);
        
        // Convert section symbols to ampersand for consistent processing
        processedText = processedText.replace('§', '&');
        
        return ampersandSerializer.deserialize(processedText);
    }
    
    /**
     * Converts color codes to legacy ChatColor format (for older systems)
     */
    public String toLegacy(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        // Process hex colors first
        String processedText = processHexColors(text);
        
        // Convert ampersand to section symbols
        processedText = processedText.replace('&', '§');
        
        return ChatColor.translateAlternateColorCodes('§', processedText);
    }
    
    /**
     * Strips all color codes from text
     */
    public String stripColors(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        // Remove hex colors
        String stripped = HEX_PATTERN.matcher(text).replaceAll("");
        
        // Remove legacy color codes
        stripped = stripped.replaceAll("[&§][0-9a-fk-orA-FK-OR]", "");
        
        return stripped;
    }
    
    /**
     * Colors an ItemStack's display name and lore
     */
    public ItemStack colorizeItem(ItemStack item, String displayName, List<String> lore) {
        if (item == null) return null;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        
        if (displayName != null && !displayName.isEmpty()) {
            meta.setDisplayName(toLegacy(displayName));
        }
        
        if (lore != null && !lore.isEmpty()) {
            List<String> coloredLore = lore.stream()
                .map(this::toLegacy)
                .collect(Collectors.toList());
            meta.setLore(coloredLore);
        }
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Creates a colored prefix for messages
     */
    public String createPrefix(String prefix) {
        return colorize(prefix).toString();
    }
    
    /**
     * Processes color codes for scoreboard entries (legacy format required)
     */
    public String colorizeForScoreboard(String text) {
        return toLegacy(text);
    }
    
    /**
     * Processes color codes for chat messages (Adventure format)
     */
    public Component colorizeForChat(String text) {
        return colorize(text);
    }
    
    /**
     * Processes color codes for titles and subtitles
     */
    public Component colorizeForTitle(String text) {
        return colorize(text);
    }
    
    /**
     * Processes color codes for action bar messages
     */
    public Component colorizeForActionBar(String text) {
        return colorize(text);
    }
    
    /**
     * Processes color codes for boss bar titles
     */
    public Component colorizeForBossBar(String text) {
        return colorize(text);
    }
    
    /**
     * Gets team color in Adventure Component format
     */
    public Component getTeamColor(String teamName, String colorCode) {
        return colorize("&" + colorCode + teamName);
    }
    
    /**
     * Gets level color based on level value
     */
    public String getLevelColor(int level) {
        if (level >= 100) return "&d"; // Light Purple
        if (level >= 75) return "&5";  // Purple
        if (level >= 50) return "&c";  // Red
        if (level >= 25) return "&6";  // Gold
        if (level >= 10) return "&e";  // Yellow
        if (level >= 5) return "&a";   // Green
        return "&7";                   // Gray
    }
    
    /**
     * Processes hex color codes for modern Minecraft support
     */
    private String processHexColors(String text) {
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        
        while (matcher.find()) {
            String hexCode = matcher.group(1);
            matcher.appendReplacement(buffer, "§x§" + 
                hexCode.charAt(0) + "§" + hexCode.charAt(1) + "§" +
                hexCode.charAt(2) + "§" + hexCode.charAt(3) + "§" +
                hexCode.charAt(4) + "§" + hexCode.charAt(5));
        }
        
        matcher.appendTail(buffer);
        return buffer.toString();
    }
    
    /**
     * Format a message with placeholders and colors
     */
    public Component formatMessage(String message, Object... placeholders) {
        String formatted = message;
        
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                String placeholder = placeholders[i].toString();
                String value = placeholders[i + 1].toString();
                formatted = formatted.replace("{" + placeholder + "}", value);
            }
        }
        
        return colorize(formatted);
    }
    
    /**
     * Common color constants for easy access
     */
    public static class Colors {
        public static final String PRIMARY = "&6";      // Gold
        public static final String SECONDARY = "&e";    // Yellow
        public static final String SUCCESS = "&a";      // Green
        public static final String ERROR = "&c";        // Red
        public static final String WARNING = "&6";      // Gold
        public static final String INFO = "&b";         // Aqua
        public static final String NEUTRAL = "&7";      // Gray
        public static final String WHITE = "&f";        // White
        public static final String BLACK = "&0";        // Black
        public static final String DARK_GRAY = "&8";    // Dark Gray
        
        // Team colors
        public static final String RED_TEAM = "&c";     // Red
        public static final String BLUE_TEAM = "&9";    // Blue
        
        // Special effects
        public static final String BOLD = "&l";
        public static final String ITALIC = "&o";
        public static final String UNDERLINE = "&n";
        public static final String STRIKETHROUGH = "&m";
        public static final String RESET = "&r";
    }
    
    /**
     * Common message formats
     */
    public Component createSuccessMessage(String message) {
        return colorize(Colors.SUCCESS + message);
    }
    
    public Component createErrorMessage(String message) {
        return colorize(Colors.ERROR + message);
    }
    
    public Component createWarningMessage(String message) {
        return colorize(Colors.WARNING + message);
    }
    
    public Component createInfoMessage(String message) {
        return colorize(Colors.INFO + message);
    }
    
    public Component createPrefixedMessage(String prefix, String message) {
        return colorize(prefix + " " + Colors.NEUTRAL + message);
    }
}