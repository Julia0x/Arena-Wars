/*
 * ArenaWarsCTF - Professional CTF Plugin
 * Copyright (c) 2025 ArenaWars Development Team - All Rights Reserved
 */

package com.arenawars.ctf.listeners;

import com.arenawars.ctf.ArenaWarsCTF;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockListener implements Listener {

    private final ArenaWarsCTF plugin;

    public BlockListener(ArenaWarsCTF plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        // Always allow breaking in setup mode for admins
        if (plugin.getArenaManager().isInSetup(player)) {
            if (player.hasPermission("arenawars.ctf.setup")) {
                return;
            }
        }

        // Check if in arena world
        if (plugin.getArenaManager().isArenaWorld(event.getBlock().getWorld().getName())) {
            // Cancel block breaking in arena worlds unless admin
            if (!player.hasPermission("arenawars.ctf.admin")) {
                event.setCancelled(true);
                return;
            }
        }

        // Use WorldGuard regions if available
        if (isInProtectedRegion(player, event.getBlock().getLocation())) {
            if (!player.hasPermission("arenawars.ctf.admin")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        // Always allow placing in setup mode for admins
        if (plugin.getArenaManager().isInSetup(player)) {
            if (player.hasPermission("arenawars.ctf.setup")) {
                return;
            }
        }

        // Check if in arena world
        if (plugin.getArenaManager().isArenaWorld(event.getBlock().getWorld().getName())) {
            // Cancel block placing in arena worlds unless admin
            if (!player.hasPermission("arenawars.ctf.admin")) {
                event.setCancelled(true);
                return;
            }
        }

        // Use WorldGuard regions if available
        if (isInProtectedRegion(player, event.getBlock().getLocation())) {
            if (!player.hasPermission("arenawars.ctf.admin")) {
                event.setCancelled(true);
            }
        }
    }

    private boolean isInProtectedRegion(Player player, org.bukkit.Location location) {
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regions = container.get(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(location.getWorld()));

            if (regions != null) {
                // Use BlockVector3 instead of Location.at()
                BlockVector3 vector = BlockVector3.at(location.getX(), location.getY(), location.getZ());
                ApplicableRegionSet set = regions.getApplicableRegions(vector);

                // Check if any region has build deny flag or is arena region
                for (com.sk89q.worldguard.protection.regions.ProtectedRegion region : set) {
                    String regionName = region.getId().toLowerCase();
                    if (regionName.contains("arena") || regionName.contains("ctf")) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            // WorldGuard integration failed, fallback to basic protection
            plugin.getLogger().warning("WorldGuard integration error: " + e.getMessage());
        }

        return false;
    }
}