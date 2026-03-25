package com.creatorsplash.oxygenheist.domain.match;

import com.creatorsplash.oxygenheist.domain.match.config.MatchConfig;
import com.creatorsplash.oxygenheist.domain.player.PlayerSnapshot;
import com.creatorsplash.oxygenheist.domain.zone.ZoneSnapshot;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

/**
 * Read-only data of match session state
 */
public record MatchSnapshot(
    long tick,

    MatchState state,
    MatchConfig config,

    int remainingTicks,

    Map<UUID, PlayerSnapshot> players,
    Map<String, ZoneSnapshot> zones
) {

    public static final MatchSnapshot EMPTY = new MatchSnapshot(
        0L,
        MatchState.WAITING,
        null,
        0,
        Map.of(),
        Map.of()
    );

    public int remainingSeconds() {
        return Math.max(0, remainingTicks / 20);
    }

    public boolean isTimeExpired() {
        return remainingTicks <= 0;
    }

    public @Nullable PlayerSnapshot getPlayer(UUID playerId) {
        return players.get(playerId);
    }

    public @Nullable ZoneSnapshot getZone(String zoneId) {
        return zones.get(zoneId);
    }
}
