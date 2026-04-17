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
    double captureOxygenRestoreThreshold,

    double displayMainHeight,   // height of main zone hologram above zone surface
    double displayTeamHeight   // height of per-team holograms above zone surface
) {

    public enum ReplenishMode { PER_PLAYER, DRAIN_SPLIT }

    public static final MatchZoneConfig EMPTY = new MatchZoneConfig(
        0.2,
        0.4,
        0.3,
        5,
        100.0 / 120.0,
        5,
        (100.0 / 120.0) * 0.5,
        50,
        1,
        2.0,
        ReplenishMode.PER_PLAYER,
        5,
        80.0,
        2.8,
        4.4
    );
}
