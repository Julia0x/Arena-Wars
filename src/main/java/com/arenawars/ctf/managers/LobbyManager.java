/*
 * ArenaWarsCTF - Professional CTF Plugin
 * Copyright (c) 2025 ArenaWars Development Team - All Rights Reserved
 */

package com.arenawars.ctf.managers;

import com.arenawars.ctf.ArenaWarsCTF;
import com.arenawars.ctf.arena.Arena;
import com.arenawars.ctf.arena.ArenaState;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LobbyManager {
    
    private final ArenaWarsCTF plugin;
    private final Map<Player, Scoreboard> lobbyScoreboards;
    private final LegacyComponentSerializer serializer;
    
    public LobbyManager(ArenaWarsCTF plugin) {
        this.plugin = plugin;
        this.lobbyScoreboards = new HashMap<>();
        this.serializer = LegacyComponentSerializer.legacyAmpersand();
    }
    
    public void createLobbyScoreboard(Player player) {
        // Remove existing scoreboard
        removeLobbyScoreboard(player);
        org.bukkit.scoreboard.ScoreboardManager bukkitManager = Bukkit.getScoreboardManager();
        Scoreboard scoreboard = bukkitManager.getNewScoreboard();
        
        Objective objective = scoreboard.registerNewObjective(
            "ctf_lobby", 
            "dummy", 
            serializer.deserialize("&6&lArenaWars &e&lCTF")
        );
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        
        // Set scoreboard for player
        player.setScoreboard(scoreboard);
        lobbyScoreboards.put(player, scoreboard);
        
        // Update content
        updateLobbyScoreboard(player);
    }
    
    public void updateLobbyScoreboard(Player player) {
        Scoreboard scoreboard = lobbyScoreboards.get(player);
        if (scoreboard == null) {
            createLobbyScoreboard(player);
            return;
        }
        
        Objective objective = scoreboard.getObjective("ctf_lobby");
        if (objective == null) return;
        
        // Clear existing scores
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }
        
        PlayerManager.PlayerData data = plugin.getPlayerManager().getPlayerData(player);
        int level = plugin.getXPManager().calculateLevel(data.experience);
        String levelColor = plugin.getXPManager().getLevelColor(level);
        String levelTitle = plugin.getXPManager().getLevelTitle(level);
        
        int line = 15;
        
        // Header
        objective.getScore(" ").setScore(line--);
        
        // Player Level and Title
        objective.getScore("&7Level: " + levelColor + level + " &8[" + levelTitle + "]").setScore(line--);
        
        // XP Progress
        int currentXP = plugin.getXPManager().getCurrentLevelProgress(data.experience);
        int requiredXP = plugin.getXPManager().getXPRequiredForCurrentLevel(data.experience);
        String xpBar = createXPBar(currentXP, requiredXP);
        objective.getScore("&7XP: " + xpBar).setScore(line--);
        objective.getScore("&8" + currentXP + "/" + requiredXP + " XP").setScore(line--);
        
        // Empty line
        objective.getScore("  ").setScore(line--);
        
        // Server Stats
        objective.getScore("&6&lServer Stats").setScore(line--);
        objective.getScore("&7Active Games: &e" + plugin.getGameManager().getActiveGameCount()).setScore(line--);
        objective.getScore("&7Players Online: &a" + plugin.getGameManager().getTotalPlayers()).setScore(line--);
        
        // Empty line
        objective.getScore("   ").setScore(line--);
        
        // Available Arenas
        objective.getScore("&a&lAvailable Arenas").setScore(line--);
        
        List<Arena> availableArenas = getAvailableArenas();
        if (availableArenas.isEmpty()) {
            objective.getScore("&7No arenas available").setScore(line--);
        } else {
            int shown = 0;
            for (Arena arena : availableArenas) {
                if (shown >= 3) break; // Show max 3 arenas
                
                String status = getArenaStatusShort(arena);
                objective.getScore("&e" + arena.getDisplayName() + " " + status).setScore(line--);
                shown++;
            }
            
            if (availableArenas.size() > 3) {
                objective.getScore("&7... and " + (availableArenas.size() - 3) + " more").setScore(line--);
            }
        }
        
        // Empty line
        objective.getScore("    ").setScore(line--);
        
        // Player Personal Stats
        objective.getScore("&b&lYour Stats").setScore(line--);
        objective.getScore("&7Games: &e" + data.gamesPlayed + " &7Wins: &a" + data.gamesWon).setScore(line--);
        objective.getScore("&7Win Rate: &6" + String.format("%.1f%%", data.getWinRate())).setScore(line--);
        objective.getScore("&7K/D: &c" + String.format("%.2f", data.getKDRatio()) + " &7Captures: &e" + data.captures).setScore(line--);
        
        // Empty line
        objective.getScore("     ").setScore(line--);
        
        // Quick Join Instructions
        objective.getScore("&7Type &e/ctf join &7to play!").setScore(line--);
        
        // Empty line
        objective.getScore("      ").setScore(line--);
        
        // Footer
        objective.getScore("&ewww.arenawars.com").setScore(line--);
    }
    
    private String createXPBar(int current, int required) {
        int barLength = 10;
        double percentage = (double) current / required;
        int filled = (int) (percentage * barLength);
        
        StringBuilder bar = new StringBuilder("&8[");
        
        for (int i = 0; i < barLength; i++) {
            if (i < filled) {
                bar.append("&a■");
            } else {
                bar.append("&7■");
            }
        }
        
        bar.append("&8]");
        return bar.toString();
    }
    
    private List<Arena> getAvailableArenas() {
        return plugin.getArenaManager().getAllArenas().values().stream()
            .filter(arena -> arena.isEnabled() && arena.getState() == ArenaState.WAITING)
            .limit(5) // Limit for scoreboard space
            .toList();
    }
    
    private String getArenaStatusShort(Arena arena) {
        switch (arena.getState()) {
            case WAITING:
                // Get player count if possible
                int playerCount = 0; // You'd get this from game manager
                return "&7(" + playerCount + "/8)";
            case STARTING:
                return "&eStarting";
            case ACTIVE:
                return "&cIn Game";
            default:
                return "&cOffline";
        }
    }
    
    public void updateAllLobbyScoreboards() {
        for (Player player : new HashMap<>(lobbyScoreboards).keySet()) {
            if (player.isOnline() && !plugin.getPlayerManager().isInArena(player)) {
                updateLobbyScoreboard(player);
            } else {
                removeLobbyScoreboard(player);
            }
        }
    }
    
    public void removeLobbyScoreboard(Player player) {
        Scoreboard scoreboard = lobbyScoreboards.remove(player);
        if (scoreboard != null) {
            // Don't reset to main scoreboard here, let other managers handle it
        }
    }
    
    public void removeAllLobbyScoreboards() {
        for (Player player : new HashMap<>(lobbyScoreboards).keySet()) {
            removeLobbyScoreboard(player);
        }
    }
    
    public boolean hasLobbyScoreboard(Player player) {
        return lobbyScoreboards.containsKey(player);
    }
}