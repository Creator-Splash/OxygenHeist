package com.creatorsplash.oxygenheist.domain.match.config;

/**
 * Match level config for oxygen handling
 */
public record OxygenConfig(
    double max,
    double drainPerTick,
    int depletionDownTicks
) {
    public static final OxygenConfig EMPTY =
        new OxygenConfig(300, 0.1, 200);
}
