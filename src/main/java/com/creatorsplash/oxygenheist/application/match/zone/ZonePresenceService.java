package com.creatorsplash.oxygenheist.application.match.zone;

import com.creatorsplash.oxygenheist.application.common.math.FullPosition;
import com.creatorsplash.oxygenheist.application.common.math.PlayerPositionProvider;
import com.creatorsplash.oxygenheist.domain.match.MatchSession;
import com.creatorsplash.oxygenheist.domain.player.PlayerMatchState;
import com.creatorsplash.oxygenheist.domain.zone.CaptureZoneState;
import lombok.RequiredArgsConstructor;

import java.util.*;

@RequiredArgsConstructor
public class ZonePresenceService {

    private final PlayerPositionProvider positionProvider;

    public ZonePresence compute(MatchSession session) {
        Map<CaptureZoneState, Map<String, Set<UUID>>> zoneTeamPlayers = new HashMap<>();

        for (PlayerMatchState player : session.getPlayers()) {
            if (!player.isActive()) continue;
            String teamId = session.getPlayerTeam(player.getPlayerId());
            if (teamId == null) continue;
            FullPosition position = positionProvider.getPosition(player.getPlayerId());
            if (position == null) continue;

            for (CaptureZoneState zone : session.getZones()) {
                if (!zone.isInside(position)) continue;
                zoneTeamPlayers
                    .computeIfAbsent(zone, ignored -> new HashMap<>())
                    .computeIfAbsent(teamId, ignored -> new HashSet<>())
                    .add(player.getPlayerId());
            }
        }

        return new ZonePresence(zoneTeamPlayers);
    }

}
