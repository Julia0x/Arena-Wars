/*
 * ArenaWarsCTF - Professional CTF Plugin
 * Copyright (c) 2025 ArenaWars Development Team - All Rights Reserved
 */

package com.arenawars.ctf.arena;

public enum ArenaState {
    DISABLED,    // Arena is disabled
    WAITING,     // Waiting for players to join
    STARTING,    // Game is starting (countdown)
    ACTIVE,      // Game is active
    ENDING,      // Game is ending (victory screen)
    RESETTING    // Arena is being reset
}