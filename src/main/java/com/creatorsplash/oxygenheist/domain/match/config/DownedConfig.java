package com.creatorsplash.oxygenheist.domain.match.config;

/**
 * Match level config for handling player downing
 */
public record DownedConfig(
    int bleedoutSeconds,
    int reviveTicks,

    double reviveMaxDistance,
    long reviveIntentTtlTicks
) {
    public static final DownedConfig EMPTY =
        new DownedConfig(30, 60, 3.0, 5L);
}
