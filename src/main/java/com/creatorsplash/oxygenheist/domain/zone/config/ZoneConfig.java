package com.creatorsplash.oxygenheist.domain.zone.config;

public record ZoneConfig(
    double captureRatePerTick,

    double drainPercentPerSecond,
    int maxDrainMultiplier,

    double refillPercentPerSecond,

    int captureOxygenRestore
) {}
