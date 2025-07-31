/*
 * ArenaWarsCTF - Professional CTF Plugin
 * 
 * Copyright (c) 2025 ArenaWars Development Team
 * All rights reserved. This software is proprietary and confidential.
 * 
 * Unauthorized copying, distribution, or use is strictly prohibited.
 * Licensed users may not reverse engineer, decompile, or distribute.
 * 
 * For licensing information, contact: license@arenawars.com
 */

package com.arenawars.ctf;

import com.arenawars.ctf.commands.CTFCommand;
import com.arenawars.ctf.commands.CTFAdminCommand;
import com.arenawars.ctf.listeners.GameListener;
import com.arenawars.ctf.listeners.PlayerListener;
import com.arenawars.ctf.listeners.BlockListener;
import com.arenawars.ctf.managers.*;
import com.arenawars.ctf.storage.ConfigManager;
import com.arenawars.ctf.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class ArenaWarsCTF extends JavaPlugin {
    
    private static ArenaWarsCTF instance;
    
    // Managers
    private ConfigManager configManager;
    private ArenaManager arenaManager;
    private GameManager gameManager;
    private PlayerManager playerManager;
    private RespawnManager respawnManager;
    private ScoreboardManager scoreboardManager;
    private XPManager xpManager;
    private MVPManager mvpManager;
    private LobbyManager lobbyManager;
    private ChatManager chatManager;
    private TabListManager tabListManager;
    private MessageUtil messageUtil;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Print copyright notice
        getLogger().info("§6╔══════════════════════════════════════╗");
        getLogger().info("§6║          §eArenaWarsCTF v1.0.0          §6║");
        getLogger().info("§6║   §7© 2025 ArenaWars Development Team   §6║");
        getLogger().info("§6║        §cAll Rights Reserved        §6║");
        getLogger().info("§6╚══════════════════════════════════════╝");
        
        // Check for WorldGuard
        if (!Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
            getLogger().severe("WorldGuard not found! This plugin requires WorldGuard to function.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        
        // Initialize managers
        initializeManagers();
        
        // Register commands
        registerCommands();
        
        // Register listeners
        registerListeners();
        
        // Start periodic tasks
        startTasks();
        
        getLogger().info("ArenaWarsCTF has been enabled successfully!");
    }
    
    @Override
    public void onDisable() {
        // End all active games
        if (gameManager != null) {
            gameManager.endAllGames();
        }
        
        // Save all data
        if (arenaManager != null) {
            arenaManager.saveAllArenas();
        }
        
        if (playerManager != null) {
            playerManager.saveAllPlayerData();
        }
        
        getLogger().info("ArenaWarsCTF has been disabled successfully!");
    }
    
    private void initializeManagers() {
        configManager = new ConfigManager(this);
        messageUtil = new MessageUtil(this);
        arenaManager = new ArenaManager(this);
        playerManager = new PlayerManager(this);
        xpManager = new XPManager(this);
        mvpManager = new MVPManager(this);
        respawnManager = new RespawnManager(this);
        scoreboardManager = new ScoreboardManager(this);
        lobbyManager = new LobbyManager(this);
        chatManager = new ChatManager(this);
        tabListManager = new TabListManager(this);
        gameManager = new GameManager(this);
    }
    
    private void registerCommands() {
        getCommand("ctf").setExecutor(new CTFCommand(this));
        getCommand("ctfadmin").setExecutor(new CTFAdminCommand(this));
    }
    
    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
        Bukkit.getPluginManager().registerEvents(new GameListener(this), this);
        Bukkit.getPluginManager().registerEvents(new BlockListener(this), this);
        Bukkit.getPluginManager().registerEvents(chatManager, this);
    }
    
    private void startTasks() {
        // Game update task (every second)
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            gameManager.updateAllGames();
        }, 20L, 20L);
        
        // Scoreboard update task (every 2 seconds)
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            scoreboardManager.updateAllScoreboards();
            lobbyManager.updateAllLobbyScoreboards();
            tabListManager.updateAllTabLists();
            tabListManager.updateAllTabListHeaders();
        }, 40L, 40L);
    }
    
    // Getters
    public static ArenaWarsCTF getInstance() {
        return instance;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public ArenaManager getArenaManager() {
        return arenaManager;
    }
    
    public GameManager getGameManager() {
        return gameManager;
    }
    
    public PlayerManager getPlayerManager() {
        return playerManager;
    }
    
    public RespawnManager getRespawnManager() {
        return respawnManager;
    }
    
    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }
    
    public MessageUtil getMessageUtil() {
        return messageUtil;
    }
    
    public XPManager getXPManager() {
        return xpManager;
    }
    
    public MVPManager getMVPManager() {
        return mvpManager;
    }
    
    public LobbyManager getLobbyManager() {
        return lobbyManager;
    }
    
    public ChatManager getChatManager() {
        return chatManager;
    }
    
    public TabListManager getTabListManager() {
        return tabListManager;
    }
}