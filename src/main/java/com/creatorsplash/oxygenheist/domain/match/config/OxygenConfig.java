package com.creatorsplash.oxygenheist.domain.match.config;

/**
 * Match level config for oxygen handling
 */
public record OxygenConfig(
    double max,
    double drainPerTick,
    double suffocationDamage,
    double lowOxygenThreshold
) {
    public static final OxygenConfig EMPTY =
        new OxygenConfig(300, 0.1, 2.0,
            0.2);
}
