package com.creatorsplash.oxygenheist.domain.match;

import com.creatorsplash.oxygenheist.domain.player.PlayerSnapshot;
import com.creatorsplash.oxygenheist.domain.zone.ZoneSnapshot;

import java.util.List;

/**
 * Read-only data of match session state
 */
public record MatchSnapshot(
    long tick,
    List<PlayerSnapshot> players,
    List<ZoneSnapshot> zones
) {}
