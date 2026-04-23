package com.creatorsplash.oxygenheist.domain.match.config;

/**
 * Match level config for handling player downing
 */
public record DownedConfig(
    int bleedoutSeconds,
    int reviveTicks,

    double reviveMaxDistance,
    long reviveIntentTtlTicks,

    float labelViewRange,
    double labelHeightOffset,

    boolean invulnerableWhileDowned,
    int killCreditWindowSeconds
) {
    public static final DownedConfig EMPTY =
        new DownedConfig(
            30, 60,
            3.0, 5L,
            16F, 2.0,
            false,
            30
        );
}
