package com.creatorsplash.oxygenheist.domain.player;

import java.util.UUID;

/**
 * Read-only data of a players match state
 */
public record PlayerSnapshot(
    UUID playerId,
    boolean alive,
    boolean downed,
    int oxygen,
    int maxOxygen,
    int score
) {}
