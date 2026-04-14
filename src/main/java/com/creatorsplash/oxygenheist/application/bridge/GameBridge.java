package com.creatorsplash.oxygenheist.application.bridge;

import java.util.Map;
import java.util.UUID;

/**
 * Abstraction layer between the game runtime and any external systems
 */
public interface GameBridge {

    /**
     * Called when a match officially begins
     */
    void onGameStart();

    /**
     * Called when a player is eliminated from the match
     *
     * @param playerId the unique ID of the eliminated player
     * @param reason human-readable reason for elimination
     */
    void onPlayerEliminated(UUID playerId, String reason);

    /**
     * Fired when points are awarded to an individual player
     *
     * @param playerId the player receiving points
     * @param points the amount awarded
     * @param reason the reason for the award
     */
    void awardPlayerPoints(UUID playerId, int points, ScoreReason reason);

    /**
     * Fired when points are awarded to a team
     *
     * @param teamId the team receiving points
     * @param points the amount awarded
     * @param reason the reason for the award
     */
    void awardTeamPoints(String teamId, int points, ScoreReason reason);

    /**
     * Called when the match ends
     *
     * @param finalPlayerScores map of player IDs to their final scores
     * @param finalTeamScores map of team id to team score
     * @param winner a string identifier for the winning team or player
     */
    void onGameEnd(
        Map<UUID, Integer> finalPlayerScores,
        Map<String, Integer> finalTeamScores,
        String winner
    );

    /**
     * Typed reason for a score event - allows the network layer to distinguish
     * between score sources without parsing strings
     */
    enum ScoreReason {
        KILL,
        CAPTAIN_KILL,
        ZONE_CAPTURE,
        ZONE_HOLDING
    }

}
