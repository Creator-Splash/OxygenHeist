package com.creatorsplash.oxygenheist.application.bridge.display;

import com.creatorsplash.oxygenheist.domain.match.MatchSnapshot;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

/**
 * Match Display orchestrator
 *
 * <p>Consumes match snapshots and determines what should be rendered</p>
 */
public interface MatchDisplayService {

    /**
     * Called every tick after a new snapshot is produced
     */
    void render(MatchSnapshot snapshot);

    /**
     * Called when match starts
     */
    void onMatchStarted();

    /**
     * Called when match ends
     */
    void onMatchEnd(String winner);

    /**
     * Called when a zone is contested by a multiple teams
     */
    void onZoneContested(String zoneId);

    default void onZoneCapturing(String zoneId, String teamId) {}

    /**
     * Called when a zone is captured by a team
     *
     * @param teamId the id of the team that captured the zone
     * @param teamName the display name of the capturing team
     * @param zoneName the display name of the captured zone
     * @param oxygenRestored the amount of oxygen restored to the team
     * @param teamMemberIds UUIDs of all capturing team members (to receive the oxygen restore message)
     */
    void onZoneCaptured(
        String teamId, String teamName, String zoneName,
        int oxygenRestored, Set<UUID> teamMemberIds
    );

    /** Called once when instant death mode activates */
    void onInstantDeathActivated();

    /**
     * Clear all UI
     */
    void clearAll();

    /* Player Hooks */

    /**
     * Called when a player leaves or is removed from the match
     */
    void onPlayerRemoved(UUID playerId);

    /** Called when a player is knocked into the downed state */
    void onPlayerDowned(
        UUID playerId,
        @Nullable UUID attackerId,
        Set<UUID> teammateIds
    );

    default void onReviveProgress(UUID targetId, UUID reviverId) {}

    /** Called when a downed player is successfully revived */
    void onPlayerRevived(UUID downedId, UUID reviverId);

    /**
     * Called when a player is fully eliminated from the match
     *
     * @param wasInstantDeath true if eliminated directly without bleedout
     */
    void onPlayerEliminated(UUID playerId, boolean wasInstantDeath);

    /**
     * Called when a kill reward is awarded to a player
     * <p>Implementations should notify the attacker</p>
     */
    void onKillReward(UUID attackerId, UUID victimId, int points);

    /**
     * Called when a captain kill bonus is awarded
     */
    void onCaptainKillBonus(UUID attackerId, UUID victimId, int bonus);

    default void showBarsToNewPlayer(UUID playerId) {}

}
