/*
 * ArenaWarsCTF - Professional CTF Plugin
 * Copyright (c) 2025 ArenaWars Development Team - All Rights Reserved
 */

package com.arenawars.ctf.storage;

import com.arenawars.ctf.ArenaWarsCTF;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ConfigManager {
    
    private final ArenaWarsCTF plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    private final String currentVersion = "1.0.0";
    
    public ConfigManager(ArenaWarsCTF plugin) {
        this.plugin = plugin;
        loadConfigs();
        checkConfigVersion();
    }
    
    private void loadConfigs() {
        // Save default config files
        plugin.saveDefaultConfig();
        if (!new File(plugin.getDataFolder(), "messages.yml").exists()) {
            plugin.saveResource("messages.yml", false);
        }
        
        // Load configurations
        config = plugin.getConfig();
        messages = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "messages.yml"));
        
        // Create arenas directory
        File arenasDir = new File(plugin.getDataFolder(), "arenas");
        if (!arenasDir.exists()) {
            arenasDir.mkdirs();
        }
        
        // Create player data directory
        File playerDataDir = new File(plugin.getDataFolder(), "playerdata");
        if (!playerDataDir.exists()) {
            playerDataDir.mkdirs();
        }
    }
    
    private void checkConfigVersion() {
        String configVersion = config.getString("config-version", "0.0.0");
        
        if (!configVersion.equals(currentVersion)) {
            plugin.getLogger().info("Config version mismatch. Updating from " + configVersion + " to " + currentVersion);
            updateConfig(configVersion);
        }
    }
    
    private void updateConfig(String oldVersion) {
        try {
            // Backup old config
            if (config.getBoolean("updates.backup-on-start", true)) {
                File configFile = new File(plugin.getDataFolder(), "config.yml");
                File backupFile = new File(plugin.getDataFolder(), "config_backup_" + oldVersion + ".yml");
                
                if (configFile.exists()) {
                    java.nio.file.Files.copy(configFile.toPath(), backupFile.toPath());
                    plugin.getLogger().info("Config backed up to: " + backupFile.getName());
                }
            }
            
            // Update config version
            config.set("config-version", currentVersion);
            
            // Add any missing default values without overwriting existing ones
            addMissingDefaults();
            
            // Save updated config
            plugin.saveConfig();
            plugin.reloadConfig();
            config = plugin.getConfig();
            
            plugin.getLogger().info("Config updated successfully to version " + currentVersion);
            
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to update config: " + e.getMessage());
        }
    }
    
    private void addMissingDefaults() {
        // Add any new config options that don't exist
        if (!config.contains("settings.spectator-time")) {
            config.set("settings.spectator-time", 5);
        }
        
        if (!config.contains("settings.spawn-protection-time")) {
            config.set("settings.spawn-protection-time", 5);
        }
        
        if (!config.contains("settings.spawn-protection-remove-on-hit")) {
            config.set("settings.spawn-protection-remove-on-hit", true);
        }
        
        if (!config.contains("updates.preserve-custom-settings")) {
            config.set("updates.preserve-custom-settings", true);
        }
    }
    
    public void reloadConfigs() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        messages = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "messages.yml"));
    }
    
    public FileConfiguration getConfig() {
        return config;
    }
    
    public FileConfiguration getMessages() {
        return messages;
    }
    
    public File getArenasFolder() {
        return new File(plugin.getDataFolder(), "arenas");
    }
    
    public File getPlayerDataFolder() {
        return new File(plugin.getDataFolder(), "playerdata");
    }
    
    // Configuration getters with defaults
    public int getMaxPlayersPerArena() {
        return config.getInt("settings.max-players-per-arena", 8);
    }
    
    public int getMinPlayersToStart() {
        return config.getInt("settings.min-players-to-start", 4);
    }
    
    public int getGameDuration() {
        return config.getInt("settings.game-duration", 900);
    }
    
    public int getRespawnDelay() {
        return config.getInt("settings.respawn-delay", 5);
    }
    
    public int getSpectatorTime() {
        return config.getInt("settings.spectator-time", 5);
    }
    
    public int getFlagReturnDelay() {
        return config.getInt("settings.flag-return-delay", 30);
    }
    
    public int getCapturesToWin() {
        return config.getInt("settings.captures-to-win", 3);
    }
    
    public int getSpawnProtectionTime() {
        return config.getInt("settings.spawn-protection-time", 5);
    }
    
    public boolean isSpawnProtectionRemoveOnHit() {
        return config.getBoolean("settings.spawn-protection-remove-on-hit", true);
    }
    
    public boolean isAutoCleanupEnabled() {
        return config.getBoolean("settings.auto-cleanup-after-game", true);
    }
    
    public int getTeleportDelay() {
        return config.getInt("settings.teleport-delay", 3);
    }
    
    public boolean areParticlesEnabled() {
        return config.getBoolean("settings.enable-particles", true);
    }
    
    public boolean areSoundsEnabled() {
        return config.getBoolean("settings.enable-sounds", true);
    }
    
    public boolean areTitlesEnabled() {
        return config.getBoolean("settings.enable-titles", true);
    }
}