package com.creatorsplash.oxygenheist.application.match.zone;

import com.creatorsplash.oxygenheist.domain.match.MatchSession;
import com.creatorsplash.oxygenheist.domain.zone.CaptureZoneState;
import com.creatorsplash.oxygenheist.domain.zone.ZoneTeamOxygenState;
import lombok.RequiredArgsConstructor;

import java.util.*;

/**
 * Handles per-team oxygen pressure on capture zones during an active match
 *
 * <p>Responsible for:
 * <ul>
 *     <li>Tracking which teams are present on each zone</li>
 *     <li>Draining zone oxygen for present team</li>
 *     <li>Scaling drain by player count on the zone</li>
 *     <li>Switching teams into refil mode at 0%</li>
 *     <li>Refilling teams until their zone oxygen returns to 100%</li>
 * </ul>
 * </p>
 */
@RequiredArgsConstructor
public class ZoneOxygenService {

    // TODO replace with options data class for cfg
    private static final double DEFAULT_DRAIN_TIME_SECONDS = 120.0; // cfg
    private static final double DRAIN_PER_TICK = 100.0 / (DEFAULT_DRAIN_TIME_SECONDS * 20.0); // cfg?
    private static final int MAX_DRAIN_MULTIPLIER = 5;

    private static final double REFILL_PER_TICK = DRAIN_PER_TICK * 0.5; // cfg

    private final ZonePresenceService presenceService;

    /**
     * Processes zone oxygen for all zones in the match
     *
     * @param session the active match session
     */
    public void tick(MatchSession session, ZonePresence presence) {
        for (CaptureZoneState zone : session.getZones()) {
            Map<String, Integer> teamCounts = presence.getTeamCounts(zone);

            processPresentTeams(zone, teamCounts);
            processAbsentRefillingTeams(zone, teamCounts);
        }
    }

    /**
     * Gets the current zone oxygen percentage for a team on a zone
     *
     * @param zone the zone
     * @param teamId the team id
     * @return the teams zone oxygen percentage
     */
    public double getZoneOxygenPercent(CaptureZoneState zone, String teamId) {
        return zone.getOrCreateZoneOxygen(teamId).getOxygenPercent();
    }

    /* Internals */

    private void processPresentTeams(CaptureZoneState zone, Map<String, Integer> teamCounts) {
        for (Map.Entry<String, Integer> entry : teamCounts.entrySet()) {
            String teamId = entry.getKey();
            int playerCount = entry.getValue();

            ZoneTeamOxygenState oxygenState = zone.getOrCreateZoneOxygen(teamId);

            if (oxygenState.isRefilling()) {
                oxygenState.refill(REFILL_PER_TICK);
                continue;
            }

            int drainMultiplier = Math.clamp(playerCount, 1, MAX_DRAIN_MULTIPLIER);
            oxygenState.drain(DRAIN_PER_TICK * drainMultiplier);
        }
    }

    private void processAbsentRefillingTeams(CaptureZoneState zone, Map<String, Integer> teamCounts) {
        for (Map.Entry<String, ZoneTeamOxygenState> entry : zone.getZoneOxygen().entrySet()) {
            String teamId = entry.getKey();
            ZoneTeamOxygenState oxygenState = entry.getValue();

            if (!teamCounts.containsKey(teamId) && oxygenState.isRefilling()) {
                oxygenState.refill(REFILL_PER_TICK);
            }
        }
    }

}
