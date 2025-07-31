/*
 * ArenaWarsCTF - Professional CTF Plugin
 * Copyright (c) 2025 ArenaWars Development Team - All Rights Reserved
 */

package com.arenawars.ctf.arena;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Arena {
    
    private String name;
    private String displayName;
    private String worldName;
    private String regionName;
    private ArenaState state;
    
    // Positions
    private Location waitingLobby;
    private Location spectatorPoint;
    private List<Location> redSpawns;
    private List<Location> blueSpawns;
    private Location redFlag;
    private Location blueFlag;
    private Location redFlagReturn;
    private Location blueFlagReturn;
    
    // Arena bounds for cleanup
    private Location corner1;
    private Location corner2;
    
    // Settings
    private int maxPlayers;
    private boolean enabled;
    
    public Arena(String name) {
        this.name = name;
        this.displayName = name;
        this.state = ArenaState.DISABLED;
        this.redSpawns = new ArrayList<>();
        this.blueSpawns = new ArrayList<>();
        this.maxPlayers = 8;
        this.enabled = false;
    }
    
    // Save arena to file
    public void save(File file) throws IOException {
        YamlConfiguration config = new YamlConfiguration();
        
        config.set("name", name);
        config.set("display-name", displayName);
        config.set("world", worldName);
        config.set("region", regionName);
        config.set("enabled", enabled);
        config.set("max-players", maxPlayers);
        
        // Save positions
        if (waitingLobby != null) {
            saveLocation(config, "waiting-lobby", waitingLobby);
        }
        
        if (spectatorPoint != null) {
            saveLocation(config, "spectator-point", spectatorPoint);
        }
        
        if (redFlag != null) {
            saveLocation(config, "red-flag", redFlag);
        }
        
        if (blueFlag != null) {
            saveLocation(config, "blue-flag", blueFlag);
        }
        
        if (redFlagReturn != null) {
            saveLocation(config, "red-flag-return", redFlagReturn);
        }
        
        if (blueFlagReturn != null) {
            saveLocation(config, "blue-flag-return", blueFlagReturn);
        }
        
        if (corner1 != null) {
            saveLocation(config, "corner1", corner1);
        }
        
        if (corner2 != null) {
            saveLocation(config, "corner2", corner2);
        }
        
        // Save spawn lists
        saveLocationList(config, "red-spawns", redSpawns);
        saveLocationList(config, "blue-spawns", blueSpawns);
        
        config.save(file);
    }
    
    // Load arena from file
    public static Arena load(File file) throws IOException {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        
        String name = config.getString("name");
        Arena arena = new Arena(name);
        
        arena.displayName = config.getString("display-name", name);
        arena.worldName = config.getString("world");
        arena.regionName = config.getString("region");
        arena.enabled = config.getBoolean("enabled", false);
        arena.maxPlayers = config.getInt("max-players", 8);
        
        // Load positions
        arena.waitingLobby = loadLocation(config, "waiting-lobby");
        arena.spectatorPoint = loadLocation(config, "spectator-point");
        arena.redFlag = loadLocation(config, "red-flag");
        arena.blueFlag = loadLocation(config, "blue-flag");
        arena.redFlagReturn = loadLocation(config, "red-flag-return");
        arena.blueFlagReturn = loadLocation(config, "blue-flag-return");
        arena.corner1 = loadLocation(config, "corner1");
        arena.corner2 = loadLocation(config, "corner2");
        
        // Load spawn lists
        arena.redSpawns = loadLocationList(config, "red-spawns");
        arena.blueSpawns = loadLocationList(config, "blue-spawns");
        
        return arena;
    }
    
    private void saveLocation(YamlConfiguration config, String path, Location location) {
        config.set(path + ".world", location.getWorld().getName());
        config.set(path + ".x", location.getX());
        config.set(path + ".y", location.getY());
        config.set(path + ".z", location.getZ());
        config.set(path + ".yaw", location.getYaw());
        config.set(path + ".pitch", location.getPitch());
    }
    
    private static Location loadLocation(YamlConfiguration config, String path) {
        if (!config.contains(path + ".world")) {
            return null;
        }
        
        String worldName = config.getString(path + ".world");
        double x = config.getDouble(path + ".x");
        double y = config.getDouble(path + ".y");
        double z = config.getDouble(path + ".z");
        float yaw = (float) config.getDouble(path + ".yaw");
        float pitch = (float) config.getDouble(path + ".pitch");
        
        return new Location(org.bukkit.Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
    }
    
    private void saveLocationList(YamlConfiguration config, String path, List<Location> locations) {
        config.set(path, null); // Clear existing
        
        for (int i = 0; i < locations.size(); i++) {
            saveLocation(config, path + "." + i, locations.get(i));
        }
    }
    
    private static List<Location> loadLocationList(YamlConfiguration config, String path) {
        List<Location> locations = new ArrayList<>();
        
        if (config.contains(path)) {
            ConfigurationSection section = config.getConfigurationSection(path);
            if (section != null) {
                for (String key : section.getKeys(false)) {
                    Location location = loadLocation(config, path + "." + key);
                    if (location != null) {
                        locations.add(location);
                    }
                }
            }
        }
        
        return locations;
    }
    
    // Validation
    public boolean isValid() {
        return worldName != null && 
               isWorldValid() &&
               waitingLobby != null &&
               spectatorPoint != null &&
               !redSpawns.isEmpty() &&
               !blueSpawns.isEmpty() &&
               redFlag != null &&
               blueFlag != null &&
               redFlagReturn != null &&
               blueFlagReturn != null &&
               corner1 != null &&
               corner2 != null;
    }
    
    public boolean isWorldValid() {
        return worldName != null && org.bukkit.Bukkit.getWorld(worldName) != null;
    }
    
    public List<String> getValidationErrors() {
        List<String> errors = new ArrayList<>();
        
        if (worldName == null) errors.add("World not set");
        else if (!isWorldValid()) errors.add("World '" + worldName + "' does not exist");
        if (waitingLobby == null) errors.add("Waiting lobby not set");
        if (spectatorPoint == null) errors.add("Spectator point not set");
        if (redSpawns.isEmpty()) errors.add("Red team spawns not set (minimum 2 required)");
        else if (redSpawns.size() < 2) errors.add("Red team needs at least 2 spawns (currently: " + redSpawns.size() + ")");
        if (blueSpawns.isEmpty()) errors.add("Blue team spawns not set (minimum 2 required)");
        else if (blueSpawns.size() < 2) errors.add("Blue team needs at least 2 spawns (currently: " + blueSpawns.size() + ")");
        if (redFlag == null) errors.add("Red flag position not set");
        if (blueFlag == null) errors.add("Blue flag position not set");
        if (redFlagReturn == null) errors.add("Red flag return position not set");
        if (blueFlagReturn == null) errors.add("Blue flag return position not set");
        if (corner1 == null || corner2 == null) errors.add("Arena bounds not set (both corners required)");
        
        return errors;
    }
    
    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    
    public String getWorldName() { return worldName; }
    public void setWorldName(String worldName) { this.worldName = worldName; }
    
    public String getRegionName() { return regionName; }
    public void setRegionName(String regionName) { this.regionName = regionName; }
    
    public ArenaState getState() { return state; }
    public void setState(ArenaState state) { this.state = state; }
    
    public Location getWaitingLobby() { return waitingLobby; }
    public void setWaitingLobby(Location waitingLobby) { this.waitingLobby = waitingLobby; }
    
    public Location getSpectatorPoint() { return spectatorPoint; }
    public void setSpectatorPoint(Location spectatorPoint) { this.spectatorPoint = spectatorPoint; }
    
    public List<Location> getRedSpawns() { return redSpawns; }
    public void setRedSpawns(List<Location> redSpawns) { this.redSpawns = redSpawns; }
    
    public List<Location> getBlueSpawns() { return blueSpawns; }
    public void setBlueSpawns(List<Location> blueSpawns) { this.blueSpawns = blueSpawns; }
    
    public Location getRedFlag() { return redFlag; }
    public void setRedFlag(Location redFlag) { this.redFlag = redFlag; }
    
    public Location getBlueFlag() { return blueFlag; }
    public void setBlueFlag(Location blueFlag) { this.blueFlag = blueFlag; }
    
    public Location getRedFlagReturn() { return redFlagReturn; }
    public void setRedFlagReturn(Location redFlagReturn) { this.redFlagReturn = redFlagReturn; }
    
    public Location getBlueFlagReturn() { return blueFlagReturn; }
    public void setBlueFlagReturn(Location blueFlagReturn) { this.blueFlagReturn = blueFlagReturn; }
    
    public Location getCorner1() { return corner1; }
    public void setCorner1(Location corner1) { this.corner1 = corner1; }
    
    public Location getCorner2() { return corner2; }
    public void setCorner2(Location corner2) { this.corner2 = corner2; }
    
    public int getMaxPlayers() { return maxPlayers; }
    public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }
    
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}