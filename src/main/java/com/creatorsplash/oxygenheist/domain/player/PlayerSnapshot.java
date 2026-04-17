package com.creatorsplash.oxygenheist.domain.player;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Read-only data of a players match state
 */
public record PlayerSnapshot(
    UUID playerId,
    @Nullable String teamId,
    boolean alive,
    boolean downed,
    double oxygen,
    double maxOxygen,
    int score,
    int bleedoutTicks,
    int reviveProgressPercent
) {}
