package com.creatorsplash.oxygenheist.application.match.zone;

import com.creatorsplash.oxygenheist.application.common.math.FullPosition;
import com.creatorsplash.oxygenheist.application.common.math.PlayerPositionProvider;
import com.creatorsplash.oxygenheist.domain.match.MatchSession;
import com.creatorsplash.oxygenheist.domain.zone.CaptureZoneState;
import lombok.RequiredArgsConstructor;

import java.util.*;

/**
 * Provides zone related queries for match systems
 */
@RequiredArgsConstructor
public class ZoneService {

    private final PlayerPositionProvider positionProvider;

    /**
     * Checks whether a player is currently inside a zone owned by their team
     *
     * @param playerId the player UUID
     * @return true if the player is inside a friendly owned zone
     */
    public boolean isPlayerInOwnedZone(
        MatchSession session,
        UUID playerId
    ) {
        String teamId = session.getPlayerTeam(playerId);
        if (teamId == null) return false;

        return getZonesAt(session, playerId).stream()
            .anyMatch(zone -> teamId.equals(zone.getOwnerTeamId()));
    }

    /**
     * Gets a zone the player is currently inside
     * <p>Falls back to direct position lookup</p>
     *
     * @param session the match session
     * @param playerId the player UUID
     * @return optional zone
     */
    public Optional<CaptureZoneState> getZoneAt(
        MatchSession session,
        UUID playerId
    ) {
        FullPosition position = positionProvider.getPosition(playerId);
        if (position == null) return Optional.empty();

        for (CaptureZoneState zone : session.getZones()) {
            if (zone.isInside(position)) return Optional.of(zone);
        }

        return Optional.empty();
    }

    /**
     * Gets all zones the player is currently inside
     *
     * @param session the match session
     * @param playerId the player UUID
     * @return collection of zones
     */
    public Collection<CaptureZoneState> getZonesAt(
        MatchSession session,
        UUID playerId
    ) {
        FullPosition position = positionProvider.getPosition(playerId);
        if (position == null) return Collections.emptyList();

        List<CaptureZoneState> result = new ArrayList<>();

        for (CaptureZoneState zone : session.getZones()) {
            if (zone.isInside(position)) result.add(zone);
        }

        return result;
    }

}
