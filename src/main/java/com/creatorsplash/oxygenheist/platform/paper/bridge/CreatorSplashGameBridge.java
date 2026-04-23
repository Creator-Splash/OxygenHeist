package com.creatorsplash.oxygenheist.platform.paper.bridge;

import com.creatorsplash.oxygenheist.application.bridge.GameBridge;
import creatorsplash.creatorsplashcore.api.ProxyConnector;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.Map;
import java.util.UUID;

public final class CreatorSplashGameBridge implements GameBridge {

    private static final String GAME_ID = "oxygenheist";

    private final ProxyConnector connector;

    public CreatorSplashGameBridge(ProxyConnector connector) {
        this.connector = connector;
    }

    @Override
    public void onGameStart() {
        connector.notifyGameStarted(GAME_ID);
        connector.setGameActive(true);
    }

    @Override
    public void onPlayerEliminated(UUID playerId, String reason) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(playerId);
        String name = op.getName() != null ? op.getName() : playerId.toString();
        connector.notifyPlayerEliminated(playerId, name, reason);
    }

    @Override
    public void awardPlayerPoints(UUID playerId, int points, ScoreReason reason) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(playerId);
        String name = op.getName() != null ? op.getName() : playerId.toString();
        connector.awardPoints(playerId, name, points, reason.name());
    }

    @Override
    public void awardTeamPoints(String teamId, int points, ScoreReason reason) {
        connector.broadcastGameEvent(
            "TEAM_AWARD",
            teamId,
            Map.of("points", points, "reason", reason.name())
        );
    }

    @Override
    public void onGameEnd(
        Map<UUID, Integer> finalPlayerScores,
        Map<String, Integer> finalTeamScores,
        String winner
    ) {
        connector.updateLeaderboard(finalPlayerScores);
        connector.notifyGameEnded(GAME_ID, finalPlayerScores, winner == null ? "" : winner);
        connector.setGameActive(false);
    }
}
