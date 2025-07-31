/*
 * ArenaWarsCTF - Professional CTF Plugin
 * Copyright (c) 2025 ArenaWars Development Team - All Rights Reserved
 */

package com.arenawars.ctf.utils;

import com.arenawars.ctf.ArenaWarsCTF;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class MessageUtil {
    
    private final ArenaWarsCTF plugin;
    private final LegacyComponentSerializer serializer;
    
    public MessageUtil(ArenaWarsCTF plugin) {
        this.plugin = plugin;
        this.serializer = LegacyComponentSerializer.legacyAmpersand();
    }
    
    public String getMessage(String path) {
        return plugin.getConfigManager().getMessages().getString(path, "Message not found: " + path);
    }
    
    public String getMessage(String path, Map<String, String> placeholders) {
        String message = getMessage(path);
        
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        
        return message;
    }
    
    public void sendMessage(Player player, String path) {
        String message = getPrefix() + getMessage(path);
        Component component = serializer.deserialize(message);
        player.sendMessage(component);
    }
    
    public void sendMessage(Player player, String path, Map<String, String> placeholders) {
        String message = getPrefix() + getMessage(path, placeholders);
        Component component = serializer.deserialize(message);
        player.sendMessage(component);
    }
    
    public void sendRawMessage(Player player, String message) {
        Component component = serializer.deserialize(message);
        player.sendMessage(component);
    }
    
    public void sendTitle(Player player, String titlePath, String subtitlePath) {
        sendTitle(player, titlePath, subtitlePath, new HashMap<>());
    }
    
    public void sendTitle(Player player, String titlePath, String subtitlePath, Map<String, String> placeholders) {
        if (!plugin.getConfigManager().areTitlesEnabled()) return;
        
        String titleText = getMessage(titlePath, placeholders);
        String subtitleText = getMessage(subtitlePath, placeholders);
        
        Component title = serializer.deserialize(titleText);
        Component subtitle = serializer.deserialize(subtitleText);
        
        Title titleObj = Title.title(
            title,
            subtitle,
            Title.Times.times(
                Duration.ofMillis(500),
                Duration.ofSeconds(3),
                Duration.ofMillis(500)
            )
        );
        
        player.showTitle(titleObj);
    }
    
    public void sendActionBar(Player player, String message) {
        Component component = serializer.deserialize(message);
        player.sendActionBar(component);
    }
    
    public void playSound(Player player, Sound sound) {
        if (plugin.getConfigManager().areSoundsEnabled()) {
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        }
    }
    
    public void broadcast(String path) {
        String message = getPrefix() + getMessage(path);
        Component component = serializer.deserialize(message);
        plugin.getServer().broadcast(component);
    }
    
    public void broadcast(String path, Map<String, String> placeholders) {
        String message = getPrefix() + getMessage(path, placeholders);
        Component component = serializer.deserialize(message);
        plugin.getServer().broadcast(component);
    }
    
    public void broadcastToArena(String arenaName, String path) {
        // Implementation would depend on arena player tracking
        // This is a placeholder for arena-specific broadcasting
    }
    
    public String getPrefix() {
        return getMessage("prefix");
    }
    
    public Component colorize(String text) {
        return serializer.deserialize(text);
    }
    
    // Utility method to create placeholder maps easily
    public static Map<String, String> createPlaceholders(String... keyValuePairs) {
        Map<String, String> placeholders = new HashMap<>();
        
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            if (i + 1 < keyValuePairs.length) {
                placeholders.put(keyValuePairs[i], keyValuePairs[i + 1]);
            }
        }
        
        return placeholders;
    }
}