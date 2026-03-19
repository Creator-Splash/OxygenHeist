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
     * Awards points to a player
     *
     * @param playerId the player receiving points
     * @param points the amount of points awarded
     * @param reason a description of why points were awarded
     */
    void awardPoints(UUID playerId, int points, String reason);

    /**
     * Called when the match ends
     *
     * @param finalScores a map of player IDs to their final scores
     * @param winner a string identifier for the winning team or player
     */
    void onGameEnd(Map<UUID, Integer> finalScores, String winner);

}
