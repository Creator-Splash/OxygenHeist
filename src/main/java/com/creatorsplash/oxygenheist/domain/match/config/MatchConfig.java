package com.creatorsplash.oxygenheist.domain.match.config;

/**
 * Full match level config
 */
public record MatchConfig(
    // Match
    int durationSeconds,
    int countdownSeconds,
    int instantDeathSecondsRemaining,
    boolean globalFriendlyFire,

    // Sub configs
    MatchBorderConfig border,
    OxygenConfig oxygen,
    DownedConfig downed,
    MatchZoneConfig zones
) {
    public static final MatchConfig EMPTY = new MatchConfig(
        600,
        10,
        120,
        false,
        MatchBorderConfig.EMPTY,
        OxygenConfig.EMPTY,
        DownedConfig.EMPTY,
        MatchZoneConfig.EMPTY
    );
}
