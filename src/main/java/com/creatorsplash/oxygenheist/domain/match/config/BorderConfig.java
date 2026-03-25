package com.creatorsplash.oxygenheist.domain.match.config;

/**
 * Match level config for border timing
 */
public record BorderConfig(
    int shrinkDelaySeconds,
    int shrinkDurationSeconds,
    double shrinkSizePercent
) {
    public static final BorderConfig EMPTY =
        new BorderConfig(60, 300, 20.0);
}
