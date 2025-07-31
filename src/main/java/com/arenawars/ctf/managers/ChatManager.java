/*
 * ArenaWarsCTF - Professional CTF Plugin
 * Copyright (c) 2025 ArenaWars Development Team - All Rights Reserved
 */

package com.arenawars.ctf.managers;

import com.arenawars.ctf.ArenaWarsCTF;
import com.arenawars.ctf.game.Team;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashSet;
import java.util.Set;

public class ChatManager implements Listener {
    
    private final ArenaWarsCTF plugin;
    private final ColorManager colorManager;
    
    public ChatManager(ArenaWarsCTF plugin) {
        this.plugin = plugin;
        this.colorManager = new ColorManager();
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();
        String message = event.getMessage();
        
        // Cancel the default chat
        event.setCancelled(true);
        
        // Determine chat scope and format
        String arenaName = plugin.getPlayerManager().getPlayerArena(sender);
        
        if (arenaName != null) {
            // Player is in an arena - arena-specific chat
            handleArenaChat(sender, message, arenaName);
        } else {
            // Player is in lobby - lobby chat
            handleLobbyChat(sender, message);
        }
    }
    
    private void handleArenaChat(Player sender, String message, String arenaName) {
        Team senderTeam = plugin.getPlayerManager().getPlayerTeam(sender);
        
        // Check if it's team chat (message starts with @)
        if (message.startsWith("@") && senderTeam != null) {
            handleTeamChat(sender, message.substring(1).trim(), arenaName, senderTeam);
            return;
        }
        
        // Arena-wide chat
        String formattedMessage = formatArenaMessage(sender, message, senderTeam);
        
        // Send to all players in the same arena
        Set<Player> recipients = new HashSet<>(plugin.getPlayerManager().getPlayersInArena(arenaName));
        
        for (Player recipient : recipients) {
            if (recipient.isOnline()) {
                Component messageComponent = colorManager.colorizeForChat(formattedMessage);
                recipient.sendMessage(messageComponent);
            }
        }
        
        // Log to console
        plugin.getLogger().info("[Arena:" + arenaName + "] " + sender.getName() + ": " + message);
    }
    
    private void handleTeamChat(Player sender, String message, String arenaName, Team team) {
        String formattedMessage = formatTeamMessage(sender, message, team);
        
        // Send only to team members
        for (Player teammate : plugin.getPlayerManager().getPlayersInTeam(team, arenaName)) {
            if (teammate.isOnline()) {
                Component messageComponent = colorManager.colorizeForChat(formattedMessage);
                teammate.sendMessage(messageComponent);
            }
        }
        
        // Log to console
        plugin.getLogger().info("[Team:" + team.name() + "@" + arenaName + "] " + sender.getName() + ": " + message);
    }
    
    private void handleLobbyChat(Player sender, String message) {
        String formattedMessage = formatLobbyMessage(sender, message);
        
        // Send to all lobby players (not in any arena)
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!plugin.getPlayerManager().isInArena(player)) {
                Component messageComponent = colorManager.colorizeForChat(formattedMessage);
                player.sendMessage(messageComponent);
            }
        }
        
        // Log to console
        plugin.getLogger().info("[Lobby] " + sender.getName() + ": " + message);
    }
    
    private String formatArenaMessage(Player sender, String message, Team team) {
        // Get player level and title
        PlayerManager.PlayerData data = plugin.getPlayerManager().getPlayerData(sender);
        int level = plugin.getXPManager().calculateLevel(data.experience);
        String levelColor = colorManager.getLevelColor(level);
        
        String teamPrefix = "";
        if (team != null) {
            teamPrefix = team.getColoredName() + " ";
        }
        
        return "&8[&6Arena&8] " + teamPrefix + levelColor + "[" + level + "] &f" + sender.getName() + "&8: &7" + message;
    }
    
    private String formatTeamMessage(Player sender, String message, Team team) {
        // Get player level
        PlayerManager.PlayerData data = plugin.getPlayerManager().getPlayerData(sender);
        int level = plugin.getXPManager().calculateLevel(data.experience);
        String levelColor = colorManager.getLevelColor(level);
        
        return "&8[" + team.getColoredName() + " TEAM&8] " + levelColor + "[" + level + "] &f" + sender.getName() + "&8: &f" + message;
    }
    
    private String formatLobbyMessage(Player sender, String message) {
        // Get player level and title
        PlayerManager.PlayerData data = plugin.getPlayerManager().getPlayerData(sender);
        int level = plugin.getXPManager().calculateLevel(data.experience);
        String levelColor = colorManager.getLevelColor(level);
        String levelTitle = plugin.getXPManager().getLevelTitle(level);
        
        return "&8[&bLobby&8] " + levelColor + "[" + level + " " + levelTitle + "] &f" + sender.getName() + "&8: &7" + message;
    }
    
    // Public method for system messages to specific arenas
    public void sendArenaMessage(String arenaName, String message) {
        Component messageComponent = colorManager.colorizeForChat(message);
        
        for (Player player : plugin.getPlayerManager().getPlayersInArena(arenaName)) {
            if (player.isOnline()) {
                player.sendMessage(messageComponent);
            }
        }
    }
    
    // Public method for system messages to lobby
    public void sendLobbyMessage(String message) {
        Component messageComponent = colorManager.colorizeForChat(message);
        
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!plugin.getPlayerManager().isInArena(player)) {
                player.sendMessage(messageComponent);
            }
        }
    }
    
    // Public method for team messages
    public void sendTeamMessage(String arenaName, Team team, String message) {
        Component messageComponent = colorManager.colorizeForChat(message);
        
        for (Player player : plugin.getPlayerManager().getPlayersInTeam(team, arenaName)) {
            if (player.isOnline()) {
                player.sendMessage(messageComponent);
            }
        }
    }
}