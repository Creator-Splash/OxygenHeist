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
    public void awardPoints(UUID playerId, int points, String reason) { /* no op */ }

    @Override
    public void onGameEnd(Map<UUID, Integer> finalScores, String winner) { /* no op */ }

}
