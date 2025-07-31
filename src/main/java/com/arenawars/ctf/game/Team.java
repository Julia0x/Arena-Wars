/*
 * ArenaWarsCTF - Professional CTF Plugin
 * Copyright (c) 2025 ArenaWars Development Team - All Rights Reserved
 */

package com.arenawars.ctf.game;

import com.arenawars.ctf.managers.ColorManager;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;

public enum Team {
    RED(ChatColor.RED, "Red Team", "c"),
    BLUE(ChatColor.BLUE, "Blue Team", "9");
    
    private final ChatColor chatColor;
    private final String displayName;
    private final String colorCode;
    private static final ColorManager colorManager = new ColorManager();
    
    Team(ChatColor chatColor, String displayName, String colorCode) {
        this.chatColor = chatColor;
        this.displayName = displayName;
        this.colorCode = colorCode;
    }
    
    public ChatColor getChatColor() {
        return chatColor;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getColorCode() {
        return colorCode;
    }
    
    public String getColoredName() {
        return "&" + colorCode + displayName;
    }
    
    public Component getColoredNameComponent() {
        return colorManager.getTeamColor(displayName, colorCode);
    }
    
    public String getColoredPrefix() {
        return "&" + colorCode + name().toUpperCase();
    }
    
    public Component getColoredPrefixComponent() {
        return colorManager.getTeamColor(name().toUpperCase(), colorCode);
    }
    
    public Team getOpposite() {
        return this == RED ? BLUE : RED;
    }
    
    // Helper methods for common team operations
    public String getColoredDisplayName() {
        return getColoredName();
    }
    
    public String getShortColoredName() {
        return "&" + colorCode + name().charAt(0);
    }
    
    public Component getShortColoredNameComponent() {
        return colorManager.getTeamColor(String.valueOf(name().charAt(0)), colorCode);
    }
}