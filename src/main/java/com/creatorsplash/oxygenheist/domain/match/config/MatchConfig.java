package com.creatorsplash.oxygenheist.domain.match.config;

import com.creatorsplash.oxygenheist.domain.zone.config.ZoneConfig;

public record MatchConfig(
    // Match
    int durationSeconds,

    // Sub configs
    OxygenConfig oxygen,
    DownedConfig downed,
    ZoneConfig zones
) {
    public static final MatchConfig EMPTY = new MatchConfig(
        0,
        new OxygenConfig(300, 0.1, 200),
        new DownedConfig(30, 60, 3.0, 5L),
        new ZoneConfig(
            0.05,
            100.0 / 120.0,
            5,
            (100.0 / 120.0) * 0.5,
            50
        )
    );
}
