package com.creatorsplash.oxygenheist.application.match.zone;

import com.creatorsplash.oxygenheist.application.match.oxygen.PlayerOxygenService;
import com.creatorsplash.oxygenheist.domain.match.MatchSession;
import com.creatorsplash.oxygenheist.domain.zone.CaptureZoneState;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles capture zone progression during an active match
 *
 * <p>Responsible for:
 * <ul>
 *     <li>Determining which teams are present in each zone</li>
 *     <li>Applying capture progression or regression</li>
 *     <li>Handling contested zones</li>
 *     <li>Completing captures and triggering rewards</li>
 * </ul>
 * </p>
 */
@RequiredArgsConstructor
public class CaptureService {

    private final PlayerOxygenService oxygenService;

    /**
     * Processes all capture zones for the current tick
     *
     * @param session the match session
     */
    public List<CaptureEvent> tick(MatchSession session, ZonePresence presence) {
        List<CaptureEvent> events = new ArrayList<>();

        for (CaptureZoneState zone : session.getZones()) {
            CaptureEvent event = applyCapture(zone, presence, session);
            if (event != null) events.add(event);
        }

        return events;
    }

    /**
     * Applies capture logic to a single zone
     */
    private CaptureEvent applyCapture(
        CaptureZoneState zone,
        ZonePresence presence,
        MatchSession session
    ) {
        var config = session.config().zones();

        double capturePerTick = config.captureRatePerTick();
        int captureOxygenRestore = config.captureOxygenRestore();

        if (presence.isEmpty(zone)) return null;
        if (presence.isContested(zone)) {
            return new CaptureEvent(CaptureEvent.Type.CONTESTED, null, zone, 0);
        }

        String teamId = presence.getSingleTeam(zone).orElseThrow();

        // Owner is standing in their own zone - award holding points and stop
        if (zone.hasOwner() && teamId.equals(zone.getOwnerTeamId())) {
            session.addTeamScore(teamId, config.holdingPointsPerTick());
            return null;
        }

        // Enemy team is contesting an owned zone - regress
        if (zone.hasOwner() && !teamId.equals(zone.getOwnerTeamId())) {
            zone.regressCapture(teamId, capturePerTick);
            return null;
        }

        // Neutral or re-capturing team progresses
        zone.progressCapture(teamId, capturePerTick);

        if (zone.isFullyCaptured()) {
            zone.completeCapture();
            oxygenService.restoreTeamOxygen(session, teamId, captureOxygenRestore);
            return new CaptureEvent(CaptureEvent.Type.CAPTURED, teamId, zone, captureOxygenRestore);
        }

        return new CaptureEvent(CaptureEvent.Type.CAPTURING, teamId, zone, 0);
    }

    /**
     * Represents a completed zone capture event for a single tick
     *
     * @param teamId the team that captured the zone
     * @param zone the zone that was captured
     * @param oxygenRestored the amount of oxygen restored to the capturing team
     */
    public record CaptureEvent(
        Type type,
        String teamId,
        CaptureZoneState zone,
        int oxygenRestored
    ) {
        public enum Type { CAPTURED, CONTESTED, CAPTURING }
    }

}
