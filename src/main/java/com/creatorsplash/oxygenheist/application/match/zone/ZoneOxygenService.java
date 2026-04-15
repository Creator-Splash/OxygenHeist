package com.creatorsplash.oxygenheist.application.match.zone;

import com.creatorsplash.oxygenheist.application.bridge.display.MatchDisplayService;
import com.creatorsplash.oxygenheist.application.match.oxygen.PlayerOxygenService;
import com.creatorsplash.oxygenheist.domain.match.MatchSession;
import com.creatorsplash.oxygenheist.domain.zone.CaptureZoneState;
import com.creatorsplash.oxygenheist.domain.zone.ZoneTeamOxygenState;
import com.creatorsplash.oxygenheist.domain.match.config.MatchZoneConfig;
import lombok.RequiredArgsConstructor;

import java.util.*;

/**
 * Handles per-team oxygen pressure on capture zones during an active match
 *
 * <p>Phase lifecycle:
 * <ol>
 *   <li>NORMAL - draining while owned and occupied; owner players are replenished</li>
 *   <li>EVACUATING - hit 0%; zone neutralized; waiting for team to leave</li>
 *   <li>REFILLING - team fully left; oxygen recovers passively</li>
 *   <li>NORMAL - full again; team may recapture</li>
 * </ol>
 * </p>
 */
@RequiredArgsConstructor
public class ZoneOxygenService {

    private final MatchDisplayService displayService;
    private final PlayerOxygenService playerOxygenService;

    /**
     * Processes zone oxygen for all zones in the match
     *
     * @param session the active match session
     */
    public void tick(MatchSession session, ZonePresence presence) {
        MatchZoneConfig config = session.config().zones();

        for (CaptureZoneState zone : session.getZones()) {
            Map<String, Integer> teamCounts = presence.getTeamCounts(zone);
            processPresentOwner(config, zone, teamCounts, presence, session);
            processAbsentTeams(config, zone, teamCounts);
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

    private void processPresentOwner(
        MatchZoneConfig config,
        CaptureZoneState zone,
        Map<String, Integer> teamCounts,
        ZonePresence presence,
        MatchSession session
    ) {
        double drainPerTick = config.drainPercentPerSecond() / 20.0;
        int maxMultiplier = config.maxDrainMultiplier();

        for (Map.Entry<String, Integer> entry : teamCounts.entrySet()) {
            String teamId = entry.getKey();
            int playerCount = entry.getValue();

            // Zone oxygen only applies to the team that owns the zone
            if (!teamId.equals(zone.getOwnerTeamId())) continue;

            ZoneTeamOxygenState oxygenState = zone.getOrCreateZoneOxygen(teamId);

            switch (oxygenState.getPhase()) {
                case EVACUATING -> { /* Team still present after depletion - wait, do nothing */ }
                // Team wandered back in during refill
                case REFILLING -> oxygenState.refill(config.refillPercentPerSecond() / 20.0);
                case NORMAL -> {
                    int drainMultiplier = Math.clamp(playerCount, 1, maxMultiplier);
                    double totalDrain = drainPerTick * drainMultiplier;

                    boolean justDepleted = oxygenState.drain(totalDrain);

                    if (justDepleted) {
                        zone.neutralize();
                        displayService.onZoneOxygenDepleted(
                            zone.getId(),
                            teamId,
                            presence.getPlayersForTeam(zone, teamId)
                        );
                    } else {
                        // Replenish personal oxygen for present owner-team players
                        Set<UUID> presentPlayers = presence.getPlayersForTeam(zone, teamId);
                        if (!presentPlayers.isEmpty()) {
                            double replenish = calculateReplenish(
                                config, totalDrain, playerCount
                            );
                            playerOxygenService.replenishPlayersInZone(
                                    session, presentPlayers, replenish
                            );
                        }
                    }
                }
            }
        }
    }

    private void processAbsentTeams(
        MatchZoneConfig config,
        CaptureZoneState zone,
        Map<String, Integer> teamCounts
    ) {
        double refillPerTick = config.refillPercentPerSecond() / 20.0;

        for (Map.Entry<String, ZoneTeamOxygenState> entry : zone.getZoneOxygen().entrySet()) {
            String teamId = entry.getKey();
            ZoneTeamOxygenState oxygenState = entry.getValue();

            if (teamCounts.containsKey(teamId)) continue;

            switch (oxygenState.getPhase()) {
                case EVACUATING -> {
                    // Team just fully vacated - begin refill
                    oxygenState.beginRefill();
                    oxygenState.refill(refillPerTick);
                }
                case REFILLING -> oxygenState.refill(refillPerTick);
                case NORMAL -> { /* nothing to do */ }
            }
        }
    }

    private double calculateReplenish(MatchZoneConfig config, double totalDrain, int playerCount) {
        return switch (config.replenishMode()) {
            case PER_PLAYER -> config.replenishPlayerPerSecond() / 20.0;
            case DRAIN_SPLIT -> playerCount > 0 ? totalDrain / playerCount : 0;
        };
    }

}
