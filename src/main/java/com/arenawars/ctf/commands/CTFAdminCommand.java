/*
 * ArenaWarsCTF - Professional CTF Plugin
 * Copyright (c) 2025 ArenaWars Development Team - All Rights Reserved
 */

package com.arenawars.ctf.commands;

import com.arenawars.ctf.ArenaWarsCTF;
import com.arenawars.ctf.arena.Arena;
import com.arenawars.ctf.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CTFAdminCommand implements CommandExecutor, TabCompleter {
    
    private final ArenaWarsCTF plugin;
    
    public CTFAdminCommand(ArenaWarsCTF plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("arenawars.ctf.admin")) {
            plugin.getMessageUtil().sendRawMessage((Player) sender, plugin.getMessageUtil().getMessage("general.no-permission"));
            return true;
        }
        
        if (args.length == 0) {
            showAdminHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "setup":
                return handleSetup(sender, args);
            case "create":
                return handleCreate(sender, args);
            case "delete":
                return handleDelete(sender, args);
            case "enable":
                return handleEnable(sender, args);
            case "disable":
                return handleDisable(sender, args);
            case "start":
                return handleStart(sender, args);
            case "stop":
                return handleStop(sender, args);
            case "reload":
                return handleReload(sender, args);
            case "info":
                return handleInfo(sender, args);
            case "help":
            default:
                showAdminHelp(sender);
                return true;
        }
    }
    
    private boolean handleSetup(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageUtil().sendRawMessage((Player) sender, plugin.getMessageUtil().getMessage("general.player-only"));
            return true;
        }
        
        if (args.length < 2) {
            plugin.getMessageUtil().sendRawMessage((Player) sender, "&cUsage: /ctfadmin setup <arena>");
            return true;
        }
        
        Player player = (Player) sender;
        String arenaName = args[1];
        
        // Check if already in setup mode
        if (plugin.getArenaManager().isInSetup(player)) {
            plugin.getArenaManager().endSetup(player);
        }
        
        plugin.getArenaManager().startSetup(player, arenaName);
        return true;
    }
    
    private boolean handleCreate(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.getMessageUtil().sendRawMessage((Player) sender, "&cUsage: /ctfadmin create <arena>");
            return true;
        }
        
        String arenaName = args[1];
        
        if (plugin.getArenaManager().createArena(arenaName)) {
            Map<String, String> placeholders = MessageUtil.createPlaceholders("arena", arenaName);
            plugin.getMessageUtil().sendMessage((Player) sender, "arena.created", placeholders);
        } else {
            Map<String, String> placeholders = MessageUtil.createPlaceholders("arena", arenaName);
            plugin.getMessageUtil().sendMessage((Player) sender, "arena.already-exists", placeholders);
        }
        
        return true;
    }
    
    private boolean handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.getMessageUtil().sendRawMessage((Player) sender, "&cUsage: /ctfadmin delete <arena>");
            return true;
        }
        
        String arenaName = args[1];
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        
        if (arena == null) {
            Map<String, String> placeholders = MessageUtil.createPlaceholders("arena", arenaName);
            plugin.getMessageUtil().sendMessage((Player) sender, "arena.not-found", placeholders);
            return true;
        }
        
        // End any active game first
        plugin.getGameManager().endGame(arena);
        
        plugin.getArenaManager().removeArena(arenaName);
        
        Map<String, String> placeholders = MessageUtil.createPlaceholders("arena", arenaName);
        plugin.getMessageUtil().sendMessage((Player) sender, "arena.deleted", placeholders);
        
        return true;
    }
    
    private boolean handleEnable(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.getMessageUtil().sendRawMessage((Player) sender, "&cUsage: /ctfadmin enable <arena>");
            return true;
        }
        
        String arenaName = args[1];
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        
        if (arena == null) {
            Map<String, String> placeholders = MessageUtil.createPlaceholders("arena", arenaName);
            plugin.getMessageUtil().sendMessage((Player) sender, "arena.not-found", placeholders);
            return true;
        }
        
        if (!arena.isValid()) {
            plugin.getMessageUtil().sendRawMessage((Player) sender, "&cArena is not properly configured! Use /ctfadmin info " + arenaName + " to see issues.");
            return true;
        }
        
        plugin.getArenaManager().enableArena(arenaName);
        plugin.getMessageUtil().sendRawMessage((Player) sender, "&aArena " + arenaName + " enabled!");
        
        return true;
    }
    
    private boolean handleDisable(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.getMessageUtil().sendRawMessage((Player) sender, "&cUsage: /ctfadmin disable <arena>");
            return true;
        }
        
        String arenaName = args[1];
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        
        if (arena == null) {
            Map<String, String> placeholders = MessageUtil.createPlaceholders("arena", arenaName);
            plugin.getMessageUtil().sendMessage((Player) sender, "arena.not-found", placeholders);
            return true;
        }
        
        plugin.getArenaManager().disableArena(arenaName);
        plugin.getMessageUtil().sendRawMessage((Player) sender, "&cArena " + arenaName + " disabled!");
        
        return true;
    }
    
    private boolean handleStart(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.getMessageUtil().sendRawMessage((Player) sender, "&cUsage: /ctfadmin start <arena>");
            return true;
        }
        
        String arenaName = args[1];
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        
        if (arena == null) {
            Map<String, String> placeholders = MessageUtil.createPlaceholders("arena", arenaName);
            plugin.getMessageUtil().sendMessage((Player) sender, "arena.not-found", placeholders);
            return true;
        }
        
        plugin.getGameManager().forceStartGame(arenaName);
        plugin.getMessageUtil().sendRawMessage((Player) sender, "&aForce started game in arena " + arenaName + "!");
        
        return true;
    }
    
    private boolean handleStop(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.getMessageUtil().sendRawMessage((Player) sender, "&cUsage: /ctfadmin stop <arena>");
            return true;
        }
        
        String arenaName = args[1];
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        
        if (arena == null) {
            Map<String, String> placeholders = MessageUtil.createPlaceholders("arena", arenaName);
            plugin.getMessageUtil().sendMessage((Player) sender, "arena.not-found", placeholders);
            return true;
        }
        
        plugin.getGameManager().forceEndGame(arenaName);
        plugin.getMessageUtil().sendRawMessage((Player) sender, "&cForce stopped game in arena " + arenaName + "!");
        
        return true;
    }
    
    private boolean handleReload(CommandSender sender, String[] args) {
        plugin.getConfigManager().reloadConfigs();
        plugin.getArenaManager().reloadArenas();
        
        plugin.getMessageUtil().sendMessage((Player) sender, "general.reload-success");
        
        return true;
    }
    
    private boolean handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.getMessageUtil().sendRawMessage((Player) sender, "&cUsage: /ctfadmin info <arena>");
            return true;
        }
        
        String arenaName = args[1];
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        
        if (arena == null) {
            Map<String, String> placeholders = MessageUtil.createPlaceholders("arena", arenaName);
            plugin.getMessageUtil().sendMessage((Player) sender, "arena.not-found", placeholders);
            return true;
        }
        
        plugin.getMessageUtil().sendRawMessage((Player) sender, "&6=== Arena Info: " + arena.getDisplayName() + " ===");
        plugin.getMessageUtil().sendRawMessage((Player) sender, "&7Name: &e" + arena.getName());
        plugin.getMessageUtil().sendRawMessage((Player) sender, "&7World: &e" + (arena.getWorldName() != null ? arena.getWorldName() : "Not set"));
        plugin.getMessageUtil().sendRawMessage((Player) sender, "&7Enabled: " + (arena.isEnabled() ? "&aYes" : "&cNo"));
        plugin.getMessageUtil().sendRawMessage((Player) sender, "&7State: &e" + arena.getState());
        plugin.getMessageUtil().sendRawMessage((Player) sender, "&7Valid: " + (arena.isValid() ? "&aYes" : "&cNo"));
        
        if (!arena.isValid()) {
            plugin.getMessageUtil().sendRawMessage((Player) sender, "&cValidation Errors:");
            for (String error : arena.getValidationErrors()) {
                plugin.getMessageUtil().sendRawMessage((Player) sender, "&7- &c" + error);
            }
        }
        
        plugin.getMessageUtil().sendRawMessage((Player) sender, "&7Red Spawns: &e" + arena.getRedSpawns().size());
        plugin.getMessageUtil().sendRawMessage((Player) sender, "&7Blue Spawns: &e" + arena.getBlueSpawns().size());
        
        return true;
    }
    
    private void showAdminHelp(CommandSender sender) {
        if (!(sender instanceof Player)) {
            return;
        }
        
        Player player = (Player) sender;
        
        plugin.getMessageUtil().sendMessage(player, "help.admin-header");
        plugin.getMessageUtil().sendMessage(player, "help.setup");
        plugin.getMessageUtil().sendMessage(player, "help.create");
        plugin.getMessageUtil().sendMessage(player, "help.delete");
        plugin.getMessageUtil().sendMessage(player, "help.start");
        plugin.getMessageUtil().sendMessage(player, "help.stop");
        plugin.getMessageUtil().sendMessage(player, "help.reload");
        
        plugin.getMessageUtil().sendRawMessage(player, "&c/ctfadmin enable <arena> &7- Enable arena");
        plugin.getMessageUtil().sendRawMessage(player, "&c/ctfadmin disable <arena> &7- Disable arena");
        plugin.getMessageUtil().sendRawMessage(player, "&c/ctfadmin info <arena> &7- Show arena info");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!sender.hasPermission("arenawars.ctf.admin")) {
            return completions;
        }
        
        if (args.length == 1) {
            // Admin subcommands
            List<String> subCommands = Arrays.asList("setup", "create", "delete", "enable", "disable", "start", "stop", "reload", "info", "help");
            for (String subCommand : subCommands) {
                if (subCommand.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            // Commands that need arena names
            if (Arrays.asList("setup", "delete", "enable", "disable", "start", "stop", "info").contains(subCommand)) {
                for (String arenaName : plugin.getArenaManager().getArenaNames()) {
                    if (arenaName.toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(arenaName);
                    }
                }
            }
        }
        
        return completions;
    }
}