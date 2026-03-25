package com.creatorsplash.oxygenheist.application.match.oxygen;

import com.creatorsplash.oxygenheist.application.match.zone.ZonePresence;
import com.creatorsplash.oxygenheist.application.match.zone.ZoneService;
import com.creatorsplash.oxygenheist.domain.match.MatchSession;
import com.creatorsplash.oxygenheist.domain.player.PlayerMatchState;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/**
 * Handles player oxygen logic during an active match
 *
 * <p>Responsible for:
 * <ul>
 *     <li>Draining oxygen over time</li>
 *     <li>Restoring oxygen</li>
 *     <li>Skipping oxygen drain when players are inside owned zones</li>
 *     <li>Ignoring players who are not active (dead, downed, etc)</li>
 * </ul>
 * </p>
 */
@RequiredArgsConstructor
public class PlayerOxygenService {

    private final int drainAmountPerTick; // todo cfg?
    private final ZoneService zoneService;

    /**
     * Processes oxygen drain for all players in the match
     *
     * @param session the active match session
     */
    public void tickDrain(MatchSession session, ZonePresence presence) {
        for (PlayerMatchState player: session.getPlayers()) {
            if (!player.isActive() ||
                zoneService.isPlayerInOwnedZone(session, player.getPlayerId(), presence)) continue;

            player.drainOxygen(drainAmountPerTick);

            if (player.isOxygenDepleted()) {
                handleOxygenDepleted(player.getPlayerId());
            }
        }
    }

    /**
     * Restores oxygen for all active players in a team
     *
     * @param session the active match session
     * @param teamId the team identifier
     * @param amount the amount to restore
     */
    public void restoreTeamOxygen(MatchSession session, String teamId, int amount) {
        for (PlayerMatchState player : session.getPlayers()) {
            if (!player.isActive()) continue;

            // TODO player team?

            player.restoreOxygen(amount);
        }
    }

    /**
     * Gets the current oxygen level for a player
     *
     * @param session the match session
     * @param playerId the player UUID
     * @return oxygen value, or 0 if not found
     */
    public int getOxygen(MatchSession session, UUID playerId) {
        PlayerMatchState state = session.getPlayer(playerId).orElse(null);
        return state != null ? state.getOxygen() : 0;
    }

    protected void handleOxygenDepleted(UUID playerId) {
        // TODO?
    }

}
