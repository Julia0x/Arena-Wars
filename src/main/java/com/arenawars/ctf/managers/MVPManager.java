/*
 * ArenaWarsCTF - Professional CTF Plugin
 * Copyright (c) 2025 ArenaWars Development Team - All Rights Reserved
 */

package com.arenawars.ctf.managers;

import com.arenawars.ctf.ArenaWarsCTF;
import com.arenawars.ctf.game.CTFGame;
import com.arenawars.ctf.game.Team;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class MVPManager {
    
    private final ArenaWarsCTF plugin;
    private final LegacyComponentSerializer serializer;
    
    public MVPManager(ArenaWarsCTF plugin) {
        this.plugin = plugin;
        this.serializer = LegacyComponentSerializer.legacyAmpersand();
    }
    
    public Player calculateMVP(CTFGame game, Team winningTeam) {
        if (winningTeam == null) return null;
        
        Player mvp = null;
        double highestScore = 0;
        
        for (Player player : game.getTeams().get(winningTeam)) {
            double score = calculateMVPScore(player);
            if (score > highestScore) {
                highestScore = score;
                mvp = player;
            }
        }
        
        return mvp;
    }
    
    public void announceMVP(CTFGame game, Player mvp, Team winningTeam) {
        if (mvp == null) return;
        
        // Award MVP XP
        plugin.getXPManager().awardMVPXP(mvp);
        
        // Get MVP stats for display
        PlayerManager.PlayerData mvpData = plugin.getPlayerManager().getPlayerData(mvp);
        GameStats gameStats = getGameStats(mvp);
        int level = plugin.getXPManager().calculateLevel(mvpData.experience);
        String levelColor = plugin.getXPManager().getLevelColor(level);
        
        // Create title components
        Component mvpTitle = serializer.deserialize("&6&lMVP");
        Component mvpSubtitle = serializer.deserialize(levelColor + mvp.getName() + " &7(" + winningTeam.getColoredName() + "&7)");
        
        // Show MVP title to all players in the game
        for (Player player : game.getPlayers()) {
            // Show main MVP title
            Title title = Title.title(
                mvpTitle,
                mvpSubtitle,
                Title.Times.times(
                    Duration.ofMillis(500),  // fade in
                    Duration.ofSeconds(4),   // stay
                    Duration.ofMillis(1000)  // fade out
                )
            );
            
            player.showTitle(title);
            
            // Play MVP sound
            if (plugin.getConfigManager().areSoundsEnabled()) {
                if (player.equals(mvp)) {
                    // Special sound for MVP
                    player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                } else {
                    // Regular sound for others
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
                }
            }
        }
        
        // Schedule detailed stats display after title
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            showDetailedMVPStats(game, mvp, winningTeam, gameStats);
        }, 100L); // 5 seconds after title
    }
    
    private void showDetailedMVPStats(CTFGame game, Player mvp, Team winningTeam, GameStats stats) {
        // Create detailed MVP stats title
        Component statsTitle = serializer.deserialize("&6&lMOST VALUABLE PLAYER");
        
        // Create detailed subtitle with stats
        String statsText = String.format("&7K: &a%d &7D: &c%d &7C: &e%d &7R: &b%d &6(+%d XP)", 
            stats.kills, stats.deaths, stats.captures, stats.returns, XPManager.XP_MVP);
        Component statsSubtitle = serializer.deserialize(statsText);
        
        // Show detailed stats title
        for (Player player : game.getPlayers()) {
            Title detailTitle = Title.title(
                statsTitle,
                statsSubtitle,
                Title.Times.times(
                    Duration.ofMillis(300),  // fade in
                    Duration.ofSeconds(3),   // stay
                    Duration.ofMillis(700)   // fade out
                )
            );
            
            player.showTitle(detailTitle);
            
            // Send detailed chat message as well
            sendMVPChatStats(player, mvp, winningTeam, stats);
        }
    }
    
    private void sendMVPChatStats(Player recipient, Player mvp, Team winningTeam, GameStats stats) {
        plugin.getMessageUtil().sendRawMessage(recipient, "");
        plugin.getMessageUtil().sendRawMessage(recipient, "&6&l┌──────────────────────────┐");
        plugin.getMessageUtil().sendRawMessage(recipient, "&6&l│    &e&lMOST VALUABLE PLAYER    &6&l│");
        plugin.getMessageUtil().sendRawMessage(recipient, "&6&l│                           &6&l│");
        
        PlayerManager.PlayerData data = plugin.getPlayerManager().getPlayerData(mvp);
        int level = plugin.getXPManager().calculateLevel(data.experience);
        String levelColor = plugin.getXPManager().getLevelColor(level);
        
        String playerLine = String.format("&6&l│  %s[%d] %s &7(%s&7)  &6&l│", 
            levelColor, level, mvp.getName(), winningTeam.getColoredName());
        plugin.getMessageUtil().sendRawMessage(recipient, playerLine);
        
        plugin.getMessageUtil().sendRawMessage(recipient, "&6&l│                           &6&l│");
        
        String statsLine1 = String.format("&6&l│  &7Kills: &a%-2d  &7Captures: &e%-2d  &6&l│", stats.kills, stats.captures);
        String statsLine2 = String.format("&6&l│  &7Deaths: &c%-2d &7Returns: &b%-2d   &6&l│", stats.deaths, stats.returns);
        
        plugin.getMessageUtil().sendRawMessage(recipient, statsLine1);
        plugin.getMessageUtil().sendRawMessage(recipient, statsLine2);
        
        plugin.getMessageUtil().sendRawMessage(recipient, "&6&l│                           &6&l│");
        plugin.getMessageUtil().sendRawMessage(recipient, "&6&l│      &eBonus: &6+" + XPManager.XP_MVP + " XP       &6&l│");
        plugin.getMessageUtil().sendRawMessage(recipient, "&6&l└──────────────────────────┘");
        plugin.getMessageUtil().sendRawMessage(recipient, "");
    }
    
    private double calculateMVPScore(Player player) {
        GameStats stats = getGameStats(player);
        
        // MVP Score calculation (weighted values)
        double score = 0;
        
        // Captures are most valuable
        score += stats.captures * 10.0;
        
        // Returns are important for defense
        score += stats.returns * 6.0;
        
        // Kills matter but less than objectives
        score += stats.kills * 3.0;
        
        // Deaths reduce score
        score -= stats.deaths * 1.5;
        
        // K/D ratio bonus
        if (stats.deaths > 0) {
            double kd = (double) stats.kills / stats.deaths;
            if (kd > 1.0) {
                score += (kd - 1.0) * 2.0; // Bonus for positive K/D
            }
        } else if (stats.kills > 0) {
            score += stats.kills * 2.0; // Bonus for no deaths
        }
        
        // Objective ratio bonus (captures + returns vs deaths)
        int objectives = stats.captures + stats.returns;
        if (objectives > 0 && stats.deaths > 0) {
            double objRatio = (double) objectives / stats.deaths;
            score += objRatio * 3.0;
        }
        
        return Math.max(0, score); // Ensure non-negative score
    }
    
    private GameStats getGameStats(Player player) {
        // This would track game-specific stats rather than overall stats
        // For now, we'll use a simplified version with overall stats
        PlayerManager.PlayerData data = plugin.getPlayerManager().getPlayerData(player);
        
        GameStats stats = new GameStats();
        // In a real implementation, you'd track per-game stats
        // For now, using recent activity approximation
        stats.kills = Math.min(data.kills, 20); // Reasonable game maximum
        stats.deaths = Math.min(data.deaths, 10);
        stats.captures = Math.min(data.captures, 5);
        stats.returns = Math.min(data.returns, 10);
        
        return stats;
    }
    
    public static class GameStats {
        public int kills = 0;
        public int deaths = 0;
        public int captures = 0;
        public int returns = 0;
    }
}