/*
 * ArenaWarsCTF - Professional CTF Plugin
 * Copyright (c) 2025 ArenaWars Development Team - All Rights Reserved
 */

package com.arenawars.ctf.managers;

import com.arenawars.ctf.ArenaWarsCTF;
import com.arenawars.ctf.game.Team;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class TabListManager {
    
    private final ArenaWarsCTF plugin;
    private final LegacyComponentSerializer serializer;
    private final Map<Player, String> originalPlayerNames;
    
    public TabListManager(ArenaWarsCTF plugin) {
        this.plugin = plugin;
        this.serializer = LegacyComponentSerializer.legacyAmpersand();
        this.originalPlayerNames = new HashMap<>();
    }
    
    public void updatePlayerTabList(Player player) {
        String arenaName = plugin.getPlayerManager().getPlayerArena(player);
        
        if (arenaName != null) {
            // Player is in arena - show arena-specific tab list
            updateArenaTabList(player, arenaName);
        } else {
            // Player is in lobby - show lobby tab list
            updateLobbyTabList(player);
        }
    }
    
    private void updateArenaTabList(Player player, String arenaName) {
        // Hide all players first
        for (Player otherPlayer : plugin.getServer().getOnlinePlayers()) {
            if (!otherPlayer.equals(player)) {
                player.hidePlayer(plugin, otherPlayer);
            }
        }
        
        // Show only players in the same arena
        for (Player arenaPlayer : plugin.getPlayerManager().getPlayersInArena(arenaName)) {
            if (!arenaPlayer.equals(player) && arenaPlayer.isOnline()) {
                player.showPlayer(plugin, arenaPlayer);
                
                // Update their display name based on team
                updatePlayerDisplayName(player, arenaPlayer, arenaName);
            }
        }
        
        // Update own display name
        updateOwnDisplayName(player, arenaName);
    }
    
    private void updateLobbyTabList(Player player) {
        // Show all lobby players
        for (Player otherPlayer : plugin.getServer().getOnlinePlayers()) {
            if (!plugin.getPlayerManager().isInArena(otherPlayer)) {
                player.showPlayer(plugin, otherPlayer);
                
                // Update display name for lobby
                updateLobbyPlayerDisplayName(player, otherPlayer);
            } else {
                // Hide arena players
                player.hidePlayer(plugin, otherPlayer);
            }
        }
        
        // Update own display name for lobby
        updateOwnLobbyDisplayName(player);
    }
    
    private void updatePlayerDisplayName(Player viewer, Player target, String arenaName) {
        Team targetTeam = plugin.getPlayerManager().getPlayerTeam(target);
        PlayerManager.PlayerData data = plugin.getPlayerManager().getPlayerData(target);
        int level = plugin.getXPManager().calculateLevel(data.experience);
        String levelColor = plugin.getXPManager().getLevelColor(level);
        
        String displayName;
        if (targetTeam != null) {
            displayName = targetTeam.getColoredName() + " " + levelColor + "[" + level + "] " + target.getName();
        } else {
            displayName = "&7[Waiting] " + levelColor + "[" + level + "] " + target.getName();
        }
        
        // Set custom display name in tab list
        Component displayComponent = serializer.deserialize(displayName);
        target.playerListName(displayComponent);
    }
    
    private void updateOwnDisplayName(Player player, String arenaName) {
        Team playerTeam = plugin.getPlayerManager().getPlayerTeam(player);
        PlayerManager.PlayerData data = plugin.getPlayerManager().getPlayerData(player);
        int level = plugin.getXPManager().calculateLevel(data.experience);
        String levelColor = plugin.getXPManager().getLevelColor(level);
        
        String displayName;
        if (playerTeam != null) {
            displayName = playerTeam.getColoredName() + " " + levelColor + "[" + level + "] " + player.getName();
        } else {
            displayName = "&7[Waiting] " + levelColor + "[" + level + "] " + player.getName();
        }
        
        Component displayComponent = serializer.deserialize(displayName);
        player.playerListName(displayComponent);
    }
    
    private void updateLobbyPlayerDisplayName(Player viewer, Player target) {
        PlayerManager.PlayerData data = plugin.getPlayerManager().getPlayerData(target);
        int level = plugin.getXPManager().calculateLevel(data.experience);
        String levelColor = plugin.getXPManager().getLevelColor(level);
        String levelTitle = plugin.getXPManager().getLevelTitle(level);
        
        String displayName = "&b[Lobby] " + levelColor + "[" + level + " " + levelTitle + "] " + target.getName();
        
        Component displayComponent = serializer.deserialize(displayName);
        target.playerListName(displayComponent);
    }
    
    private void updateOwnLobbyDisplayName(Player player) {
        PlayerManager.PlayerData data = plugin.getPlayerManager().getPlayerData(player);
        int level = plugin.getXPManager().calculateLevel(data.experience);
        String levelColor = plugin.getXPManager().getLevelColor(level);
        String levelTitle = plugin.getXPManager().getLevelTitle(level);
        
        String displayName = "&b[Lobby] " + levelColor + "[" + level + " " + levelTitle + "] " + player.getName();
        
        Component displayComponent = serializer.deserialize(displayName);
        player.playerListName(displayComponent);
    }
    
    public void updateAllTabLists() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            updatePlayerTabList(player);
        }
    }
    
    public void restorePlayerTabList(Player player) {
        // Show all players again
        for (Player otherPlayer : plugin.getServer().getOnlinePlayers()) {
            player.showPlayer(plugin, otherPlayer);
        }
        
        // Restore original display name
        player.playerListName(Component.text(player.getName()));
    }
    
    public void onPlayerJoinArena(Player player, String arenaName) {
        // Update tab lists for all players
        updateAllTabLists();
    }
    
    public void onPlayerLeaveArena(Player player) {
        // Restore player to lobby tab list
        updatePlayerTabList(player);
        
        // Update tab lists for all other players
        updateAllTabLists();
    }
    
    public void setTabListHeader(Player player) {
        String arenaName = plugin.getPlayerManager().getPlayerArena(player);
        
        Component header;
        Component footer;
        
        if (arenaName != null) {
            // Arena header/footer
            header = serializer.deserialize("&6&lArenaWars CTF\n&e&lArena: " + arenaName);
            footer = serializer.deserialize("&7Your team: " + getPlayerTeamDisplay(player) + "\n&ewww.arenawars.com");
        } else {
            // Lobby header/footer
            PlayerManager.PlayerData data = plugin.getPlayerManager().getPlayerData(player);
            int level = plugin.getXPManager().calculateLevel(data.experience);
            String levelColor = plugin.getXPManager().getLevelColor(level);
            
            header = serializer.deserialize("&6&lArenaWars CTF\n&b&lLobby - " + levelColor + "Level " + level);
            footer = serializer.deserialize("&7Type &e/ctf join &7to play!\n&ewww.arenawars.com");
        }
        
        player.sendPlayerListHeaderAndFooter(header, footer);
    }
    
    private String getPlayerTeamDisplay(Player player) {
        Team team = plugin.getPlayerManager().getPlayerTeam(player);
        if (team != null) {
            return team.getColoredName();
        }
        return "&7Waiting...";
    }
    
    public void updateAllTabListHeaders() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            setTabListHeader(player);
        }
    }
}