/*
 * ArenaWarsCTF - Professional CTF Plugin
 * Copyright (c) 2025 ArenaWars Development Team - All Rights Reserved
 */

package com.arenawars.ctf.managers;

import com.arenawars.ctf.ArenaWarsCTF;
import com.arenawars.ctf.arena.Arena;
import com.arenawars.ctf.arena.ArenaSetup;
import com.arenawars.ctf.arena.ArenaState;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ArenaManager {
    
    private final ArenaWarsCTF plugin;
    private final Map<String, Arena> arenas;
    private final ArenaSetup arenaSetup;
    
    public ArenaManager(ArenaWarsCTF plugin) {
        this.plugin = plugin;
        this.arenas = new HashMap<>();
        this.arenaSetup = new ArenaSetup(plugin);
        
        loadAllArenas();
    }
    
    private void loadAllArenas() {
        File arenasFolder = plugin.getConfigManager().getArenasFolder();
        
        if (!arenasFolder.exists()) {
            arenasFolder.mkdirs();
            return;
        }
        
        File[] files = arenasFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;
        
        for (File file : files) {
            try {
                Arena arena = Arena.load(file);
                arenas.put(arena.getName().toLowerCase(), arena);
                plugin.getLogger().info("Loaded arena: " + arena.getName());
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to load arena from " + file.getName() + ": " + e.getMessage());
            }
        }
        
        plugin.getLogger().info("Loaded " + arenas.size() + " arenas");
    }
    
    public void saveAllArenas() {
        for (Arena arena : arenas.values()) {
            saveArena(arena);
        }
    }
    
    public void saveArena(Arena arena) {
        File file = new File(plugin.getConfigManager().getArenasFolder(), arena.getName() + ".yml");
        
        try {
            arena.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save arena " + arena.getName() + ": " + e.getMessage());
        }
    }
    
    public Arena getArena(String name) {
        return arenas.get(name.toLowerCase());
    }
    
    public void addArena(Arena arena) {
        arenas.put(arena.getName().toLowerCase(), arena);
        saveArena(arena);
    }
    
    public void removeArena(String name) {
        Arena arena = arenas.remove(name.toLowerCase());
        if (arena != null) {
            File file = new File(plugin.getConfigManager().getArenasFolder(), arena.getName() + ".yml");
            if (file.exists()) {
                file.delete();
            }
        }
    }
    
    public Set<String> getArenaNames() {
        return arenas.keySet();
    }
    
    public Map<String, Arena> getAllArenas() {
        return new HashMap<>(arenas);
    }
    
    public Arena getAvailableArena() {
        for (Arena arena : arenas.values()) {
            if (arena.isEnabled() && arena.getState() == ArenaState.WAITING) {
                return arena;
            }
        }
        return null;
    }
    
    public boolean createArena(String name) {
        if (arenas.containsKey(name.toLowerCase())) {
            return false;
        }
        
        Arena arena = new Arena(name);
        addArena(arena);
        return true;
    }
    
    public void startSetup(Player player, String arenaName) {
        arenaSetup.startSetup(player, arenaName);
    }
    
    public void endSetup(Player player) {
        arenaSetup.endSetup(player);
    }
    
    public boolean isInSetup(Player player) {
        return arenaSetup.isInSetup(player);
    }
    
    public ArenaSetup.SetupSession getSetupSession(Player player) {
        return arenaSetup.getSetupSession(player);
    }
    
    public void handleSetupInteraction(Player player, boolean isRightClick) {
        if (!isInSetup(player)) return;
        
        if (isRightClick) {
            arenaSetup.setPosition(player, player.getLocation());
        }
    }
    
    public void handleSetupTool(Player player, String toolType) {
        if (!isInSetup(player)) return;
        
        switch (toolType.toLowerCase()) {
            case "red_spawn":
                arenaSetup.addSpawn(player, player.getLocation(), true);
                break;
            case "blue_spawn":
                arenaSetup.addSpawn(player, player.getLocation(), false);
                break;
            case "next":
                arenaSetup.handleNextStep(player);
                break;
            case "exit":
                endSetup(player);
                break;
        }
    }
    
    public void reloadArenas() {
        arenas.clear();
        loadAllArenas();
    }
    
    public boolean isArenaWorld(String worldName) {
        for (Arena arena : arenas.values()) {
            if (arena.getWorldName() != null && arena.getWorldName().equals(worldName)) {
                return true;
            }
        }
        return false;
    }
    
    public Arena getArenaByWorld(String worldName) {
        for (Arena arena : arenas.values()) {
            if (arena.getWorldName() != null && arena.getWorldName().equals(worldName)) {
                return arena;
            }
        }
        return null;
    }
    
    public boolean validateArena(Arena arena) {
        return arena.isValid();
    }
    
    public void enableArena(String name) {
        Arena arena = getArena(name);
        if (arena != null && arena.isValid()) {
            arena.setEnabled(true);
            arena.setState(ArenaState.WAITING);
            saveArena(arena);
        }
    }
    
    public void disableArena(String name) {
        Arena arena = getArena(name);
        if (arena != null) {
            arena.setEnabled(false);
            arena.setState(ArenaState.DISABLED);
            
            // End any active games in this arena
            plugin.getGameManager().endGame(arena);
            
            saveArena(arena);
        }
    }
}