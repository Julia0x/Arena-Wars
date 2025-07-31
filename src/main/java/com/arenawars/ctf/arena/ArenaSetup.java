/*
 * ArenaWarsCTF - Professional CTF Plugin
 * Copyright (c) 2025 ArenaWars Development Team - All Rights Reserved
 */

package com.arenawars.ctf.arena;

import com.arenawars.ctf.ArenaWarsCTF;
import com.arenawars.ctf.utils.MessageUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ArenaSetup {
    
    private final ArenaWarsCTF plugin;
    private final Map<Player, SetupSession> setupSessions;
    
    public ArenaSetup(ArenaWarsCTF plugin) {
        this.plugin = plugin;
        this.setupSessions = new HashMap<>();
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
    }
    
    private String getStepInstruction(SetupStep step) {
        switch (step) {
            case WAITING_LOBBY: return "Right-click to set waiting lobby position";
            case SPECTATOR_POINT: return "Right-click to set spectator point";
            case RED_SPAWNS: return "Right-click to add red team spawns (minimum 2)";
            case BLUE_SPAWNS: return "Right-click to add blue team spawns (minimum 2)";
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
        ItemMeta meta = positionTool.getItemMeta();
        meta.setDisplayName("§6Position Tool");
        meta.setLore(Arrays.asList("§7Right-click to set positions"));
        positionTool.setItemMeta(meta);
        
        // Red spawn tool
        ItemStack redSpawnTool = new ItemStack(Material.RED_WOOL);
        ItemMeta redMeta = redSpawnTool.getItemMeta();
        redMeta.setDisplayName("§cRed Spawn Tool");
        redMeta.setLore(Arrays.asList("§7Right-click to add red spawns"));
        redSpawnTool.setItemMeta(redMeta);
        
        // Blue spawn tool
        ItemStack blueSpawnTool = new ItemStack(Material.BLUE_WOOL);
        ItemMeta blueMeta = blueSpawnTool.getItemMeta();
        blueMeta.setDisplayName("§9Blue Spawn Tool");
        blueMeta.setLore(Arrays.asList("§7Right-click to add blue spawns"));
        blueSpawnTool.setItemMeta(blueMeta);
        
        // Next step tool
        ItemStack nextTool = new ItemStack(Material.ARROW);
        ItemMeta nextMeta = nextTool.getItemMeta();
        nextMeta.setDisplayName("§aNext Step");
        nextMeta.setLore(Arrays.asList("§7Right-click to go to next step"));
        nextTool.setItemMeta(nextMeta);
        
        // Exit tool
        ItemStack exitTool = new ItemStack(Material.BARRIER);
        ItemMeta exitMeta = exitTool.getItemMeta();
        exitMeta.setDisplayName("§cExit Setup");
        exitMeta.setLore(Arrays.asList("§7Right-click to exit setup mode"));
        exitTool.setItemMeta(exitMeta);
        
        player.getInventory().setItem(0, positionTool);
        player.getInventory().setItem(1, redSpawnTool);
        player.getInventory().setItem(2, blueSpawnTool);
        player.getInventory().setItem(7, nextTool);
        player.getInventory().setItem(8, exitTool);
    }
    
    private void completeSetup(Player player, SetupSession session) {
        Arena arena = session.getArena();
        
        if (!arena.isValid()) {
            // Show validation errors
            for (String error : arena.getValidationErrors()) {
                plugin.getMessageUtil().sendRawMessage(player, "§c" + error);
            }
            return;
        }
        
        // Set world and region name
        arena.setWorldName(player.getWorld().getName());
        arena.setRegionName(arena.getName() + "_region");
        arena.setEnabled(true);
        
        // Save arena
        plugin.getArenaManager().saveArena(arena);
        
        // End setup
        endSetup(player);
        
        Map<String, String> placeholders = MessageUtil.createPlaceholders("arena", arena.getName());
        plugin.getMessageUtil().sendMessage(player, "arena.setup-complete", placeholders);
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