package com.creatorsplash.oxygenheist.application.bridge;

import java.util.Map;
import java.util.UUID;

/**
 * Standalone implementation of {@link GameBridge}
 *
 * <p>This implementation performs no external communication and is used
 * during development or when running the game without a proxy/tournament system</p>
 */
public class StandaloneGameBridge implements GameBridge {

    @Override
    public void onGameStart() { /* no op */ }

    @Override
    public void onPlayerEliminated(UUID playerId, String reason) { /* no op */ }

    @Override
    public void awardPlayerPoints(UUID playerId, int points, ScoreReason reason) {
        /* no op */
    }

    @Override
    public void awardTeamPoints(String teamId, int points, ScoreReason reason) {
        /* no op */
    }

    @Override
    public void onGameEnd(
        Map<UUID, Integer> finalPlayerScores,
        Map<String, Integer> finalTeamScores,
        String winner
    ) { /* no op */ }

}
