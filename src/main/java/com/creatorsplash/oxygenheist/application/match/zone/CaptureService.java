package com.creatorsplash.oxygenheist.application.match.zone;

import com.creatorsplash.oxygenheist.application.match.oxygen.PlayerOxygenService;
import com.creatorsplash.oxygenheist.domain.match.MatchSession;
import com.creatorsplash.oxygenheist.domain.match.config.MatchZoneConfig;
import com.creatorsplash.oxygenheist.domain.zone.CaptureZoneState;
import com.creatorsplash.oxygenheist.domain.zone.ZoneTeamOxygenState;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
        MatchZoneConfig config = session.config().zones();

        // Only teams that can actually participate in capture this tick
        // Evacuating teams and teams still in recapture cooldown are excluded
        Set<String> eligibleTeams = presence.getTeamsInZone(zone).stream()
            .filter(teamId -> zone.getOrCreateZoneOxygen(teamId).canRecapture()
                    || teamId.equals(zone.getOwnerTeamId()))
            .collect(Collectors.toSet());

        boolean contested = eligibleTeams.size() > 1;
        boolean empty = eligibleTeams.isEmpty();

        zone.setContested(contested);

        if (contested) {
            String capturingTeam = zone.getCapturingTeamId();
            if (!zone.hasOwner() && capturingTeam != null && !eligibleTeams.contains(capturingTeam)) {
                zone.regressCapture(config.regressRatePerTick());
            }

            return new CaptureEvent(CaptureEvent.Type.CONTESTED, null, zone, 0);
        }

        if (empty) return handleEmpty(zone, config);

        zone.clearRestoreCooldown();

        String teamId = presence.getSingleTeam(zone).orElseThrow();

        if (zone.hasOwner()) {
            return handleOwnedZone(zone, teamId, session, config);
        }

        return handleUnownedZone(zone, teamId, session, config);
    }

    private CaptureEvent handleEmpty(CaptureZoneState zone, MatchZoneConfig config) {
        if (!zone.hasOwner() && zone.getCaptureProgress() > 0) {
            zone.regressCapture(config.regressRatePerTick());
        } else if (zone.hasOwner() && zone.getCaptureProgress() < 100.0) {
            if (!zone.isRestoreCooldownActive()) {
                zone.startRestoreCooldown(config.restoreCooldownSeconds() * 20);
            }
            zone.tickRestoreCooldown();
            if (zone.isRestoreCooldownComplete()) {
                zone.progressCapture(zone.getOwnerTeamId(), config.restoreRatePerTick());
                if (zone.getCaptureProgress() >= 100.0) {
                    zone.clearRestoreCooldown();
                }
            }
        }
        return null;
    }

    private CaptureEvent handleOwnedZone(
        CaptureZoneState zone,
        String teamId,
        MatchSession session,
        MatchZoneConfig config
    ) {
        if (teamId.equals(zone.getOwnerTeamId())) {
            session.addTeamScore(teamId, config.holdingPointsPerTick());

            // If capture was partially regressed while the owner was off-point,
            // restore it now that the owner has successfully defended and returned
            if (zone.getCaptureProgress() < 100.0) {
                zone.progressCapture(teamId, config.restoreRatePerTick());
            }
        } else {
            zone.regressCapture(config.regressRatePerTick());
        }
        return null;
    }

    private CaptureEvent handleUnownedZone(
        CaptureZoneState zone,
        String teamId,
        MatchSession session,
        MatchZoneConfig config
    ) {
        // Block recapture until this teams zone oxygen has fully refilled
        if (!zone.getOrCreateZoneOxygen(teamId).canRecapture()) return null;

        // Different team entered - regress the current capturer's progress
        if (zone.getCapturingTeamId() != null && !zone.getCapturingTeamId().equals(teamId)) {
            zone.regressCapture(config.regressRatePerTick());
            return null;
        }

        // Same team or fresh neutral zone - progress
        zone.progressCapture(teamId, config.captureRatePerTick());

        if (zone.isFullyCaptured()) {
            ZoneTeamOxygenState oxygenState = zone.getOrCreateZoneOxygen(teamId);
            double oxygenAtCapture = oxygenState.getOxygenPercent();

            zone.completeCapture();
            oxygenState.resetToNormal();

            int oxygenRestored = 0;
            if (oxygenAtCapture >= config.captureOxygenRestoreThreshold()) {
                oxygenService.restoreTeamOxygen(session, teamId, config.captureOxygenRestore());
                oxygenRestored = config.captureOxygenRestore();
            }

            return new CaptureEvent(CaptureEvent.Type.CAPTURED, teamId, zone, oxygenRestored);
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
