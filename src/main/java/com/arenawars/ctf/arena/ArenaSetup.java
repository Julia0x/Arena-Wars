/*
 * ArenaWarsCTF - Professional CTF Plugin
 * Copyright (c) 2025 ArenaWars Development Team - All Rights Reserved
 */

package com.arenawars.ctf.arena;

import com.arenawars.ctf.ArenaWarsCTF;
import com.arenawars.ctf.managers.ColorManager;
import com.arenawars.ctf.utils.MessageUtil;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ArenaSetup {
    
    private final ArenaWarsCTF plugin;
    private final Map<Player, SetupSession> setupSessions;
    private final ColorManager colorManager;
    
    public ArenaSetup(ArenaWarsCTF plugin) {
        this.plugin = plugin;
        this.setupSessions = new HashMap<>();
        this.colorManager = new ColorManager();
    }
    
    public void startSetup(Player player, String arenaName) {
        Arena arena = new Arena(arenaName);
        SetupSession session = new SetupSession(arena);
        setupSessions.put(player, session);
        
        giveSetupTools(player);
        showCurrentStep(player);
        
        Map<String, String> placeholders = MessageUtil.createPlaceholders("arena", arenaName);
        plugin.getMessageUtil().sendMessage(player, "arena.setup-mode-entered", placeholders);
    }
    
    public void endSetup(Player player) {
        setupSessions.remove(player);
        player.getInventory().clear();
        plugin.getMessageUtil().sendMessage(player, "arena.setup-mode-exited");
    }
    
    public boolean isInSetup(Player player) {
        return setupSessions.containsKey(player);
    }
    
    public SetupSession getSetupSession(Player player) {
        return setupSessions.get(player);
    }
    
    public void handleNextStep(Player player) {
        SetupSession session = setupSessions.get(player);
        if (session == null) return;
        
        session.nextStep();
        showCurrentStep(player);
        
        if (session.isComplete()) {
            completeSetup(player, session);
        }
    }
    
    public void setPosition(Player player, Location location) {
        SetupSession session = setupSessions.get(player);
        if (session == null) return;
        
        SetupStep currentStep = session.getCurrentStep();
        Arena arena = session.getArena();
        
        switch (currentStep) {
            case WAITING_LOBBY:
                arena.setWaitingLobby(location);
                plugin.getMessageUtil().sendMessage(player, "arena.position-set", 
                    MessageUtil.createPlaceholders("position", "Waiting lobby"));
                break;
            case SPECTATOR_POINT:
                arena.setSpectatorPoint(location);
                plugin.getMessageUtil().sendMessage(player, "arena.position-set", 
                    MessageUtil.createPlaceholders("position", "Spectator point"));
                break;
            case RED_FLAG:
                arena.setRedFlag(location);
                plugin.getMessageUtil().sendMessage(player, "arena.position-set", 
                    MessageUtil.createPlaceholders("position", "Red flag"));
                break;
            case BLUE_FLAG:
                arena.setBlueFlag(location);
                plugin.getMessageUtil().sendMessage(player, "arena.position-set", 
                    MessageUtil.createPlaceholders("position", "Blue flag"));
                break;
            case RED_FLAG_RETURN:
                arena.setRedFlagReturn(location);
                plugin.getMessageUtil().sendMessage(player, "arena.position-set", 
                    MessageUtil.createPlaceholders("position", "Red flag return"));
                break;
            case BLUE_FLAG_RETURN:
                arena.setBlueFlagReturn(location);
                plugin.getMessageUtil().sendMessage(player, "arena.position-set", 
                    MessageUtil.createPlaceholders("position", "Blue flag return"));
                break;
            case CORNER1:
                arena.setCorner1(location);
                plugin.getMessageUtil().sendMessage(player, "arena.position-set", 
                    MessageUtil.createPlaceholders("position", "Corner 1"));
                break;
            case CORNER2:
                arena.setCorner2(location);
                plugin.getMessageUtil().sendMessage(player, "arena.position-set", 
                    MessageUtil.createPlaceholders("position", "Corner 2"));
                break;
        }
        
        handleNextStep(player);
    }
    
    public void addSpawn(Player player, Location location, boolean isRed) {
        SetupSession session = setupSessions.get(player);
        if (session == null) return;
        
        Arena arena = session.getArena();
        
        if (isRed) {
            arena.getRedSpawns().add(location);
            plugin.getMessageUtil().sendMessage(player, "arena.position-set", 
                MessageUtil.createPlaceholders("position", "Red spawn " + arena.getRedSpawns().size()));
        } else {
            arena.getBlueSpawns().add(location);
            plugin.getMessageUtil().sendMessage(player, "arena.position-set", 
                MessageUtil.createPlaceholders("position", "Blue spawn " + arena.getBlueSpawns().size()));
        }
    }
    
    private void showCurrentStep(Player player) {
        SetupSession session = setupSessions.get(player);
        if (session == null) return;
        
        SetupStep step = session.getCurrentStep();
        String instruction = getStepInstruction(step);
        
        Map<String, String> placeholders = MessageUtil.createPlaceholders(
            "step", String.valueOf(step.ordinal() + 1),
            "instruction", instruction
        );
        
        plugin.getMessageUtil().sendMessage(player, "arena.setup-step", placeholders);
        
        // Update setup tools based on current step
        updateSetupTools(player, step);
    }
    
    private String getStepInstruction(SetupStep step) {
        switch (step) {
            case WAITING_LOBBY: return "Right-click to set waiting lobby position";
            case SPECTATOR_POINT: return "Right-click to set spectator point";
            case RED_SPAWNS: return "Right-click to add red team spawns (minimum 2, use red wool tool)";
            case BLUE_SPAWNS: return "Right-click to add blue team spawns (minimum 2, use blue wool tool)";
            case RED_FLAG: return "Right-click to set red flag position";
            case BLUE_FLAG: return "Right-click to set blue flag position";
            case RED_FLAG_RETURN: return "Right-click to set red flag return position";
            case BLUE_FLAG_RETURN: return "Right-click to set blue flag return position";
            case CORNER1: return "Right-click to set first corner of arena bounds";
            case CORNER2: return "Right-click to set second corner of arena bounds";
            case COMPLETE: return "Setup complete!";
            default: return "Unknown step";
        }
    }
    
    private void giveSetupTools(Player player) {
        player.getInventory().clear();
        
        // Position tool
        ItemStack positionTool = new ItemStack(Material.GOLDEN_SWORD);
        colorManager.colorizeItem(positionTool, "&6Position Tool", 
            Arrays.asList("&7Right-click to set positions"));
        
        // Red spawn tool
        ItemStack redSpawnTool = new ItemStack(Material.RED_WOOL);
        colorManager.colorizeItem(redSpawnTool, "&cRed Spawn Tool", 
            Arrays.asList("&7Right-click to add red spawns"));
        
        // Blue spawn tool
        ItemStack blueSpawnTool = new ItemStack(Material.BLUE_WOOL);
        colorManager.colorizeItem(blueSpawnTool, "&9Blue Spawn Tool", 
            Arrays.asList("&7Right-click to add blue spawns"));
        
        // Next step tool
        ItemStack nextTool = new ItemStack(Material.ARROW);
        colorManager.colorizeItem(nextTool, "&aNext Step", 
            Arrays.asList("&7Right-click to go to next step"));
        
        // Exit tool
        ItemStack exitTool = new ItemStack(Material.BARRIER);
        colorManager.colorizeItem(exitTool, "&cExit Setup", 
            Arrays.asList("&7Right-click to exit setup mode"));
        
        player.getInventory().setItem(0, positionTool);
        player.getInventory().setItem(1, redSpawnTool);
        player.getInventory().setItem(2, blueSpawnTool);
        player.getInventory().setItem(7, nextTool);
        player.getInventory().setItem(8, exitTool);
    }
    
    private void updateSetupTools(Player player, SetupStep step) {
        // Highlight the current tool based on the step
        ItemStack highlightedTool = null;
        
        switch (step) {
            case RED_SPAWNS:
                highlightedTool = new ItemStack(Material.RED_WOOL);
                colorManager.colorizeItem(highlightedTool, "&c&l>> Red Spawn Tool <<", 
                    Arrays.asList("&7Right-click to add red spawns", "&e&lCURRENT STEP"));
                player.getInventory().setItem(1, highlightedTool);
                break;
            case BLUE_SPAWNS:
                highlightedTool = new ItemStack(Material.BLUE_WOOL);
                colorManager.colorizeItem(highlightedTool, "&9&l>> Blue Spawn Tool <<", 
                    Arrays.asList("&7Right-click to add blue spawns", "&e&lCURRENT STEP"));
                player.getInventory().setItem(2, highlightedTool);
                break;
            default:
                highlightedTool = new ItemStack(Material.GOLDEN_SWORD);
                colorManager.colorizeItem(highlightedTool, "&6&l>> Position Tool <<", 
                    Arrays.asList("&7Right-click to set positions", "&e&lCURRENT STEP"));
                player.getInventory().setItem(0, highlightedTool);
                break;
        }
    }
    
    private void completeSetup(Player player, SetupSession session) {
        Arena arena = session.getArena();
        
        // Validate world exists
        World world = player.getWorld();
        if (world == null) {
            plugin.getMessageUtil().sendErrorMessage(player, "World not found! Please ensure you're in a valid world.");
            return;
        }
        
        if (!arena.isValid()) {
            // Show validation errors
            plugin.getMessageUtil().sendErrorMessage(player, "Arena setup is incomplete:");
            for (String error : arena.getValidationErrors()) {
                plugin.getMessageUtil().sendRawMessage(player, "&c- " + error);
            }
            return;
        }
        
        // Set world and region name
        arena.setWorldName(world.getName());
        arena.setRegionName(arena.getName().toLowerCase() + "_region");
        arena.setEnabled(true);
        
        // Create WorldGuard region
        if (createWorldGuardRegion(arena, world)) {
            plugin.getMessageUtil().sendSuccessMessage(player, "WorldGuard region created successfully!");
        } else {
            plugin.getMessageUtil().sendWarningMessage(player, "WorldGuard region creation failed, but arena was saved.");
        }
        
        // Save arena
        plugin.getArenaManager().saveArena(arena);
        
        // Send completion messages
        Map<String, String> placeholders = MessageUtil.createPlaceholders("arena", arena.getName());
        plugin.getMessageUtil().sendMessage(player, "arena.setup-complete", placeholders);
        
        plugin.getMessageUtil().sendSuccessMessage(player, "Arena '" + arena.getName() + "' has been created and enabled!");
        plugin.getMessageUtil().sendInfoMessage(player, "Players can now join this arena using /ctf join " + arena.getName());
        
        // Auto-exit setup mode
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            endSetup(player);
        }, 60L); // Wait 3 seconds before auto-exit
    }
    
    private boolean createWorldGuardRegion(Arena arena, World world) {
        try {
            if (arena.getCorner1() == null || arena.getCorner2() == null) {
                return false;
            }
            
            // Get WorldGuard region manager
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regionManager = container.get(BukkitAdapter.adapt(world));
            
            if (regionManager == null) {
                return false;
            }
            
            // Create region boundaries
            Location corner1 = arena.getCorner1();
            Location corner2 = arena.getCorner2();
            
            BlockVector3 min = BlockVector3.at(
                Math.min(corner1.getBlockX(), corner2.getBlockX()),
                Math.min(corner1.getBlockY(), corner2.getBlockY()),
                Math.min(corner1.getBlockZ(), corner2.getBlockZ())
            );
            
            BlockVector3 max = BlockVector3.at(
                Math.max(corner1.getBlockX(), corner2.getBlockX()),
                Math.max(corner1.getBlockY(), corner2.getBlockY()),
                Math.max(corner1.getBlockZ(), corner2.getBlockZ())
            );
            
            // Create the region
            ProtectedCuboidRegion region = new ProtectedCuboidRegion(arena.getRegionName(), min, max);
            
            // Set region flags (optional - customize as needed)
            // region.setFlag(Flags.BUILD, StateFlag.State.DENY);
            // region.setFlag(Flags.PVP, StateFlag.State.ALLOW);
            
            // Add region to manager
            regionManager.addRegion(region);
            
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create WorldGuard region for arena " + arena.getName() + ": " + e.getMessage());
            return false;
        }
    }
    
    public static class SetupSession {
        private final Arena arena;
        private SetupStep currentStep;
        
        public SetupSession(Arena arena) {
            this.arena = arena;
            this.currentStep = SetupStep.WAITING_LOBBY;
        }
        
        public Arena getArena() { return arena; }
        public SetupStep getCurrentStep() { return currentStep; }
        
        public void nextStep() {
            if (currentStep != SetupStep.COMPLETE) {
                currentStep = SetupStep.values()[currentStep.ordinal() + 1];
            }
        }
        
        public boolean isComplete() {
            return currentStep == SetupStep.COMPLETE;
        }
    }
    
    public enum SetupStep {
        WAITING_LOBBY,
        SPECTATOR_POINT,
        RED_SPAWNS,
        BLUE_SPAWNS,
        RED_FLAG,
        BLUE_FLAG,
        RED_FLAG_RETURN,
        BLUE_FLAG_RETURN,
        CORNER1,
        CORNER2,
        COMPLETE
    }
}