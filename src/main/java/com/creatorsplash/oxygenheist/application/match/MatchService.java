package com.creatorsplash.oxygenheist.application.match;

import com.creatorsplash.oxygenheist.application.bridge.GameBridge;
import com.creatorsplash.oxygenheist.application.common.LogCenter;
import com.creatorsplash.oxygenheist.application.common.debug.DebugFlags;
import com.creatorsplash.oxygenheist.application.common.math.PlayerPositionProvider;
import com.creatorsplash.oxygenheist.application.match.combat.DownedService;
import com.creatorsplash.oxygenheist.application.match.combat.revive.ReviveService;
import com.creatorsplash.oxygenheist.application.match.oxygen.PlayerOxygenService;
import com.creatorsplash.oxygenheist.application.match.zone.CaptureService;
import com.creatorsplash.oxygenheist.application.match.zone.ZoneOxygenService;
import com.creatorsplash.oxygenheist.application.match.zone.ZonePresence;
import com.creatorsplash.oxygenheist.application.match.zone.ZonePresenceService;
import com.creatorsplash.oxygenheist.domain.match.MatchSession;
import com.creatorsplash.oxygenheist.domain.match.MatchSnapshot;
import com.creatorsplash.oxygenheist.domain.match.MatchState;
import com.creatorsplash.oxygenheist.domain.player.PlayerMatchState;
import com.creatorsplash.oxygenheist.platform.paper.bootstrap.logging.MatchLogCenter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Primary orchestration service for match lifecycle and high-level gameplay actions
 *
 * <p>This class coordinates match creation, start, end, and player-related actions.
 * It delegates integration events through {@link GameBridge}</p>
 */
@RequiredArgsConstructor
public final class MatchService {

    @Getter
    private LogCenter log;

    @Getter
    private final Scheduler scheduler;
    private final GameBridge gameBridge;
    private final DebugFlags debugFlags;

    private final DownedService downedService;
    private final ReviveService reviveService;

    private final PlayerPositionProvider playerPositionProvider;
    private final CaptureService captureService;
    private final ZoneOxygenService zoneOxygenService;
    private final PlayerOxygenService playerOxygenService;
    private final ZonePresenceService zonePresenceService;

    private MatchSession session;

    private Scheduler.Task syncGameTask;
    private Scheduler.Task asyncGameTask;

    private final AtomicReference<MatchSnapshot> latestSnapshot = new AtomicReference<>();

    private long tickCounter = 0;

    /* == Match == */

    /**
     * Initializes a new match session
     */
    public void createMatch() {
        this.session = new MatchSession();

        this.log = new MatchLogCenter(
            UUID.randomUUID().toString().substring(0, 6),
            debugFlags
        );

        log.info("Match created");
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

        log.info("Match started");
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

        log.info("Match ended with winner " + winner);
    }

    /* == Player == */

    public boolean isPlayerInActiveMatch(UUID playerId) {
        return session != null
            && session.isPlaying()
            && session.getPlayer(playerId).isPresent();
    }

    /**
     * Adds a player to the current match
     *
     * @param playerId the players UUID
     */
    public void addPlayer(UUID playerId) {
        if (session == null) {
            throw new IllegalStateException("Match not created");
        }

        PlayerMatchState player = session.getOrCreatePlayer(playerId);
        player.initOxygen(100.0); // todo cfg
    }

    /**
     * Removes a player from the current match
     *
     * @param playerId the players UUID
     */
    public void removePlayer(UUID playerId) {
        if (session == null) {
            throw new IllegalStateException("Match not created");
        }

        session.removePlayer(playerId);

        log.debug("player", "Player " + playerId + " removed from match");
    }

    /**
     * Eliminates a player from the match
     *
     * @param playerId the players UUID
     * @param reason reason for elimination
     */
    public void eliminatePlayer(UUID playerId, String reason) {
        if (session == null) {
            throw new IllegalStateException("Match not created");
        }

        PlayerMatchState player = session.getOrCreatePlayer(playerId);

        player.eliminate();

        reviveService.cancelRevivesInvolving(playerId);

        gameBridge.onPlayerEliminated(playerId, reason);

        log.info("Player eliminated '" + playerId + "' reason: " + reason);
    }

    /**
     * Awards points to a player
     *
     * @param playerId the players UUID
     * @param amount points to award
     * @param reason reason for the points
     */
    public void awardPoints(UUID playerId, int amount, String reason) {
        if (session == null) {
            throw new IllegalStateException("Match not created");
        }

        PlayerMatchState player = session.getOrCreatePlayer(playerId);

        player.addScore(amount);

        gameBridge.awardPoints(playerId, amount, reason);

        log.debug("player", "Points awarded to "
            + playerId + " Points: " + amount + " Reason: " + reason);
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

        this.syncGameTask = scheduler.runRepeating(
            this::tickGame,
            1L,
            1L
        );

        this.asyncGameTask = scheduler.runRepeatingAsync(
            this::tickGameAsync,
            1L,
            1L
        );
    }

    private void stopTasks() {
       if (stopTask(this.syncGameTask)) this.syncGameTask = null;
       if (stopTask(this.asyncGameTask)) this.asyncGameTask = null;
    }

    private boolean stopTask(@Nullable Scheduler.Task task) {
        if (task != null) {
            task.cancel();
            return true;
        }
        return false;
    }

    private void tickGame() {
        if (session == null) return;

        /* Core Sim */

        tickCounter++;

        ZonePresence presence =
            zonePresenceService.compute(session);

        zoneOxygenService.tick(session, presence);
        captureService.tick(session, presence);
        playerOxygenService.tickDrain(session);

        /* Player State */

        downedService.tick(session, playerId ->
            eliminatePlayer(playerId, "Bled out"));

        reviveService.tick(
            session,
            playerPositionProvider,
            playerId -> {
                // TODO callbacks for display service or related
            }
        );

        /* Snapshot */

        MatchSnapshot snapshot = session.createSnapshot(tickCounter);
        latestSnapshot.set(snapshot);

        /* Debug */

        if (tickCounter % 100 != 0) return;

        session.getPlayers().forEach(player -> {
            String team = session.getPlayerTeam(player.getPlayerId());
            log.info(
                "Player=" + player.getPlayerId() +
                " Team=" + team +
                " 02=" + player.getOxygen() +
                " Downed=" + player.isDowned()
            );
        });
    }

    private void tickGameAsync() {
        MatchSnapshot snapshot = latestSnapshot.getAndSet(null);
        if (snapshot != null) handleSnapshot(snapshot);
    }

    private void handleSnapshot(MatchSnapshot snapshot) {
        // TODO
    }

}
