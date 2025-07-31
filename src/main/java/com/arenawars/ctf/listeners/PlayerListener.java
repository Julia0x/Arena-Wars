/*
 * ArenaWarsCTF - Professional CTF Plugin
 * Copyright (c) 2025 ArenaWars Development Team - All Rights Reserved
 */

package com.arenawars.ctf.listeners;

import com.arenawars.ctf.ArenaWarsCTF;
import com.arenawars.ctf.game.CTFGame;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

public class PlayerListener implements Listener {
    
    private final ArenaWarsCTF plugin;
    
    public PlayerListener(ArenaWarsCTF plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Create lobby scoreboard
        plugin.getLobbyManager().createLobbyScoreboard(player);
        
        // Set up tab list
        plugin.getTabListManager().updatePlayerTabList(player);
        plugin.getTabListManager().setTabListHeader(player);
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Remove from any active games
        if (plugin.getPlayerManager().isInArena(player)) {
            plugin.getGameManager().leaveGame(player);
        }
        
        // Remove scoreboard
        plugin.getScoreboardManager().removeScoreboard(player);
        plugin.getLobbyManager().removeLobbyScoreboard(player);
        
        // Restore tab list
        plugin.getTabListManager().restorePlayerTabList(player);
        
        // End setup mode if active
        if (plugin.getArenaManager().isInSetup(player)) {
            plugin.getArenaManager().endSetup(player);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        
        if (!plugin.getPlayerManager().isInArena(player)) {
            return;
        }
        
        // Award kill to the killer if there is one
        Player killer = player.getKiller();
        if (killer != null && plugin.getPlayerManager().isInArena(killer)) {
            // Check if they're on different teams
            if (plugin.getPlayerManager().getPlayerTeam(killer) != plugin.getPlayerManager().getPlayerTeam(player)) {
                plugin.getPlayerManager().addKill(killer);
            }
        }
        
        // Cancel default death behavior in CTF games
        event.setCancelled(true);
        event.getDrops().clear();
        event.setDroppedExp(0);
        
        // Handle CTF death
        plugin.getRespawnManager().handlePlayerDeath(player);
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) {
            return;
        }
        
        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();
        
        // Check if both players are in the same arena
        String victimArena = plugin.getPlayerManager().getPlayerArena(victim);
        String attackerArena = plugin.getPlayerManager().getPlayerArena(attacker);
        
        if (victimArena == null || attackerArena == null || !victimArena.equals(attackerArena)) {
            return;
        }
        
        // Check if they're on the same team
        if (plugin.getPlayerManager().getPlayerTeam(victim) == plugin.getPlayerManager().getPlayerTeam(attacker)) {
            event.setCancelled(true);
            return;
        }
        
        // Check spawn protection for victim
        if (plugin.getPlayerManager().hasSpawnProtection(victim)) {
            event.setCancelled(true);
            return;
        }
        
        // Remove spawn protection from attacker if configured
        if (plugin.getConfigManager().isSpawnProtectionRemoveOnHit()) {
            if (plugin.getPlayerManager().hasSpawnProtection(attacker)) {
                plugin.getPlayerManager().removeSpawnProtection(attacker);
            }
        }
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        // Handle setup mode interactions
        if (plugin.getArenaManager().isInSetup(player)) {
            handleSetupInteraction(event);
            return;
        }
        
        // Handle flag interactions in game
        if (plugin.getPlayerManager().isInArena(player)) {
            handleGameInteraction(event);
        }
    }
    
    private void handleSetupInteraction(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item == null) return;
        
        event.setCancelled(true);
        
        switch (item.getType()) {
            case GOLDEN_SWORD:
                if (event.getAction().toString().contains("RIGHT")) {
                    plugin.getArenaManager().handleSetupInteraction(player, true);
                }
                break;
            case RED_WOOL:
                if (event.getAction().toString().contains("RIGHT")) {
                    plugin.getArenaManager().handleSetupTool(player, "red_spawn");
                }
                break;
            case BLUE_WOOL:
                if (event.getAction().toString().contains("RIGHT")) {
                    plugin.getArenaManager().handleSetupTool(player, "blue_spawn");
                }
                break;
            case ARROW:
                if (event.getAction().toString().contains("RIGHT")) {
                    plugin.getArenaManager().handleSetupTool(player, "next");
                }
                break;
            case BARRIER:
                if (event.getAction().toString().contains("RIGHT")) {
                    plugin.getArenaManager().handleSetupTool(player, "exit");
                }
                break;
        }
    }
    
    private void handleGameInteraction(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        CTFGame game = plugin.getGameManager().getPlayerGame(player);
        if (game == null || !game.isGameStarted()) return;
        
        // Check for flag pickup
        if (event.getAction().toString().contains("RIGHT")) {
            if (game.attemptFlagPickup(player, player.getLocation())) {
                event.setCancelled(true);
                return;
            }
            
            // Check for flag capture
            if (game.attemptFlagCapture(player, player.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }
    
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        
        // Prevent dropping items in setup mode
        if (plugin.getArenaManager().isInSetup(player)) {
            event.setCancelled(true);
            return;
        }
        
        // Handle flag dropping in game
        if (plugin.getPlayerManager().isInArena(player)) {
            ItemStack item = event.getItemDrop().getItemStack();
            
            // Check if it's a flag item (simplified check)
            if (item.getType() == Material.RED_BANNER || item.getType() == Material.BLUE_BANNER) {
                event.setCancelled(true);
                
                CTFGame game = plugin.getGameManager().getPlayerGame(player);
                if (game != null) {
                    // Determine which flag and drop it
                    // This would need more sophisticated flag tracking
                    // For now, we'll prevent flag dropping via item drop
                }
            }
        }
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        
        // Prevent inventory manipulation in setup mode
        if (plugin.getArenaManager().isInSetup(player)) {
            event.setCancelled(true);
            return;
        }
        
        // Prevent moving flag items in game
        if (plugin.getPlayerManager().isInArena(player)) {
            ItemStack item = event.getCurrentItem();
            if (item != null && (item.getType() == Material.RED_BANNER || item.getType() == Material.BLUE_BANNER)) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        // Check for flag interactions on movement
        if (plugin.getPlayerManager().isInArena(player)) {
            CTFGame game = plugin.getGameManager().getPlayerGame(player);
            if (game != null && game.isGameStarted()) {
                // Check for flag pickup/capture on movement
                if (game.attemptFlagPickup(player, event.getTo())) {
                    return;
                }
                
                if (game.attemptFlagCapture(player, event.getTo())) {
                    return;
                }
            }
        }
    }
    
    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        
        // Cancel teleportation in setup mode (except plugin teleports)
        if (plugin.getArenaManager().isInSetup(player) && 
            event.getCause() != PlayerTeleportEvent.TeleportCause.PLUGIN) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage().toLowerCase();
        
        // Block certain commands in game
        if (plugin.getPlayerManager().isInArena(player)) {
            // Allow CTF commands
            if (command.startsWith("/ctf") || command.startsWith("/arenactf") || command.startsWith("/awctf")) {
                return;
            }
            
            // Block teleportation commands
            if (command.startsWith("/tp") || command.startsWith("/teleport") || 
                command.startsWith("/home") || command.startsWith("/spawn") ||
                command.startsWith("/warp")) {
                event.setCancelled(true);
                plugin.getMessageUtil().sendRawMessage(player, "&cYou cannot use that command while in a CTF game!");
            }
        }
    }
}