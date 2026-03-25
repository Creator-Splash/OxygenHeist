package com.creatorsplash.oxygenheist.application.match.zone;

import com.creatorsplash.oxygenheist.application.common.math.PlayerPositionProvider;
import com.creatorsplash.oxygenheist.application.match.oxygen.PlayerOxygenService;
import com.creatorsplash.oxygenheist.domain.match.MatchSession;
import com.creatorsplash.oxygenheist.domain.zone.CaptureZoneState;
import lombok.RequiredArgsConstructor;

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

    private final PlayerPositionProvider positionProvider;
    private final PlayerOxygenService oxygenService;

    private final double capturePerTick; // todo cfg
    private final int captureOxygenRestore;

    /**
     * Processes all capture zones for the current tick
     *
     * @param session the match session
     */
    public void tick(MatchSession session, ZonePresence presence) {
        for (CaptureZoneState zone : session.getZones()) {
            applyCapture(zone, presence, session);
        }
    }

    /**
     * Applies capture logic to a single zone
     */
    private void applyCapture(
        CaptureZoneState zone,
        ZonePresence presence,
        MatchSession session
    ) {
        if (presence.isEmpty(zone)) return;

        if (presence.isContested(zone)) return;

        String teamId = presence.getSingleTeam(zone).orElseThrow();

        if (zone.hasOwner() && !teamId.equals(zone.getOwnerTeamId())) {
            zone.regressCapture(teamId, capturePerTick);
            return;
        }

        zone.progressCapture(teamId, capturePerTick);

        if (zone.isFullyCaptured()) {
            zone.completeCapture();
            oxygenService.restoreTeamOxygen(session, teamId, captureOxygenRestore);
        }
    }

}
