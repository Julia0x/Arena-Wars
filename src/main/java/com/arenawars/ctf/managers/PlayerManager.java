/*
 * ArenaWarsCTF - Professional CTF Plugin
 * Copyright (c) 2025 ArenaWars Development Team - All Rights Reserved
 */

package com.arenawars.ctf.managers;

import com.arenawars.ctf.ArenaWarsCTF;
import com.arenawars.ctf.game.Team;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PlayerManager {
    
    private final ArenaWarsCTF plugin;
    private final Map<UUID, PlayerData> playerData;
    private final Map<UUID, String> playerArenas; // Player UUID -> Arena name
    private final Map<UUID, Team> playerTeams;    // Player UUID -> Team
    private final Map<UUID, Long> spawnProtection; // Player UUID -> Protection end time
    
    public PlayerManager(ArenaWarsCTF plugin) {
        this.plugin = plugin;
        this.playerData = new HashMap<>();
        this.playerArenas = new HashMap<>();
        this.playerTeams = new HashMap<>();
        this.spawnProtection = new HashMap<>();
        
        loadAllPlayerData();
    }
    
    private void loadAllPlayerData() {
        File playerDataFolder = plugin.getConfigManager().getPlayerDataFolder();
        
        if (!playerDataFolder.exists()) {
            playerDataFolder.mkdirs();
            return;
        }
        
        File[] files = playerDataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;
        
        for (File file : files) {
            try {
                String fileName = file.getName();
                String uuidString = fileName.substring(0, fileName.length() - 4);
                UUID uuid = UUID.fromString(uuidString);
                
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                PlayerData data = new PlayerData(uuid);
                
                data.kills = config.getInt("kills", 0);
                data.deaths = config.getInt("deaths", 0);
                data.captures = config.getInt("captures", 0);
                data.returns = config.getInt("returns", 0);
                data.gamesPlayed = config.getInt("games-played", 0);
                data.gamesWon = config.getInt("games-won", 0);
                data.experience = config.getInt("experience", 0);
                data.coins = config.getInt("coins", 0);
                
                playerData.put(uuid, data);
                
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load player data from " + file.getName() + ": " + e.getMessage());
            }
        }
        
        plugin.getLogger().info("Loaded player data for " + playerData.size() + " players");
    }
    
    public void saveAllPlayerData() {
        for (PlayerData data : playerData.values()) {
            savePlayerData(data);
        }
    }
    
    public void savePlayerData(PlayerData data) {
        File file = new File(plugin.getConfigManager().getPlayerDataFolder(), data.uuid + ".yml");
        YamlConfiguration config = new YamlConfiguration();
        
        config.set("kills", data.kills);
        config.set("deaths", data.deaths);
        config.set("captures", data.captures);
        config.set("returns", data.returns);
        config.set("games-played", data.gamesPlayed);
        config.set("games-won", data.gamesWon);
        config.set("experience", data.experience);
        config.set("coins", data.coins);
        
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save player data for " + data.uuid + ": " + e.getMessage());
        }
    }
    
    public PlayerData getPlayerData(UUID uuid) {
        return playerData.computeIfAbsent(uuid, PlayerData::new);
    }
    
    public PlayerData getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId());
    }
    
    public void setPlayerArena(Player player, String arenaName) {
        if (arenaName == null) {
            playerArenas.remove(player.getUniqueId());
        } else {
            playerArenas.put(player.getUniqueId(), arenaName);
        }
    }
    
    public String getPlayerArena(Player player) {
        return playerArenas.get(player.getUniqueId());
    }
    
    public boolean isInArena(Player player) {
        return playerArenas.containsKey(player.getUniqueId());
    }
    
    public void setPlayerTeam(Player player, Team team) {
        if (team == null) {
            playerTeams.remove(player.getUniqueId());
        } else {
            playerTeams.put(player.getUniqueId(), team);
        }
    }
    
    public Team getPlayerTeam(Player player) {
        return playerTeams.get(player.getUniqueId());
    }
    
    public void addSpawnProtection(Player player) {
        long protectionEnd = System.currentTimeMillis() + (plugin.getConfigManager().getSpawnProtectionTime() * 1000L);
        spawnProtection.put(player.getUniqueId(), protectionEnd);
    }
    
    public void removeSpawnProtection(Player player) {
        spawnProtection.remove(player.getUniqueId());
        
        if (plugin.getConfigManager().areTitlesEnabled()) {
            plugin.getMessageUtil().sendActionBar(player, "&cSpawn protection removed!");
        }
    }
    
    public boolean hasSpawnProtection(Player player) {
        Long protectionEnd = spawnProtection.get(player.getUniqueId());
        if (protectionEnd == null) return false;
        
        if (System.currentTimeMillis() > protectionEnd) {
            spawnProtection.remove(player.getUniqueId());
            return false;
        }
        
        return true;
    }
    
    public void checkSpawnProtection() {
        Iterator<Map.Entry<UUID, Long>> iterator = spawnProtection.entrySet().iterator();
        long currentTime = System.currentTimeMillis();
        
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            if (currentTime > entry.getValue()) {
                iterator.remove();
                
                Player player = plugin.getServer().getPlayer(entry.getKey());
                if (player != null && plugin.getConfigManager().areTitlesEnabled()) {
                    plugin.getMessageUtil().sendActionBar(player, "&cSpawn protection expired!");
                }
            }
        }
    }
    
    public List<Player> getPlayersInArena(String arenaName) {
        List<Player> players = new ArrayList<>();
        
        for (Map.Entry<UUID, String> entry : playerArenas.entrySet()) {
            if (entry.getValue().equals(arenaName)) {
                Player player = plugin.getServer().getPlayer(entry.getKey());
                if (player != null) {
                    players.add(player);
                }
            }
        }
        
        return players;
    }
    
    public List<Player> getPlayersInTeam(Team team, String arenaName) {
        List<Player> players = new ArrayList<>();
        
        for (Player player : getPlayersInArena(arenaName)) {
            if (getPlayerTeam(player) == team) {
                players.add(player);
            }
        }
        
        return players;
    }
    
    public void removePlayerFromArena(Player player) {
        setPlayerArena(player, null);
        setPlayerTeam(player, null);
        removeSpawnProtection(player);
    }
    
    public void addKill(Player player) {
        PlayerData data = getPlayerData(player);
        data.kills++;
        savePlayerData(data);
        
        // Award kill XP
        plugin.getXPManager().awardKillXP(player);
    }
    
    public void addDeath(Player player) {
        PlayerData data = getPlayerData(player);
        data.deaths++;
        savePlayerData(data);
    }
    
    public void addCapture(Player player) {
        PlayerData data = getPlayerData(player);
        data.captures++;
        savePlayerData(data);
    }
    
    public void addReturn(Player player) {
        PlayerData data = getPlayerData(player);
        data.returns++;
        savePlayerData(data);
        
        // Award return XP
        plugin.getXPManager().awardReturnXP(player);
    }
    
    public void addGamePlayed(Player player) {
        PlayerData data = getPlayerData(player);
        data.gamesPlayed++;
        savePlayerData(data);
    }
    
    public void addGameWon(Player player) {
        PlayerData data = getPlayerData(player);
        data.gamesWon++;
        savePlayerData(data);
    }
    
    public static class PlayerData {
        public final UUID uuid;
        public int kills;
        public int deaths;
        public int captures;
        public int returns;
        public int gamesPlayed;
        public int gamesWon;
        public int experience;
        public int coins;
        
        public PlayerData(UUID uuid) {
            this.uuid = uuid;
            this.kills = 0;
            this.deaths = 0;
            this.captures = 0;
            this.returns = 0;
            this.gamesPlayed = 0;
            this.gamesWon = 0;
            this.experience = 0;
            this.coins = 0;
        }
        
        public double getKDRatio() {
            if (deaths == 0) return kills;
            return (double) kills / deaths;
        }
        
        public double getWinRate() {
            if (gamesPlayed == 0) return 0.0;
            return (double) gamesWon / gamesPlayed * 100;
        }
    }
}