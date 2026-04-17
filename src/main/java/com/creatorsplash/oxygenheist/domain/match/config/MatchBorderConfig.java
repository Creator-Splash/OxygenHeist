package com.creatorsplash.oxygenheist.domain.match.config;

/**
 * Match level config for border timing
 */
public record MatchBorderConfig(
    int shrinkDelaySeconds,
    int shrinkDurationSeconds,
    double shrinkSizePercent,
    double minimumSize
) {
    public static final MatchBorderConfig EMPTY =
        new MatchBorderConfig(60, 300, 20.0, 10.0);
}
