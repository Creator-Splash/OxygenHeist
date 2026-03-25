package com.creatorsplash.oxygenheist.application.match.zone;

import com.creatorsplash.oxygenheist.domain.match.MatchSession;
import com.creatorsplash.oxygenheist.domain.zone.CaptureZoneState;
import com.creatorsplash.oxygenheist.domain.zone.ZoneTeamOxygenState;
import com.creatorsplash.oxygenheist.domain.match.config.MatchZoneConfig;
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

    private final ZonePresenceService presenceService;

    /**
     * Processes zone oxygen for all zones in the match
     *
     * @param session the active match session
     */
    public void tick(MatchSession session, ZonePresence presence) {
        MatchZoneConfig matchZoneConfig = session.config().zones();

        for (CaptureZoneState zone : session.getZones()) {
            Map<String, Integer> teamCounts = presence.getTeamCounts(zone);

            processPresentTeams(matchZoneConfig, zone, teamCounts);
            processAbsentRefillingTeams(matchZoneConfig, zone, teamCounts);
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

    private void processPresentTeams(
        MatchZoneConfig config,
        CaptureZoneState zone,
        Map<String, Integer> teamCounts
    ) {
        double drainPerTick = config.drainPercentPerSecond() / 20.0;
        double refillPerTick = config.refillPercentPerSecond() / 20.0;
        int maxMultiplier = config.maxDrainMultiplier();

        for (Map.Entry<String, Integer> entry : teamCounts.entrySet()) {
            String teamId = entry.getKey();
            int playerCount = entry.getValue();

            ZoneTeamOxygenState oxygenState = zone.getOrCreateZoneOxygen(teamId);

            if (oxygenState.isRefilling()) {
                oxygenState.refill(refillPerTick);
                continue;
            }

            int drainMultiplier = Math.clamp(playerCount, 1, maxMultiplier);
            oxygenState.drain(drainPerTick * drainMultiplier);
        }
    }

    private void processAbsentRefillingTeams(
        MatchZoneConfig config,
        CaptureZoneState zone,
        Map<String, Integer> teamCounts
    ) {
        double drainPerTick = config.drainPercentPerSecond() / 20.0;

        for (Map.Entry<String, ZoneTeamOxygenState> entry : zone.getZoneOxygen().entrySet()) {
            String teamId = entry.getKey();
            ZoneTeamOxygenState oxygenState = entry.getValue();

            if (!teamCounts.containsKey(teamId) && oxygenState.isRefilling()) {
                oxygenState.refill(drainPerTick);
            }
        }
    }

}
