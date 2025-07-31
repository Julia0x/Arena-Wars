/*
 * ArenaWarsCTF - Professional CTF Plugin
 * Copyright (c) 2025 ArenaWars Development Team - All Rights Reserved
 */

package com.arenawars.ctf.commands;

import com.arenawars.ctf.ArenaWarsCTF;
import com.arenawars.ctf.arena.Arena;
import com.arenawars.ctf.managers.PlayerManager;
import com.arenawars.ctf.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CTFCommand implements CommandExecutor, TabCompleter {
    
    private final ArenaWarsCTF plugin;
    
    public CTFCommand(ArenaWarsCTF plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "join":
                return handleJoin(sender, args);
            case "leave":
                return handleLeave(sender, args);
            case "list":
                return handleList(sender, args);
            case "stats":
                return handleStats(sender, args);
            case "chat":
                return handleChatHelp(sender, args);
            case "help":
            default:
                showHelp(sender);
                return true;
        }
    }
    
    private boolean handleJoin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageUtil().sendRawMessage((Player) sender, plugin.getMessageUtil().getMessage("general.player-only"));
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 1) {
            // Join random arena
            if (plugin.getGameManager().joinRandomGame(player)) {
                // Success message sent by GameManager
            }
        } else {
            // Join specific arena
            String arenaName = args[1];
            if (plugin.getGameManager().joinGame(player, arenaName)) {
                // Success message sent by GameManager
            }
        }
        
        return true;
    }
    
    private boolean handleLeave(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageUtil().sendRawMessage((Player) sender, plugin.getMessageUtil().getMessage("general.player-only"));
            return true;
        }
        
        Player player = (Player) sender;
        plugin.getGameManager().leaveGame(player);
        return true;
    }
    
    private boolean handleList(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageUtil().sendRawMessage((Player) sender, plugin.getMessageUtil().getMessage("general.player-only"));
            return true;
        }
        
        Player player = (Player) sender;
        
        plugin.getMessageUtil().sendRawMessage(player, "&6=== Available Arenas ===");
        
        boolean hasArenas = false;
        for (Arena arena : plugin.getArenaManager().getAllArenas().values()) {
            if (arena.isEnabled()) {
                hasArenas = true;
                String status = getArenaStatus(arena);
                plugin.getMessageUtil().sendRawMessage(player, 
                    "&e" + arena.getDisplayName() + " &7- " + status);
            }
        }
        
        if (!hasArenas) {
            plugin.getMessageUtil().sendRawMessage(player, "&cNo arenas available!");
        }
        
        plugin.getMessageUtil().sendRawMessage(player, "&7Use &e/ctf join [arena] &7to join!");
        
        return true;
    }
    
    private String getArenaStatus(Arena arena) {
        switch (arena.getState()) {
            case WAITING:
                return "&aWaiting for players";
            case STARTING:
                return "&eStarting...";
            case ACTIVE:
                return "&cGame in progress";
            case ENDING:
                return "&6Ending...";
            case RESETTING:
                return "&7Resetting...";
            default:
                return "&cDisabled";
        }
    }
    
    private boolean handleStats(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageUtil().sendRawMessage((Player) sender, plugin.getMessageUtil().getMessage("general.player-only"));
            return true;
        }
        
        Player player = (Player) sender;
        Player target = player;
        
        // Check if viewing another player's stats
        if (args.length > 1) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                plugin.getMessageUtil().sendRawMessage(player, "&cPlayer not found!");
                return true;
            }
        }
        
        PlayerManager.PlayerData data = plugin.getPlayerManager().getPlayerData(target);
        
        // Calculate level and XP info
        int level = plugin.getXPManager().calculateLevel(data.experience);
        int currentLevelXP = plugin.getXPManager().getCurrentLevelProgress(data.experience);
        int requiredXP = plugin.getXPManager().getXPRequiredForCurrentLevel(data.experience);
        String levelTitle = plugin.getXPManager().getLevelTitle(level);
        String levelColor = plugin.getXPManager().getLevelColor(level);
        
        plugin.getMessageUtil().sendRawMessage(player, "&6═══ " + target.getName() + "'s Stats ═══");
        plugin.getMessageUtil().sendRawMessage(player, "");
        
        // Level and XP
        plugin.getMessageUtil().sendRawMessage(player, "&7Level: " + levelColor + level + " &8[" + levelTitle + "]");
        plugin.getMessageUtil().sendRawMessage(player, "&7Experience: &e" + data.experience + " XP");
        plugin.getMessageUtil().sendRawMessage(player, "&7Progress: &a" + currentLevelXP + "&7/&e" + requiredXP + " &7(" + String.format("%.1f%%", (double)currentLevelXP/requiredXP*100) + ")");
        
        plugin.getMessageUtil().sendRawMessage(player, "");
        
        // Game Stats
        plugin.getMessageUtil().sendRawMessage(player, "&b&lGame Statistics:");
        plugin.getMessageUtil().sendRawMessage(player, "&7Games Played: &e" + data.gamesPlayed);
        plugin.getMessageUtil().sendRawMessage(player, "&7Games Won: &a" + data.gamesWon);
        plugin.getMessageUtil().sendRawMessage(player, "&7Win Rate: &6" + String.format("%.1f%%", data.getWinRate()));
        
        plugin.getMessageUtil().sendRawMessage(player, "");
        
        // Combat Stats  
        plugin.getMessageUtil().sendRawMessage(player, "&c&lCombat Statistics:");
        plugin.getMessageUtil().sendRawMessage(player, "&7Kills: &a" + data.kills);
        plugin.getMessageUtil().sendRawMessage(player, "&7Deaths: &c" + data.deaths);
        plugin.getMessageUtil().sendRawMessage(player, "&7K/D Ratio: &6" + String.format("%.2f", data.getKDRatio()));
        
        plugin.getMessageUtil().sendRawMessage(player, "");
        
        // Objective Stats
        plugin.getMessageUtil().sendRawMessage(player, "&e&lObjective Statistics:");
        plugin.getMessageUtil().sendRawMessage(player, "&7Flag Captures: &e" + data.captures);
        plugin.getMessageUtil().sendRawMessage(player, "&7Flag Returns: &b" + data.returns);
        
        // Economy (if implemented)
        plugin.getMessageUtil().sendRawMessage(player, "");
        plugin.getMessageUtil().sendRawMessage(player, "&6&lEconomy:");
        plugin.getMessageUtil().sendRawMessage(player, "&7Coins: &6" + data.coins);
        
        return true;
    }
    
    private boolean handleChatHelp(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageUtil().sendRawMessage((Player) sender, plugin.getMessageUtil().getMessage("general.player-only"));
            return true;
        }
        
        Player player = (Player) sender;
        
        plugin.getMessageUtil().sendRawMessage(player, "&6=== CTF Chat System ===");
        plugin.getMessageUtil().sendRawMessage(player, "");
        plugin.getMessageUtil().sendRawMessage(player, "&e&lChat Types:");
        plugin.getMessageUtil().sendRawMessage(player, "&7• &bLobby Chat &7- Talk to lobby players");
        plugin.getMessageUtil().sendRawMessage(player, "&7• &6Arena Chat &7- Talk to your arena players");
        plugin.getMessageUtil().sendRawMessage(player, "&7• &cTeam Chat &7- Use &e@message &7for team only");
        plugin.getMessageUtil().sendRawMessage(player, "");
        plugin.getMessageUtil().sendRawMessage(player, "&e&lExamples:");
        plugin.getMessageUtil().sendRawMessage(player, "&7Normal: &fHello everyone!");
        plugin.getMessageUtil().sendRawMessage(player, "&7Team: &e@Enemy has our flag!");
        plugin.getMessageUtil().sendRawMessage(player, "");
        plugin.getMessageUtil().sendRawMessage(player, "&e&lTab List:");
        plugin.getMessageUtil().sendRawMessage(player, "&7• Only see players in your current context");
        plugin.getMessageUtil().sendRawMessage(player, "&7• Lobby players see lobby, arena players see arena");
        plugin.getMessageUtil().sendRawMessage(player, "&7• Team colors and levels shown");
        
        return true;
    }
    
    private void showHelp(CommandSender sender) {
        if (!(sender instanceof Player)) {
            return;
        }
        
        Player player = (Player) sender;
        
        plugin.getMessageUtil().sendMessage(player, "help.header");
        plugin.getMessageUtil().sendMessage(player, "help.join");
        plugin.getMessageUtil().sendMessage(player, "help.leave");
        plugin.getMessageUtil().sendMessage(player, "help.list");
        plugin.getMessageUtil().sendMessage(player, "help.stats");
        plugin.getMessageUtil().sendRawMessage(player, "&e/ctf chat &7- Show chat system help");
        plugin.getMessageUtil().sendMessage(player, "help.help");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // Main subcommands
            List<String> subCommands = Arrays.asList("join", "leave", "list", "stats", "chat", "help");
            for (String subCommand : subCommands) {
                if (subCommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            if (subCommand.equals("join")) {
                // Arena names
                for (String arenaName : plugin.getArenaManager().getArenaNames()) {
                    Arena arena = plugin.getArenaManager().getArena(arenaName);
                    if (arena != null && arena.isEnabled() && 
                        arenaName.toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(arenaName);
                    }
                }
            } else if (subCommand.equals("stats")) {
                // Online player names
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(player.getName());
                    }
                }
            }
        }
        
        return completions;
    }
}