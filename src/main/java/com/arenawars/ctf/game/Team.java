/*
 * ArenaWarsCTF - Professional CTF Plugin
 * Copyright (c) 2025 ArenaWars Development Team - All Rights Reserved
 */

package com.arenawars.ctf.game;

import org.bukkit.ChatColor;

public enum Team {
    RED(ChatColor.RED, "Red Team", "c"),
    BLUE(ChatColor.BLUE, "Blue Team", "9");
    
    private final ChatColor chatColor;
    private final String displayName;
    private final String colorCode;
    
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
    
    public Team getOpposite() {
        return this == RED ? BLUE : RED;
    }
}