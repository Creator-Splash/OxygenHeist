package com.creatorsplash.oxygenheist.domain.match.config;

/**
 * Full match level config
 */
public record MatchConfig(
    // Match
    int durationSeconds,
    int countdownSeconds,
    int instantDeathSecondsRemaining,

    // Sub configs
    BorderConfig border,
    OxygenConfig oxygen,
    DownedConfig downed,
    MatchZoneConfig zones
) {
    public static final MatchConfig EMPTY = new MatchConfig(
        600,
        10,
        120,
        BorderConfig.EMPTY,
        OxygenConfig.EMPTY,
        DownedConfig.EMPTY,
        MatchZoneConfig.EMPTY
    );
}
