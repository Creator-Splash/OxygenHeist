package com.creatorsplash.oxygenheist.domain.match.config;

/**
 * Match level config for zones
 */
public record MatchZoneConfig(
    double captureRatePerTick,
    double regressRatePerTick,
    double restoreRatePerTick,
    int restoreCooldownSeconds,

    double drainPercentPerSecond,
    int maxDrainMultiplier,

    double refillPercentPerSecond,

    int captureOxygenRestore,

    int holdingPointsPerTick,

    double replenishPlayerPerSecond,
    ReplenishMode replenishMode,

    int recaptureCooldownSeconds,
    double captureOxygenRestoreThreshold
) {

    public enum ReplenishMode { PER_PLAYER, DRAIN_SPLIT }

    public static final MatchZoneConfig EMPTY = new MatchZoneConfig(
        0.05,
        0.1,
        0.075,
        5,
        100.0 / 120.0,
        5,
        (100.0 / 120.0) * 0.5,
        50,
        1,
        2.0,
        ReplenishMode.PER_PLAYER,
        5,
        80.0
    );
}
