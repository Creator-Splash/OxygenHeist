package com.creatorsplash.oxygenheist.application.match.zone;

import com.creatorsplash.oxygenheist.application.common.math.FullPosition;
import com.creatorsplash.oxygenheist.application.common.math.PlayerPositionProvider;
import com.creatorsplash.oxygenheist.domain.match.MatchSession;
import com.creatorsplash.oxygenheist.domain.player.PlayerMatchState;
import com.creatorsplash.oxygenheist.domain.zone.CaptureZoneState;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class ZonePresenceService {

    private final PlayerPositionProvider positionProvider;

    public ZonePresence compute(MatchSession session) {
        // zone -> (team -> player count)
        Map<CaptureZoneState, Map<String, Integer>> zoneTeamCounts = new HashMap<>();

        // Player -> Zones
        for (PlayerMatchState player : session.getPlayers()) {
            if (!player.isActive()) continue;

            String teamId = session.getPlayerTeam(player.getPlayerId());
            if (teamId == null) continue;

            FullPosition position = positionProvider.getPosition(player.getPlayerId());
            if (position == null) continue;

            // Check player against all zones
            for (CaptureZoneState zone : session.getZones()) {
                if (!zone.isInside(position)) continue;

                zoneTeamCounts.computeIfAbsent(zone, ignored -> new HashMap<>())
                    .merge(teamId, 1, Integer::sum);
            }
        }

        return new ZonePresence(zoneTeamCounts);
    }

}
