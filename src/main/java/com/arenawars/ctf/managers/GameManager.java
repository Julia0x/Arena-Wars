/*
 * ArenaWarsCTF - Professional CTF Plugin
 * Copyright (c) 2025 ArenaWars Development Team - All Rights Reserved
 */

package com.arenawars.ctf.managers;

import com.arenawars.ctf.ArenaWarsCTF;
import com.arenawars.ctf.arena.Arena;
import com.arenawars.ctf.arena.ArenaState;
import com.arenawars.ctf.game.CTFGame;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class GameManager {
    
    private final ArenaWarsCTF plugin;
    private final Map<String, CTFGame> activeGames; // Arena name -> Game
    
    public GameManager(ArenaWarsCTF plugin) {
        this.plugin = plugin;
        this.activeGames = new HashMap<>();
    }
    
    public boolean joinGame(Player player, String arenaName) {
        // Check if player is already in a game
        if (plugin.getPlayerManager().isInArena(player)) {
            plugin.getMessageUtil().sendMessage(player, "error.already-in-game");
            return false;
        }
        
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        if (arena == null) {
            plugin.getMessageUtil().sendMessage(player, "arena.not-found", 
                Map.of("arena", arenaName));
            return false;
        }
        
        if (!arena.isEnabled()) {
            plugin.getMessageUtil().sendMessage(player, "error.invalid-arena");
            return false;
        }
        
        // Get or create game for this arena
        CTFGame game = getOrCreateGame(arena);
        
        if (game == null) {
            plugin.getMessageUtil().sendMessage(player, "error.arena-full");
            return false;
        }
        
        if (game.addPlayer(player)) {
            // Update tab list for player joining arena
            plugin.getTabListManager().onPlayerJoinArena(player, arenaName);
            plugin.getTabListManager().setTabListHeader(player);
            return true;
        }
        return false;
    }
    
    public boolean joinRandomGame(Player player) {
        // Check if player is already in a game
        if (plugin.getPlayerManager().isInArena(player)) {
            plugin.getMessageUtil().sendMessage(player, "error.already-in-game");
            return false;
        }
        
        // Find an available arena
        Arena availableArena = plugin.getArenaManager().getAvailableArena();
        if (availableArena != null) {
            return joinGame(player, availableArena.getName());
        }
        
        // No available arenas, try to create a new game in any arena
        for (Arena arena : plugin.getArenaManager().getAllArenas().values()) {
            if (arena.isEnabled() && arena.getState() == ArenaState.WAITING) {
                CTFGame game = getOrCreateGame(arena);
                if (game != null && game.addPlayer(player)) {
                    return true;
                }
            }
        }
        
        plugin.getMessageUtil().sendMessage(player, "error.arena-full");
        return false;
    }
    
    public void leaveGame(Player player) {
        String arenaName = plugin.getPlayerManager().getPlayerArena(player);
        if (arenaName == null) {
            plugin.getMessageUtil().sendMessage(player, "error.not-in-game");
            return;
        }
        
        CTFGame game = activeGames.get(arenaName);
        if (game != null) {
            game.removePlayer(player);
        }
        
        plugin.getPlayerManager().removePlayerFromArena(player);
        plugin.getRespawnManager().removeFromRespawnQueue(player);
        
        // Update tab list for player leaving arena
        plugin.getTabListManager().onPlayerLeaveArena(player);
        plugin.getTabListManager().setTabListHeader(player);
        
        // Create lobby scoreboard
        plugin.getLobbyManager().createLobbyScoreboard(player);
        
        plugin.getMessageUtil().sendMessage(player, "game.left-queue", 
            Map.of("arena", arenaName));
    }
    
    private CTFGame getOrCreateGame(Arena arena) {
        CTFGame existingGame = activeGames.get(arena.getName());
        
        // If there's an existing game and it's not full, return it
        if (existingGame != null && 
            existingGame.getPlayers().size() < plugin.getConfigManager().getMaxPlayersPerArena() &&
            arena.getState() == ArenaState.WAITING) {
            return existingGame;
        }
        
        // If arena is not in waiting state, can't create new game
        if (arena.getState() != ArenaState.WAITING) {
            return null;
        }
        
        // Create new game
        CTFGame newGame = new CTFGame(plugin, arena);
        activeGames.put(arena.getName(), newGame);
        
        return newGame;
    }
    
    public void endGame(Arena arena) {
        CTFGame game = activeGames.get(arena.getName());
        if (game != null) {
            game.endGame(null);
            activeGames.remove(arena.getName());
        }
    }
    
    public void endAllGames() {
        for (CTFGame game : activeGames.values()) {
            game.endGame(null);
        }
        activeGames.clear();
    }
    
    public void forceStartGame(String arenaName) {
        CTFGame game = activeGames.get(arenaName);
        if (game != null && !game.isGameStarted()) {
            game.startGame();
        }
    }
    
    public void forceEndGame(String arenaName) {
        CTFGame game = activeGames.get(arenaName);
        if (game != null) {
            game.endGame(null);
        }
    }
    
    public void updateAllGames() {
        for (CTFGame game : activeGames.values()) {
            game.update();
        }
        
        // Check spawn protection
        plugin.getPlayerManager().checkSpawnProtection();
    }
    
    public CTFGame getGame(String arenaName) {
        return activeGames.get(arenaName);
    }
    
    public CTFGame getPlayerGame(Player player) {
        String arenaName = plugin.getPlayerManager().getPlayerArena(player);
        if (arenaName != null) {
            return activeGames.get(arenaName);
        }
        return null;
    }
    
    public boolean isInGame(Player player) {
        return plugin.getPlayerManager().isInArena(player);
    }
    
    public Map<String, CTFGame> getActiveGames() {
        return new HashMap<>(activeGames);
    }
    
    public int getActiveGameCount() {
        return activeGames.size();
    }
    
    public int getTotalPlayers() {
        return activeGames.values().stream()
            .mapToInt(game -> game.getPlayers().size())
            .sum();
    }
}