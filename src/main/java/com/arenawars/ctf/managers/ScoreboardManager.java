/*
 * ArenaWarsCTF - Professional CTF Plugin
 * Copyright (c) 2025 ArenaWars Development Team - All Rights Reserved
 */

package com.arenawars.ctf.managers;

import com.arenawars.ctf.ArenaWarsCTF;
import com.arenawars.ctf.game.CTFGame;
import com.arenawars.ctf.game.Team;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.Map;

public class ScoreboardManager {
    
    private final ArenaWarsCTF plugin;
    private final Map<Player, Scoreboard> playerScoreboards;
    private final Map<Player, BossBar> playerBossBars;
    private final ColorManager colorManager;
    
    public ScoreboardManager(ArenaWarsCTF plugin) {
        this.plugin = plugin;
        this.playerScoreboards = new HashMap<>();
        this.playerBossBars = new HashMap<>();
        this.colorManager = new ColorManager();
    }
    
    public void createGameScoreboard(Player player, CTFGame game) {
        // Remove existing scoreboard and boss bar
        removeScoreboard(player);
        
        // Create new scoreboard
        org.bukkit.scoreboard.ScoreboardManager bukkitManager = Bukkit.getScoreboardManager();
        Scoreboard scoreboard = bukkitManager.getNewScoreboard();
        
        Objective objective = scoreboard.registerNewObjective(
            "ctf_game", 
            "dummy", 
            colorManager.colorize("&6&lArenaWars CTF")
        );
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        
        // Set scoreboard for player
        player.setScoreboard(scoreboard);
        playerScoreboards.put(player, scoreboard);
        
        // Create boss bar
        createGameBossBar(player, game);
        
        // Update scoreboard content
        updateGameScoreboard(game);
    }
    
    public void updateGameScoreboard(CTFGame game) {
        for (Player player : game.getPlayers()) {
            Scoreboard scoreboard = playerScoreboards.get(player);
            if (scoreboard == null) {
                createGameScoreboard(player, game);
                continue;
            }
            
            Objective objective = scoreboard.getObjective("ctf_game");
            if (objective == null) continue;
            
            // Clear existing scores
            for (String entry : scoreboard.getEntries()) {
                scoreboard.resetScores(entry);
            }
            
            Team playerTeam = plugin.getPlayerManager().getPlayerTeam(player);
            Map<Team, Integer> scores = game.getScores();
            
            // Build scoreboard content
            int line = 15;
            
            // Empty line
            objective.getScore(" ").setScore(line--);
            
            // Arena name
            String arenaLine = colorManager.colorizeForScoreboard("&7Arena: &e" + game.getArena().getDisplayName());
            objective.getScore(arenaLine).setScore(line--);
            
            // Empty line
            objective.getScore("  ").setScore(line--);
            
            // Team scores
            String redScoreLine = colorManager.colorizeForScoreboard("&c&lRed Team: &f" + scores.get(Team.RED));
            String blueScoreLine = colorManager.colorizeForScoreboard("&9&lBlue Team: &f" + scores.get(Team.BLUE));
            objective.getScore(redScoreLine).setScore(line--);
            objective.getScore(blueScoreLine).setScore(line--);
            
            // Empty line
            objective.getScore("   ").setScore(line--);
            
            // Your team
            if (playerTeam != null) {
                String teamLine = colorManager.colorizeForScoreboard("&7Your Team: " + playerTeam.getColoredName());
                objective.getScore(teamLine).setScore(line--);
            }
            
            // Empty line
            objective.getScore("    ").setScore(line--);
            
            // Game time
            int minutes = game.getGameTime() / 60;
            int seconds = game.getGameTime() % 60;
            String timeLine = colorManager.colorizeForScoreboard("&7Time: &e" + String.format("%02d:%02d", minutes, seconds));
            objective.getScore(timeLine).setScore(line--);
            
            // Empty line
            objective.getScore("     ").setScore(line--);
            
            // Player stats
            PlayerManager.PlayerData data = plugin.getPlayerManager().getPlayerData(player);
            String killsLine = colorManager.colorizeForScoreboard("&7Kills: &a" + data.kills);
            String deathsLine = colorManager.colorizeForScoreboard("&7Deaths: &c" + data.deaths);
            String capturesLine = colorManager.colorizeForScoreboard("&7Captures: &6" + data.captures);
            
            objective.getScore(killsLine).setScore(line--);
            objective.getScore(deathsLine).setScore(line--);
            objective.getScore(capturesLine).setScore(line--);
            
            // Empty line
            objective.getScore("      ").setScore(line--);
            
            // Website/branding
            String brandingLine = colorManager.colorizeForScoreboard("&ewww.arenawars.com");
            objective.getScore(brandingLine).setScore(line--);
        }
    }
    
    private void createGameBossBar(Player player, CTFGame game) {
        Component title = colorManager.colorizeForBossBar("&6ArenaWars CTF - Preparing...");
        
        BossBar bossBar = BossBar.bossBar(
            title,
            1.0f,
            BossBar.Color.YELLOW,
            BossBar.Overlay.PROGRESS
        );
        
        player.showBossBar(bossBar);
        playerBossBars.put(player, bossBar);
    }
    
    public void updateAllScoreboards() {
        for (CTFGame game : plugin.getGameManager().getActiveGames().values()) {
            updateGameScoreboard(game);
            updateGameBossBar(game);
        }
    }
    
    private void updateGameBossBar(CTFGame game) {
        Map<Team, Integer> scores = game.getScores();
        int redScore = scores.get(Team.RED);
        int blueScore = scores.get(Team.BLUE);
        int capturesToWin = plugin.getConfigManager().getCapturesToWin();
        
        for (Player player : game.getPlayers()) {
            BossBar bossBar = playerBossBars.get(player);
            if (bossBar == null) continue;
            
            // Update boss bar content based on game state
            if (!game.isGameStarted()) {
                // Waiting/starting
                String waitingText = "&eWaiting for players... (" + game.getPlayers().size() + "/" + plugin.getConfigManager().getMaxPlayersPerArena() + ")";
                bossBar.name(colorManager.colorizeForBossBar(waitingText));
                bossBar.progress(1.0f);
                bossBar.color(BossBar.Color.YELLOW);
            } else if (game.isGameEnded()) {
                // Game ended
                bossBar.name(colorManager.colorizeForBossBar("&6Game Ended!"));
                bossBar.progress(0.0f);
                bossBar.color(BossBar.Color.WHITE);
            } else {
                // Active game
                String bossBarText = String.format("&c&lRed: %d &f| &9&lBlue: %d &f| &7Time: %02d:%02d", 
                    redScore, blueScore,
                    game.getGameTime() / 60, game.getGameTime() % 60);
                
                bossBar.name(colorManager.colorizeForBossBar(bossBarText));
                
                // Progress based on highest team score
                int maxScore = Math.max(redScore, blueScore);
                float progress = Math.min((float) maxScore / capturesToWin, 1.0f);
                bossBar.progress(progress);
                
                // Color based on leading team
                if (redScore > blueScore) {
                    bossBar.color(BossBar.Color.RED);
                } else if (blueScore > redScore) {
                    bossBar.color(BossBar.Color.BLUE);
                } else {
                    bossBar.color(BossBar.Color.WHITE);
                }
            }
        }
    }
    
    public void removeScoreboard(Player player) {
        // Remove scoreboard
        Scoreboard scoreboard = playerScoreboards.remove(player);
        if (scoreboard != null) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
        
        // Remove boss bar
        BossBar bossBar = playerBossBars.remove(player);
        if (bossBar != null) {
            player.hideBossBar(bossBar);
        }
    }
    
    public void removeAllScoreboards() {
        for (Player player : new HashMap<>(playerScoreboards).keySet()) {
            removeScoreboard(player);
        }
    }
}