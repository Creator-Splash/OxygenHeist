package com.creatorsplash.oxygenheist.application.match.zone;

import com.creatorsplash.oxygenheist.application.common.math.FullPosition;
import com.creatorsplash.oxygenheist.application.common.math.PlayerPositionProvider;
import com.creatorsplash.oxygenheist.application.match.oxygen.PlayerOxygenService;
import com.creatorsplash.oxygenheist.domain.match.MatchSession;
import com.creatorsplash.oxygenheist.domain.player.PlayerMatchState;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 *
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
    public void tick(MatchSession session) {
        for (CaptureZoneState zone : session.getZones()) {
            Map<String, Integer> teamCounts = new HashMap<>();

            for (PlayerMatchState player : session.getPlayers()) {
                if (!player.isActive()) continue;

                UUID playerId = player.getPlayerId();
                FullPosition pos = positionProvider.getPosition(playerId);
                if (pos == null) continue;

                if (!zone.isInside(pos.x(), pos.y(), pos.z())) continue;

                String teamId = ""; // TODO
                if (teamId == null) continue;

                teamCounts.merge(teamId, 1, Integer::sum);
            }

            // No players
            if (teamCounts.isEmpty()) continue;

            // Multiple teams contesting
            if (teamCounts.size() > 1) continue;

            // Only one team present
            String teamId = teamCounts.keySet().iterator().next();

            if (zone.hasOwner() && !teamId.equals(zone.getOwnerTeamId())) {
                // Enemy is trying to take zone -> regress
                zone.regressCapture(teamId, capturePerTick);
            } else {
                // Capture or reinforce
                zone.progressCapture(teamId, capturePerTick);

                if (zone.isFullyCaptured()) {
                    zone.completeCapture();

                    oxygenService.restoreTeamOxygen(session, teamId, captureOxygenRestore);
                }
            }
        }
    }

}
