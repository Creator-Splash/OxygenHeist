package com.creatorsplash.oxygenheist.domain.player;

import java.util.UUID;

/**
 * Read-only data of a players match state
 */
public record PlayerSnapshot(
    UUID playerId,
    boolean alive,
    boolean downed,
    double oxygen,
    double maxOxygen,
    int score,
    int bleedoutTicks,
    int reviveProgressPercent
) {}
