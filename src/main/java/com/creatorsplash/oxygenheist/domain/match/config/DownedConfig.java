package com.creatorsplash.oxygenheist.domain.match.config;

public record DownedConfig(
    int bleedoutSeconds,
    int reviveTicks,

    double reviveMaxDistance,
    long reviveIntentTtlTicks
) {}
