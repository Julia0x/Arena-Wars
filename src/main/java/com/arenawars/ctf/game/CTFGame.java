/*
 * ArenaWarsCTF - Professional CTF Plugin
 * Copyright (c) 2025 ArenaWars Development Team - All Rights Reserved
 */

package com.arenawars.ctf.game;

import com.arenawars.ctf.ArenaWarsCTF;
import com.arenawars.ctf.arena.Arena;
import com.arenawars.ctf.arena.ArenaState;
import com.arenawars.ctf.utils.MessageUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class CTFGame {
    
    private final ArenaWarsCTF plugin;
    private final Arena arena;
    private final Map<Team, Integer> scores;
    private final Map<Team, List<Player>> teams;
    private final Map<Team, Location> flagLocations;
    private final Map<Team, Player> flagCarriers;
    private final Set<Player> players;
    
    private int gameTime;
    private int startCountdown;
    private boolean gameStarted;
    private boolean gameEnded;
    
    public CTFGame(ArenaWarsCTF plugin, Arena arena) {
        this.plugin = plugin;
        this.arena = arena;
        this.scores = new HashMap<>();
        this.teams = new HashMap<>();
        this.flagLocations = new HashMap<>();
        this.flagCarriers = new HashMap<>();
        this.players = new HashSet<>();
        
        // Initialize teams
        teams.put(Team.RED, new ArrayList<>());
        teams.put(Team.BLUE, new ArrayList<>());
        
        // Initialize scores
        scores.put(Team.RED, 0);
        scores.put(Team.BLUE, 0);
        
        // Initialize flag locations
        flagLocations.put(Team.RED, arena.getRedFlag());
        flagLocations.put(Team.BLUE, arena.getBlueFlag());
        
        this.gameTime = plugin.getConfigManager().getGameDuration();
        this.startCountdown = 10; // 10 second countdown
        this.gameStarted = false;
        this.gameEnded = false;
        
        arena.setState(ArenaState.WAITING);
    }
    
    public boolean addPlayer(Player player) {
        if (players.size() >= plugin.getConfigManager().getMaxPlayersPerArena()) {
            return false;
        }
        
        players.add(player);
        
        // Balance teams
        Team team = getBalancedTeam();
        teams.get(team).add(player);
        
        // Set player data
        plugin.getPlayerManager().setPlayerArena(player, arena.getName());
        plugin.getPlayerManager().setPlayerTeam(player, team);
        
        // Teleport to waiting lobby
        player.teleport(arena.getWaitingLobby());
        
        // Send join message
        Map<String, String> placeholders = MessageUtil.createPlaceholders(
            "arena", arena.getDisplayName(),
            "team", team.getColoredName()
        );
        plugin.getMessageUtil().sendMessage(player, "game.joined-queue", placeholders);
        plugin.getMessageUtil().sendMessage(player, "game.team-assigned", placeholders);
        
        // Check if we can start the game
        if (players.size() >= plugin.getConfigManager().getMinPlayersToStart()) {
            startGame();
        }
        
        return true;
    }
    
    public void removePlayer(Player player) {
        players.remove(player);
        
        Team team = plugin.getPlayerManager().getPlayerTeam(player);
        if (team != null) {
            teams.get(team).remove(player);
            
            // Drop flag if carrying
            if (flagCarriers.get(team.getOpposite()) == player) {
                dropFlag(player, team.getOpposite());
            }
        }
        
        plugin.getPlayerManager().removePlayerFromArena(player);
        
        // Check if game should end due to insufficient players
        if (gameStarted && players.size() < plugin.getConfigManager().getMinPlayersToStart()) {
            endGame(null); // No winner due to insufficient players
        }
    }
    
    private Team getBalancedTeam() {
        int redSize = teams.get(Team.RED).size();
        int blueSize = teams.get(Team.BLUE).size();
        
        return redSize <= blueSize ? Team.RED : Team.BLUE;
    }
    
    public void startGame() {
        if (gameStarted || arena.getState() == ArenaState.STARTING) return;
        
        arena.setState(ArenaState.STARTING);
        
        // Start countdown
        plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                if (startCountdown > 0) {
                    // Send countdown message
                    Map<String, String> placeholders = MessageUtil.createPlaceholders(
                        "seconds", String.valueOf(startCountdown)
                    );
                    
                    for (Player player : players) {
                        plugin.getMessageUtil().sendMessage(player, "game.game-starting", placeholders);
                        plugin.getMessageUtil().sendTitle(player, "&6&l" + startCountdown, "&eGet ready!");
                    }
                    
                    startCountdown--;
                } else {
                    // Start the actual game
                    actuallyStartGame();
                }
            }
        }, 0L, 20L);
    }
    
    private void actuallyStartGame() {
        gameStarted = true;
        arena.setState(ArenaState.ACTIVE);
        
        // Teleport players to their spawns and give equipment
        for (Team team : Team.values()) {
            List<Player> teamPlayers = teams.get(team);
            List<Location> spawns = team == Team.RED ? arena.getRedSpawns() : arena.getBlueSpawns();
            
            for (int i = 0; i < teamPlayers.size(); i++) {
                Player player = teamPlayers.get(i);
                Location spawn = spawns.get(i % spawns.size()); // Cycle through spawns
                
                player.teleport(spawn);
                givePlayerEquipment(player, team);
                plugin.getPlayerManager().addSpawnProtection(player);
            }
        }
        
        // Send game start message
        for (Player player : players) {
            plugin.getMessageUtil().sendMessage(player, "game.game-started");
            plugin.getMessageUtil().sendTitle(player, "&a&lGAME STARTED!", "&eCapture the enemy flag!");
        }
        
        // Spawn flags
        spawnFlags();
        
        // Start game timer
        startGameTimer();
    }
    
    private void startGameTimer() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                if (!gameStarted || gameEnded) return;
                
                gameTime--;
                
                if (gameTime <= 0) {
                    // Time's up - determine winner by score
                    Team winner = getWinnerByScore();
                    endGame(winner);
                }
            }
        }, 20L, 20L);
    }
    
    private Team getWinnerByScore() {
        int redScore = scores.get(Team.RED);
        int blueScore = scores.get(Team.BLUE);
        
        if (redScore > blueScore) return Team.RED;
        if (blueScore > redScore) return Team.BLUE;
        return null; // Tie
    }
    
    public void update() {
        if (!gameStarted || gameEnded) return;
        
        // Update scoreboards
        plugin.getScoreboardManager().updateGameScoreboard(this);
        
        // Check flag return timers
        // Implementation for flag auto-return would go here
    }
    
    public boolean attemptFlagPickup(Player player, Location location) {
        Team playerTeam = plugin.getPlayerManager().getPlayerTeam(player);
        if (playerTeam == null) return false;
        
        Team oppositeTeam = playerTeam.getOpposite();
        Location flagLocation = flagLocations.get(oppositeTeam);
        
        if (flagLocation == null) return false;
        
        // Check if player is close enough to the flag
        if (location.distance(flagLocation) <= 2.0) {
            // Check if flag is not already taken
            if (flagCarriers.get(oppositeTeam) == null) {
                pickupFlag(player, oppositeTeam);
                return true;
            }
        }
        
        return false;
    }
    
    private void pickupFlag(Player player, Team flagTeam) {
        flagCarriers.put(flagTeam, player);
        flagLocations.put(flagTeam, null); // Flag is now carried
        
        // Give flag item to player
        ItemStack flag = createFlagItem(flagTeam);
        player.getInventory().addItem(flag);
        
        // Broadcast flag taken message
        Map<String, String> placeholders = MessageUtil.createPlaceholders(
            "player", player.getName(),
            "team", flagTeam.getDisplayName(),
            "color", flagTeam.getColorCode()
        );
        
        for (Player gamePlayer : players) {
            plugin.getMessageUtil().sendMessage(gamePlayer, "game.flag-taken", placeholders);
        }
    }
    
    public void dropFlag(Player player, Team flagTeam) {
        if (flagCarriers.get(flagTeam) != player) return;
        
        flagCarriers.put(flagTeam, null);
        flagLocations.put(flagTeam, player.getLocation());
        
        // Remove flag item from inventory
        removeItemFromInventory(player, createFlagItem(flagTeam));
        
        // Broadcast flag dropped message
        Map<String, String> placeholders = MessageUtil.createPlaceholders(
            "player", player.getName(),
            "team", flagTeam.getDisplayName(),
            "color", flagTeam.getColorCode()
        );
        
        for (Player gamePlayer : players) {
            plugin.getMessageUtil().sendMessage(gamePlayer, "game.flag-dropped", placeholders);
        }
        
        // Start flag return timer
        startFlagReturnTimer(flagTeam);
    }
    
    public boolean attemptFlagCapture(Player player, Location location) {
        Team playerTeam = plugin.getPlayerManager().getPlayerTeam(player);
        if (playerTeam == null) return false;
        
        Team oppositeTeam = playerTeam.getOpposite();
        
        // Check if player is carrying the opposite team's flag
        if (flagCarriers.get(oppositeTeam) != player) return false;
        
        // Check if player is at their team's flag return point
        Location returnPoint = playerTeam == Team.RED ? arena.getRedFlagReturn() : arena.getBlueFlagReturn();
        
        if (location.distance(returnPoint) <= 2.0) {
            captureFlag(player, oppositeTeam);
            return true;
        }
        
        return false;
    }
    
    private void captureFlag(Player player, Team flagTeam) {
        Team playerTeam = plugin.getPlayerManager().getPlayerTeam(player);
        
        // Add score
        scores.put(playerTeam, scores.get(playerTeam) + 1);
        
        // Remove flag from player
        flagCarriers.put(flagTeam, null);
        removeItemFromInventory(player, createFlagItem(flagTeam));
        
        // Return flag to original position
        returnFlag(flagTeam);
        
        // Add capture to player stats and award XP
        plugin.getPlayerManager().addCapture(player);
        plugin.getXPManager().awardCaptureXP(player);
        
        // Broadcast capture message
        Map<String, String> placeholders = MessageUtil.createPlaceholders(
            "player", player.getName(),
            "team", flagTeam.getDisplayName(),
            "color", flagTeam.getColorCode(),
            "score", String.valueOf(scores.get(playerTeam))
        );
        
        for (Player gamePlayer : players) {
            plugin.getMessageUtil().sendMessage(gamePlayer, "game.flag-captured", placeholders);
        }
        
        // Check win condition
        if (scores.get(playerTeam) >= plugin.getConfigManager().getCapturesToWin()) {
            endGame(playerTeam);
        }
    }
    
    private void returnFlag(Team flagTeam) {
        Location originalLocation = flagTeam == Team.RED ? arena.getRedFlag() : arena.getBlueFlag();
        flagLocations.put(flagTeam, originalLocation);
        
        // Spawn flag at original location
        spawnFlag(flagTeam, originalLocation);
    }
    
    private void startFlagReturnTimer(Team flagTeam) {
        int returnDelay = plugin.getConfigManager().getFlagReturnDelay();
        
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // Check if flag is still dropped
            if (flagCarriers.get(flagTeam) == null && flagLocations.get(flagTeam) != null) {
                returnFlag(flagTeam);
                
                // Broadcast return message
                Map<String, String> placeholders = MessageUtil.createPlaceholders(
                    "team", flagTeam.getDisplayName(),
                    "color", flagTeam.getColorCode()
                );
                
                for (Player gamePlayer : players) {
                    plugin.getMessageUtil().sendMessage(gamePlayer, "game.flag-returned", placeholders);
                }
            }
        }, returnDelay * 20L);
    }
    
    public void endGame(Team winner) {
        if (gameEnded) return;
        
        gameEnded = true;
        arena.setState(ArenaState.ENDING);
        
        // Calculate and announce MVP
        Player mvp = plugin.getMVPManager().calculateMVP(this, winner);
        
        // Award XP and show victory/defeat screens
        for (Player player : players) {
            Team playerTeam = plugin.getPlayerManager().getPlayerTeam(player);
            
            // Award participation XP to everyone
            plugin.getXPManager().awardParticipationXP(player);
            
            if (winner == null) {
                // Tie or no winner
                plugin.getMessageUtil().sendTitle(player, "&6&lTIE GAME!", "&7Good game everyone!");
            } else if (playerTeam == winner) {
                plugin.getMessageUtil().sendTitle(player, "game.victory-title", "&a&lYou won!");
                plugin.getPlayerManager().addGameWon(player);
                // Award win XP
                plugin.getXPManager().awardWinXP(player);
            } else {
                plugin.getMessageUtil().sendTitle(player, "game.defeat-title", "&c&lYou lost!");
            }
            
            plugin.getPlayerManager().addGamePlayed(player);
        }
        
        // Broadcast winner
        if (winner != null) {
            Map<String, String> placeholders = MessageUtil.createPlaceholders(
                "team", winner.getColoredName()
            );
            
            for (Player player : players) {
                plugin.getMessageUtil().sendMessage(player, "game.team-won", placeholders);
            }
        }
        
        // Show MVP after a short delay
        if (mvp != null) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                plugin.getMVPManager().announceMVP(this, mvp, winner);
            }, 60L); // 3 seconds delay
        }
        
        // Schedule cleanup
        plugin.getServer().getScheduler().runTaskLater(plugin, this::cleanupGame, 200L); // 10 seconds
    }
    
    private void cleanupGame() {
        arena.setState(ArenaState.RESETTING);
        
        // Remove all players from arena
        for (Player player : new HashSet<>(players)) {
            removePlayer(player);
            // Teleport to spawn or lobby
            // Implementation depends on your server setup
        }
        
        // Clear arena if configured
        if (plugin.getConfigManager().isAutoCleanupEnabled()) {
            cleanupArena();
        }
        
        // Reset arena state
        arena.setState(ArenaState.WAITING);
    }
    
    private void cleanupArena() {
        // Implementation for arena cleanup would go here
        // This could involve restoring blocks, clearing entities, etc.
    }
    
    private void spawnFlags() {
        spawnFlag(Team.RED, arena.getRedFlag());
        spawnFlag(Team.BLUE, arena.getBlueFlag());
    }
    
    private void spawnFlag(Team team, Location location) {
        // Implementation for spawning flag entities/blocks would go here
        // This could involve spawning banners, armor stands, or other visual representations
    }
    
    private ItemStack createFlagItem(Team team) {
        Material material = team == Team.RED ? Material.RED_BANNER : Material.BLUE_BANNER;
        ItemStack flag = new ItemStack(material);
        
        // Set custom name and lore
        // Implementation would depend on your preferred flag representation
        
        return flag;
    }
    
    private void removeItemFromInventory(Player player, ItemStack item) {
        player.getInventory().removeItem(item);
    }
    
    private void givePlayerEquipment(Player player, Team team) {
        player.getInventory().clear();
        
        // Give basic CTF equipment based on team
        if (team == Team.RED) {
            // Red team equipment
            player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_SWORD));
            player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.BOW));
            player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.ARROW, 32));
            
            // Red armor
            player.getInventory().setHelmet(new org.bukkit.inventory.ItemStack(org.bukkit.Material.LEATHER_HELMET));
            player.getInventory().setChestplate(new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_CHESTPLATE));
            player.getInventory().setLeggings(new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_LEGGINGS));
            player.getInventory().setBoots(new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_BOOTS));
            
            // Color the leather helmet red
            org.bukkit.inventory.ItemStack helmet = player.getInventory().getHelmet();
            if (helmet != null && helmet.getItemMeta() instanceof org.bukkit.inventory.meta.LeatherArmorMeta) {
                org.bukkit.inventory.meta.LeatherArmorMeta meta = (org.bukkit.inventory.meta.LeatherArmorMeta) helmet.getItemMeta();
                meta.setColor(org.bukkit.Color.RED);
                helmet.setItemMeta(meta);
            }
            
        } else {
            // Blue team equipment
            player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_SWORD));
            player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.BOW));
            player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.ARROW, 32));
            
            // Blue armor
            player.getInventory().setHelmet(new org.bukkit.inventory.ItemStack(org.bukkit.Material.LEATHER_HELMET));
            player.getInventory().setChestplate(new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_CHESTPLATE));
            player.getInventory().setLeggings(new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_LEGGINGS));
            player.getInventory().setBoots(new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_BOOTS));
            
            // Color the leather helmet blue
            org.bukkit.inventory.ItemStack helmet = player.getInventory().getHelmet();
            if (helmet != null && helmet.getItemMeta() instanceof org.bukkit.inventory.meta.LeatherArmorMeta) {
                org.bukkit.inventory.meta.LeatherArmorMeta meta = (org.bukkit.inventory.meta.LeatherArmorMeta) helmet.getItemMeta();
                meta.setColor(org.bukkit.Color.BLUE);
                helmet.setItemMeta(meta);
            }
        }
        
        // Add food
        player.getInventory().addItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.COOKED_BEEF, 16));
    }
    
    // Getters
    public Arena getArena() { return arena; }
    public Set<Player> getPlayers() { return players; }
    public Map<Team, List<Player>> getTeams() { return teams; }
    public Map<Team, Integer> getScores() { return scores; }
    public int getGameTime() { return gameTime; }
    public boolean isGameStarted() { return gameStarted; }
    public boolean isGameEnded() { return gameEnded; }
    public Map<Team, Player> getFlagCarriers() { return flagCarriers; }
}