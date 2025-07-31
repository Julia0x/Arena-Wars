/*
 * ArenaWarsCTF - Professional CTF Plugin
 * Copyright (c) 2025 ArenaWars Development Team - All Rights Reserved
 */

package com.arenawars.ctf.listeners;

import com.arenawars.ctf.ArenaWarsCTF;
import com.arenawars.ctf.game.CTFGame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class GameListener implements Listener {
    
    private final ArenaWarsCTF plugin;
    
    public GameListener(ArenaWarsCTF plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        
        // Handle respawn for players in respawn queue
        if (plugin.getRespawnManager().isInRespawnQueue(player)) {
            // Let respawn manager handle the location
            // The respawn manager will teleport them properly
            return;
        }
        
        // If player is in arena but not in respawn queue, something went wrong
        if (plugin.getPlayerManager().isInArena(player)) {
            String arenaName = plugin.getPlayerManager().getPlayerArena(player);
            if (arenaName != null) {
                // Remove from arena as fallback
                plugin.getGameManager().leaveGame(player);
            }
        }
    }
    
    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        
        // Prevent damage in setup mode
        if (plugin.getArenaManager().isInSetup(player)) {
            event.setCancelled(true);
            return;
        }
        
        // Handle damage in games
        if (plugin.getPlayerManager().isInArena(player)) {
            CTFGame game = plugin.getGameManager().getPlayerGame(player);
            
            // Prevent damage before game starts
            if (game == null || !game.isGameStarted()) {
                event.setCancelled(true);
                return;
            }
            
            // Check spawn protection
            if (plugin.getPlayerManager().hasSpawnProtection(player)) {
                event.setCancelled(true);
                return;
            }
            
            // Prevent fall damage in spectator mode
            if (plugin.getRespawnManager().isInRespawnQueue(player)) {
                event.setCancelled(true);
                return;
            }
            
            // Handle void damage (teleport back to arena)
            if (event.getCause() == EntityDamageEvent.DamageCause.VOID) {
                event.setCancelled(true);
                
                // Teleport to spectator point and trigger death
                if (game.getArena().getSpectatorPoint() != null) {
                    player.teleport(game.getArena().getSpectatorPoint());
                    plugin.getRespawnManager().handlePlayerDeath(player);
                }
            }
        }
    }
    
    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        
        // Prevent hunger in setup mode
        if (plugin.getArenaManager().isInSetup(player)) {
            event.setCancelled(true);
            return;
        }
        
        // Prevent hunger while spectating
        if (plugin.getRespawnManager().isInRespawnQueue(player)) {
            event.setCancelled(true);
            return;
        }
        
        // Allow normal hunger in active games
    }
}