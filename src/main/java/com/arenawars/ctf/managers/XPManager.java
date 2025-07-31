/*
 * ArenaWarsCTF - Professional CTF Plugin
 * Copyright (c) 2025 ArenaWars Development Team - All Rights Reserved
 */

package com.arenawars.ctf.managers;

import com.arenawars.ctf.ArenaWarsCTF;
import com.arenawars.ctf.utils.MessageUtil;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class XPManager {
    
    private final ArenaWarsCTF plugin;
    
    // XP Values
    public static final int XP_KILL = 10;
    public static final int XP_CAPTURE = 50;
    public static final int XP_RETURN = 25;
    public static final int XP_WIN = 100;
    public static final int XP_MVP = 150;
    public static final int XP_PARTICIPATION = 20;
    
    // Level calculation constants
    public static final int BASE_XP = 100;
    public static final double XP_MULTIPLIER = 1.5;
    
    public XPManager(ArenaWarsCTF plugin) {
        this.plugin = plugin;
    }
    
    public void awardKillXP(Player player) {
        awardXP(player, XP_KILL, "Kill");
    }
    
    public void awardCaptureXP(Player player) {
        awardXP(player, XP_CAPTURE, "Flag Capture");
    }
    
    public void awardReturnXP(Player player) {
        awardXP(player, XP_RETURN, "Flag Return");
    }
    
    public void awardWinXP(Player player) {
        awardXP(player, XP_WIN, "Victory");
    }
    
    public void awardMVPXP(Player player) {
        awardXP(player, XP_MVP, "MVP");
        
        // Special MVP announcement
        Map<String, String> placeholders = MessageUtil.createPlaceholders(
            "player", player.getName(),
            "xp", String.valueOf(XP_MVP)
        );
        
        plugin.getMessageUtil().sendTitle(player, "&6&lMVP!", "&eYou earned " + XP_MVP + " bonus XP!");
        plugin.getMessageUtil().playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE);
    }
    
    public void awardParticipationXP(Player player) {
        awardXP(player, XP_PARTICIPATION, "Participation");
    }
    
    private void awardXP(Player player, int amount, String reason) {
        PlayerManager.PlayerData data = plugin.getPlayerManager().getPlayerData(player);
        int oldLevel = calculateLevel(data.experience);
        
        data.experience += amount;
        int newLevel = calculateLevel(data.experience);
        
        // Check for level up
        if (newLevel > oldLevel) {
            handleLevelUp(player, oldLevel, newLevel);
        }
        
        // Show XP gain
        showXPGain(player, amount, reason);
        
        // Save data
        plugin.getPlayerManager().savePlayerData(data);
    }
    
    private void handleLevelUp(Player player, int oldLevel, int newLevel) {
        PlayerManager.PlayerData data = plugin.getPlayerManager().getPlayerData(player);
        
        // Calculate levels gained (in case of multiple level ups)
        for (int level = oldLevel + 1; level <= newLevel; level++) {
            // Award level up rewards
            int coinsReward = level * 10; // 10 coins per level
            
            Map<String, String> placeholders = MessageUtil.createPlaceholders(
                "level", String.valueOf(level),
                "coins", String.valueOf(coinsReward)
            );
            
            plugin.getMessageUtil().sendTitle(player, "&6&lLEVEL UP!", "&eLevel " + level + " reached!");
            plugin.getMessageUtil().sendMessage(player, "&a&l[LEVEL UP] &eYou reached level &6" + level + "&e!");
            plugin.getMessageUtil().sendMessage(player, "&eReward: &6" + coinsReward + " coins");
            
            // Play level up sound
            plugin.getMessageUtil().playSound(player, Sound.ENTITY_PLAYER_LEVELUP);
            
            // Award coins (if you have an economy system)
            data.coins += coinsReward;
        }
    }
    
    private void showXPGain(Player player, int amount, String reason) {
        String message = "&a+" + amount + " XP &7(" + reason + ")";
        plugin.getMessageUtil().sendActionBar(player, message);
        
        // Play XP sound
        plugin.getMessageUtil().playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
    }
    
    public int calculateLevel(int experience) {
        if (experience <= 0) return 1;
        
        int level = 1;
        int requiredXP = BASE_XP;
        int totalXP = 0;
        
        while (totalXP + requiredXP <= experience) {
            totalXP += requiredXP;
            level++;
            requiredXP = (int) (BASE_XP * Math.pow(XP_MULTIPLIER, level - 1));
        }
        
        return level;
    }
    
    public int getXPForLevel(int level) {
        if (level <= 1) return 0;
        
        int totalXP = 0;
        for (int i = 1; i < level; i++) {
            totalXP += (int) (BASE_XP * Math.pow(XP_MULTIPLIER, i - 1));
        }
        return totalXP;
    }
    
    public int getXPToNextLevel(int currentXP) {
        int currentLevel = calculateLevel(currentXP);
        int nextLevelXP = getXPForLevel(currentLevel + 1);
        return nextLevelXP - currentXP;
    }
    
    public int getCurrentLevelProgress(int experience) {
        int currentLevel = calculateLevel(experience);
        int currentLevelXP = getXPForLevel(currentLevel);
        return experience - currentLevelXP;
    }
    
    public int getXPRequiredForCurrentLevel(int experience) {
        int currentLevel = calculateLevel(experience);
        return (int) (BASE_XP * Math.pow(XP_MULTIPLIER, currentLevel - 1));
    }
    
    public String getLevelTitle(int level) {
        if (level >= 100) return "Legendary Warrior";
        if (level >= 75) return "Master Fighter";
        if (level >= 50) return "Veteran";
        if (level >= 25) return "Skilled Player";
        if (level >= 10) return "Experienced";
        if (level >= 5) return "Novice";
        return "Recruit";
    }
    
    public String getLevelColor(int level) {
        if (level >= 100) return "&d"; // Light Purple
        if (level >= 75) return "&5";  // Purple
        if (level >= 50) return "&c";  // Red
        if (level >= 25) return "&6";  // Gold
        if (level >= 10) return "&e";  // Yellow
        if (level >= 5) return "&a";   // Green
        return "&7";                   // Gray
    }
}