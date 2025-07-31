/*
 * ArenaWarsCTF - Professional CTF Plugin
 * Copyright (c) 2025 ArenaWars Development Team - All Rights Reserved
 */

package com.arenawars.ctf.managers;

import com.arenawars.ctf.ArenaWarsCTF;
import com.arenawars.ctf.arena.Arena;
import com.arenawars.ctf.game.Team;
import com.arenawars.ctf.utils.MessageUtil;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class RespawnManager {
    
    private final ArenaWarsCTF plugin;
    private final Map<UUID, RespawnData> respawnQueue;
    private final Random random;
    
    public RespawnManager(ArenaWarsCTF plugin) {
        this.plugin = plugin;
        this.respawnQueue = new HashMap<>();
        this.random = new Random();
        
        // Start respawn task
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::processRespawnQueue, 20L, 20L);
    }
    
    public void handlePlayerDeath(Player player) {
        String arenaName = plugin.getPlayerManager().getPlayerArena(player);
        if (arenaName == null) return;
        
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        if (arena == null) return;
        
        Team team = plugin.getPlayerManager().getPlayerTeam(player);
        if (team == null) return;
        
        // Add death to statistics
        plugin.getPlayerManager().addDeath(player);
        
        // Create respawn data
        RespawnData respawnData = new RespawnData(player, arena, team);
        respawnQueue.put(player.getUniqueId(), respawnData);
        
        // Teleport to spectator point and set spectator mode
        player.teleport(arena.getSpectatorPoint());
        player.setGameMode(GameMode.SPECTATOR);
        player.setAllowFlight(true);
        player.setFlying(true);
        
        // Clear inventory and effects
        player.getInventory().clear();
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        
        // Send death message
        Map<String, String> placeholders = MessageUtil.createPlaceholders(
            "seconds", String.valueOf(plugin.getConfigManager().getSpectatorTime())
        );
        plugin.getMessageUtil().sendMessage(player, "game.death-message", placeholders);
        
        // Start spectator countdown
        startSpectatorCountdown(player, respawnData);
    }
    
    private void startSpectatorCountdown(Player player, RespawnData respawnData) {
        respawnData.spectatorTimeLeft = plugin.getConfigManager().getSpectatorTime();
        
        // Send initial spectator message
        updateSpectatorMessage(player, respawnData);
    }
    
    private void updateSpectatorMessage(Player player, RespawnData respawnData) {
        if (respawnData.spectatorTimeLeft > 0) {
            Map<String, String> placeholders = MessageUtil.createPlaceholders(
                "seconds", String.valueOf(respawnData.spectatorTimeLeft)
            );
            plugin.getMessageUtil().sendActionBar(player, "&cSpectating for &e" + respawnData.spectatorTimeLeft + " &cseconds...");
        } else {
            Map<String, String> placeholders = MessageUtil.createPlaceholders(
                "seconds", String.valueOf(respawnData.respawnTimeLeft)
            );
            plugin.getMessageUtil().sendActionBar(player, "&aRespawning in &e" + respawnData.respawnTimeLeft + " &aseconds...");
        }
    }
    
    private void processRespawnQueue() {
        for (RespawnData respawnData : respawnQueue.values()) {
            Player player = respawnData.player;
            
            if (!player.isOnline()) {
                respawnQueue.remove(player.getUniqueId());
                continue;
            }
            
            if (respawnData.spectatorTimeLeft > 0) {
                // Still in spectator phase
                respawnData.spectatorTimeLeft--;
                updateSpectatorMessage(player, respawnData);
                
                if (respawnData.spectatorTimeLeft == 0) {
                    // Start respawn countdown
                    respawnData.respawnTimeLeft = plugin.getConfigManager().getRespawnDelay();
                    Map<String, String> placeholders = MessageUtil.createPlaceholders(
                        "seconds", String.valueOf(respawnData.respawnTimeLeft)
                    );
                    plugin.getMessageUtil().sendMessage(player, "game.respawning", placeholders);
                }
            } else if (respawnData.respawnTimeLeft > 0) {
                // In respawn countdown
                respawnData.respawnTimeLeft--;
                updateSpectatorMessage(player, respawnData);
                
                if (respawnData.respawnTimeLeft == 0) {
                    // Respawn player
                    respawnPlayer(player, respawnData);
                }
            }
        }
        
        // Remove completed respawns
        respawnQueue.entrySet().removeIf(entry -> entry.getValue().respawnTimeLeft <= 0 && entry.getValue().spectatorTimeLeft <= 0);
    }
    
    private void respawnPlayer(Player player, RespawnData respawnData) {
        Arena arena = respawnData.arena;
        Team team = respawnData.team;
        
        // Get spawn location
        Location spawnLocation = getSpawnLocation(arena, team);
        if (spawnLocation == null) {
            plugin.getLogger().warning("No spawn location found for team " + team + " in arena " + arena.getName());
            return;
        }
        
        // Teleport and set survival mode
        player.teleport(spawnLocation);
        player.setGameMode(GameMode.SURVIVAL);
        player.setAllowFlight(false);
        player.setFlying(false);
        
        // Reset health and hunger
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
        
        // Give equipment
        givePlayerEquipment(player, team);
        
        // Add spawn protection
        plugin.getPlayerManager().addSpawnProtection(player);
        
        // Add spawn protection effect
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE,
                plugin.getConfigManager().getSpawnProtectionTime() * 20, 4, false, false));
        
        // Send respawn message
        plugin.getMessageUtil().sendMessage(player, "game.respawned");
        
        // Remove from respawn queue
        respawnQueue.remove(player.getUniqueId());
    }
    
    private Location getSpawnLocation(Arena arena, Team team) {
        List<Location> spawns = team == Team.RED ? arena.getRedSpawns() : arena.getBlueSpawns();
        
        if (spawns.isEmpty()) {
            return null;
        }
        
        // Return random spawn
        return spawns.get(random.nextInt(spawns.size()));
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
    
    public boolean isInRespawnQueue(Player player) {
        return respawnQueue.containsKey(player.getUniqueId());
    }
    
    public void removeFromRespawnQueue(Player player) {
        respawnQueue.remove(player.getUniqueId());
    }
    
    public void clearRespawnQueue() {
        respawnQueue.clear();
    }
    
    private static class RespawnData {
        public final Player player;
        public final Arena arena;
        public final Team team;
        public int spectatorTimeLeft;
        public int respawnTimeLeft;
        
        public RespawnData(Player player, Arena arena, Team team) {
            this.player = player;
            this.arena = arena;
            this.team = team;
            this.spectatorTimeLeft = 0;
            this.respawnTimeLeft = 0;
        }
    }
}