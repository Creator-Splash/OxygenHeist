package com.creatorsplash.oxygenheist.application.match;

import com.creatorsplash.oxygenheist.application.bridge.GameBridge;
import com.creatorsplash.oxygenheist.application.combat.DownedService;
import com.creatorsplash.oxygenheist.domain.match.MatchSession;
import com.creatorsplash.oxygenheist.domain.match.MatchState;
import com.creatorsplash.oxygenheist.domain.player.PlayerMatchState;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Primary orchestration service for match lifecycle and high-level gameplay actions
 *
 * <p>This class coordinates match creation, start, end, and player-related actions.
 * It delegates integration events through {@link GameBridge}</p>
 */
@RequiredArgsConstructor
public final class MatchService {

    @Getter
    private final Scheduler scheduler;
    private final GameBridge gameBridge;

    private final DownedService downedService;

    private MatchSession session;

    private Scheduler.Task downedTask;

    /**
     * Initializes a new match session
     */
    public void createMatch() {
        this.session = new MatchSession();
    }

    /**
     * Starts the current match
     *
     * @throws IllegalStateException if no match has been created
     */
    public void startMatch() {
        if (session == null) {
            throw new IllegalStateException("Match not created");
        }

        session.setState(MatchState.PLAYING);
        startTasks();

        gameBridge.onGameStart();
    }

    /**
     * Ends the current match and reports results
     *
     * @param winner the winning team of player identifier
     */
    public void endMatch(String winner) {
        if (session == null) return;

        session.setState(MatchState.ENDING);
        stopTasks();

        Map<UUID, Integer> scores = new HashMap<>();
        for (PlayerMatchState player : session.getPlayers()) {
            scores.put(player.getPlayerId(), player.getScore());
        }

        gameBridge.onGameEnd(scores, winner);

        session = null;
    }

    /**
     * Adds a player to the current match
     *
     * @param playerId the players UUID
     */
    public void addPlayer(UUID playerId) {
        session.getOrCreatePlayer(playerId);
    }

    /**
     * Removes a player from the current match
     *
     * @param playerId the players UUID
     */
    public void removePlayer(UUID playerId) {
        session.removePlayer(playerId);
    }

    /**
     * Eliminates a player from the match
     *
     * @param playerId the players UUID
     * @param reason reason for elimination
     */
    public void eliminatePlayer(UUID playerId, String reason) {
        PlayerMatchState player = session.getOrCreatePlayer(playerId);

        player.eliminate();

        gameBridge.onPlayerEliminated(playerId, reason);
    }

    /**
     * Awards points to a player
     *
     * @param playerId the players UUID
     * @param amount points to award
     * @param reason reason for the points
     */
    public void awardPoints(UUID playerId, int amount, String reason) {
        PlayerMatchState player = session.getOrCreatePlayer(playerId);

        player.addScore(amount);

        gameBridge.awardPoints(playerId, amount, reason);
    }

    /**
     * @return the current match session, if present
     */
    public Optional<MatchSession> getSession() {
        return Optional.ofNullable(session);
    }

    /* Internals */

    private void startTasks() {
        stopTasks();

        this.downedTask = scheduler.runRepeating(
            () -> {
                if (session == null) return;

                this.downedService.tick(session, playerId ->
                    eliminatePlayer(playerId, "Bled out"));
            }, 1L, 1L
        );

        // TODO
    }

    private void stopTasks() {
       if (stopTask(this.downedTask)) this.downedTask = null;
    }

    private boolean stopTask(@Nullable Scheduler.Task task) {
        if (task != null) {
            task.cancel();
            return true;
        }
        return false;
    }

}
