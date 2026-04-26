package com.creatorsplash.oxygenheist.application.match;

import com.creatorsplash.oxygenheist.application.bridge.GameBridge;
import com.creatorsplash.oxygenheist.application.bridge.GamePlayerService;
import com.creatorsplash.oxygenheist.application.bridge.GameWorldService;
import com.creatorsplash.oxygenheist.application.bridge.display.MatchDisplayService;
import com.creatorsplash.oxygenheist.application.common.LogCenter;
import com.creatorsplash.oxygenheist.application.common.math.PlayerPositionProvider;
import com.creatorsplash.oxygenheist.application.common.task.Scheduler;
import com.creatorsplash.oxygenheist.application.match.combat.DownedService;
import com.creatorsplash.oxygenheist.application.match.combat.revive.ReviveService;
import com.creatorsplash.oxygenheist.application.match.combat.revive.ReviveSession;
import com.creatorsplash.oxygenheist.application.match.oxygen.PlayerOxygenService;
import com.creatorsplash.oxygenheist.application.match.team.TeamService;
import com.creatorsplash.oxygenheist.application.match.zone.*;
import com.creatorsplash.oxygenheist.domain.match.MatchSession;
import com.creatorsplash.oxygenheist.domain.match.MatchSnapshot;
import com.creatorsplash.oxygenheist.domain.match.MatchState;
import com.creatorsplash.oxygenheist.domain.player.AttackCredit;
import com.creatorsplash.oxygenheist.domain.player.PlayerMatchState;
import com.creatorsplash.oxygenheist.domain.team.Team;
import com.creatorsplash.oxygenheist.domain.team.TeamSnapshot;
import com.creatorsplash.oxygenheist.domain.zone.CaptureZoneState;
import com.creatorsplash.oxygenheist.platform.paper.bootstrap.logging.MatchLogCenter;
import com.creatorsplash.oxygenheist.platform.paper.config.GlobalConfigService;
import com.creatorsplash.oxygenheist.platform.paper.config.match.MatchConfigService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.*;

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

    private final GlobalConfigService globals;
    private final MatchConfigService matchConfigService;
    private final MatchSnapshotProvider snapshotProvider;
    private final MatchDisplayService displayService;

    private final GamePlayerService playerService;
    private final GameWorldService worldService;

    @Getter
    private final Scheduler scheduler;
    private final GameBridge gameBridge;

    private final DownedService downedService;
    private final ReviveService reviveService;

    private final PlayerPositionProvider playerPositionProvider;
    private final CaptureService captureService;
    private final PlayerOxygenService playerOxygenService;
    private final ZoneOxygenService zoneOxygenService;
    private final ZonePresenceService zonePresenceService;
    private final ZoneProvider zoneProvider;
    private final TeamService teamService;

    private MatchSession session;

    private Scheduler.Task syncGameTask;
    private Scheduler.Task asyncGameTask;

    private long tickCounter = 0;

    private ZonePresence lastPresence = null;

    private final List<MatchLifecycle> externalLifecycles = new ArrayList<>();

    /**
     * Registers an external lifecycle participant
     *
     * <p>The registered instance will have {@link MatchLifecycle#onMatchEnd()} and
     * {@link MatchLifecycle#onPlayerLeave(UUID)} called at the appropriate moments.
     * Must be called before any match is created.</p>
     */
    public void registerLifecycle(MatchLifecycle lifecycle) {
        externalLifecycles.add(lifecycle);
    }

    public List<MatchLifecycle> readLifecycles() {
        return List.copyOf(externalLifecycles);
    }

    /* == Match == */

    public boolean isMatchActive() {
        return session != null && session.isPlaying();
    }

    /**
     * True when a match has been created and is in either the SETUP (cooldown)
     * or PLAYING phase. Different from {@link #isMatchActive()} which only
     * checks PLAYING: that one's too strict for the event-mode adapter
     * because players transfer in DURING the cooldown phase, not after.
     */
    public boolean hasActiveSession() {
        if (session == null) return false;
        com.creatorsplash.oxygenheist.domain.match.MatchState s = session.state();
        return s == com.creatorsplash.oxygenheist.domain.match.MatchState.SETUP
                || s == com.creatorsplash.oxygenheist.domain.match.MatchState.PLAYING;
    }

    /**
     * Initializes a new match session
     */
    public void createMatch() {
        this.session = new MatchSession(matchConfigService.get());

        this.log = new MatchLogCenter(
            UUID.randomUUID().toString().substring(0, 6),
            globals
        );

        // Load zones from config into session
        List<CaptureZoneState> zones = zoneProvider.loadZones();
        zones.forEach(session::addZone);

        if (zones.isEmpty()) {
            log.warn("Match created with no zones configured - use /oh zone set to add zones");
        }

        log.info("Match created");
    }

    /**
     * Starts the current match
     *
     * @throws IllegalStateException if no match has been created
     */
    public void startMatch(Set<UUID> activePlayerIds) {
        if (session == null) {
            throw new IllegalStateException("Match not created");
        }

        this.tickCounter = 0L;

        session.startCooldown();
        teamService.populateMatchSession(session);

        // Init match state for all teamed players only
        for (Team team : teamService.getAllTeams()) {
            for (UUID memberId : team.getMembers()) {
                if (!activePlayerIds.contains(memberId)) continue;
                session.getOrCreatePlayer(memberId)
                    .initOxygen(session.config().oxygen().max());
            }
        }

        externalLifecycles.forEach(l -> {
            try {
                l.onCountdownStart();
            } catch (Exception e) {
                log.error("Error in lifecycle countdown start for '"
                    + l.getClass().getSimpleName() + "' Reason: " , e);
            }
        });

        playerService.prepareForCountdown(session);

        startTasks();

        gameBridge.onGameStart();
        displayService.onMatchStart();

        log.info("Match started");
    }

    /**
     * Ends the current match and reports results
     *
     * @param winner winner the winning team identifier, or empty string if no winner
     */
    public void endMatch(String winner) {
        externalLifecycles.forEach(l -> {
            try {
                l.onMatchEnd();
            } catch (Exception e) {
                log.error("Error in lifecycle match end for '"
                    + l.getClass().getSimpleName() + "' Reason: " , e);
            }
        });

        if (session == null) return;

        session.state(MatchState.ENDING);
        stopTasks();

        Map<UUID, Integer> scores = new HashMap<>();
        for (PlayerMatchState player : session.getPlayers()) {
            scores.put(player.getPlayerId(), player.getScore());
        }

        playerService.cleanupAfterMatch(session);
        worldService.onMatchEnded();
        displayService.onMatchEnd(winner);

        gameBridge.onGameEnd(scores, session.getTeamScores(), winner);

        session = null;

        log.info("Match ended with winner " + winner);
    }

    /**
     * Ends the current match, resolving the winner by team score
     */
    public void endMatch() {
        endMatch(resolveWinner().orElseGet(this::resolveWinnerByScore));
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
        player.initOxygen(session.config().oxygen().max());
    }

    /**
     * Late-arrival per-player init for event mode. Runs the same teleport +
     * setup that {@code startMatch}'s player loop would have done if the
     * player had been online when the round started. No-op if no match is
     * active. Idempotent: calling multiple times is safe.
     */
    public void prepareLateArrival(UUID playerId) {
        if (session == null) return;
        playerService.prepareSinglePlayer(session, playerId);
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
        displayService.onPlayerRemoved(playerId);
        reviveService.cancelRevivesInvolving(playerId);

        externalLifecycles.forEach(l -> l.onPlayerLeave(playerId));
        checkWinCondition();

        log.debug("player", "Player " + playerId + " removed from match");
    }

    /**
     * Transitions a player into the downed state and notifies display
     *
     * <p>Called by combat systems when a player takes lethal damage</p>
     *
     * @param victimId the player to down
     */
    public void downPlayer(UUID victimId) {
        if (session == null) return;

        downedService.downPlayer(session, victimId);

        playerService.onPlayerDowned(victimId);

        @Nullable AttackCredit attackCredit = session.getPlayer(victimId)
            .map(PlayerMatchState::getLastAttack)
            .orElse(null);

        displayService.onPlayerDowned(victimId, attackCredit, getTeammates(victimId));
    }

    public void completeRevive(UUID downedId, UUID reviverId) {
        if (session == null) return;

        session.getPlayer(downedId).ifPresent(p -> {
            p.revive();
            p.restoreOxygen(session.config().oxygen().max());
        });

        playerService.onPlayerRevived(downedId);
        displayService.onPlayerRevived(downedId, reviverId);
    }

    /**
     * Eliminates a player from the match
     *
     * @param playerId the players UUID
     * @param reason reason for elimination
     */
    public void eliminatePlayer(UUID playerId, String reason) {
        if (session == null || !session.isPlaying()) return;

        PlayerMatchState player = session.getOrCreatePlayer(playerId);
        player.eliminate();

        AttackCredit credit = reason.equals("bleedout")
            ? player.lastAttackWithin(session.config().downed().killCreditWindowSeconds() * 1000L)
            : player.getLastAttack();

        reviveService.cancelRevivesInvolving(playerId);
        displayService.onPlayerEliminated(playerId, session.isInstantDeath(), credit);
        playerService.onPlayerEliminated(playerId, session);

        awardKillReward(playerId, credit, reason);
        gameBridge.onPlayerEliminated(playerId, credit, reason);

        checkWinCondition();

        log.info("Player eliminated '" + playerId + "' reason: " + reason);
    }

    private void awardKillReward(UUID victimId, @Nullable AttackCredit attackCredit, String reason) {
        if (attackCredit == null) return;

        PlayerMatchState victim = session.getOrCreatePlayer(victimId);

        if (reason.equals("bleedout")) {
            long windowMs = session.config().downed().killCreditWindowSeconds() * 1000L;
            if (victim.lastAttackWithin(windowMs) == null) return;
        }

        UUID attackerId = attackCredit.attackerId();
        String attackerTeamId = session.getPlayerTeam(attackerId);
        if (attackerTeamId == null) return;

        int reward = session.config().killReward();
        session.addTeamScore(attackerTeamId, reward);
        session.getPlayer(attackerId).ifPresent(p -> p.addScore(reward));

        gameBridge.awardPlayerPoints(attackerId, reward, GameBridge.ScoreReason.KILL);
        gameBridge.awardTeamPoints(attackerTeamId, reward, GameBridge.ScoreReason.KILL);

        displayService.onKillReward(attackerId, victimId, attackCredit, reward);

        String victimTeamId = session.getPlayerTeam(victimId);
        if (victimTeamId != null) {
            Team victimTeam = teamService.getTeam(victimTeamId);
            if (victimTeam != null && victimTeam.isCaptain(victimId)) {
                int bonus = session.config().captainKillBonus();
                session.addTeamScore(attackerTeamId, bonus);
                session.getPlayer(attackerId).ifPresent(p -> p.addScore(bonus));

                gameBridge.awardPlayerPoints(attackerId, reward, GameBridge.ScoreReason.CAPTAIN_KILL);
                gameBridge.awardTeamPoints(attackerTeamId, reward, GameBridge.ScoreReason.CAPTAIN_KILL);

                displayService.onCaptainKillBonus(attackerId, victimId, bonus);
            }
        }
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
        session.tickTimer();

        // External systems tick against current state first
        externalLifecycles.forEach(l -> {
            try {
                l.onGameTick(session);
            } catch (Exception e) {
                log.error("Error in lifecycle tick handler for '"
                    + l.getClass().getSimpleName() + "' Reason: " , e);
            }
        });

        // Start display immediately
        displayService.onMatchStart();

        switch (session.state()) {
            case SETUP -> {
                if (session.isTimeExpired()) {
                    session.startMatch();
                    playerService.prepareForMatch(session);
                    worldService.onMatchStarted(session.config());

                    externalLifecycles.forEach(l -> {
                        try {
                            l.onMatchStart();
                        } catch (Exception e) {
                            log.error("Error in lifecycle match start for '"
                                + l.getClass().getSimpleName() + "' Reason: " , e);
                        }
                    });
                }
            }

            case PLAYING -> {
                handleGameTick();
                if (session == null) return;

                if (session.shouldEnterInstantDeath()) {
                    session.enterInstantDeath();
                    displayService.onInstantDeathActivated();
                }

                if (session.shouldStartBorderShrink()) {
                    session.markBorderShrinkStarted();
                    worldService.onBorderShrinkStart(session.config());
                }

                if (session.isTimeExpired()) {
                    endMatch(resolveWinner().orElseGet(this::resolveWinnerByScore));
                }
            }

            case WAITING, ENDING -> { /* no-op */ }
        }

        /* Snapshot */

        // Get teams
        Map<String, TeamSnapshot> teamSnapshots = new HashMap<>();
        for (Team team : teamService.getAllTeams()) {
            teamSnapshots.put(team.getId(), team.toSnapshot(session.getTeamScore(team.getId())));
        }

        MatchSnapshot snapshot = session.createSnapshot(
            tickCounter,
            buildReviveProgressMap(),
            teamSnapshots,
            buildZonePresentTeams()
        );

        snapshotProvider.update(snapshot);

        // Trigger post tick updates
        externalLifecycles.forEach(l -> {
            try {
                l.readGameTick(snapshot);
            } catch (Exception e) {
                log.error("Error in lifecycle tick reader for '"
                    + l.getClass().getSimpleName() + "' Reason: " , e);
            }
        });

        /* Debug */

        if (tickCounter % 100 != 0) return;

        session.getPlayers().forEach(player -> {
            String team = session.getPlayerTeam(player.getPlayerId());
            log.debug("oxygen",
                "Player=" + player.getPlayerId() +
                " Team=" + team +
                " 02=" + player.getOxygen() +
                " Downed=" + player.isDowned()
            );
        });
    }

    private void tickGameAsync() {
        MatchSnapshot snapshot = snapshotProvider.get();
        if (snapshot != null) handleSnapshotAsync(snapshot);
    }

    private void handleGameTick() {
        ZonePresence presence =
            zonePresenceService.compute(session);
        this.lastPresence = presence;

        zoneOxygenService.tick(session, presence);

        for (CaptureService.CaptureEvent event : captureService.tick(session, presence)) {
            switch (event.type()) {
                case CAPTURED -> {
                    Team team = teamService.getTeam(event.teamId());
                    if (team != null) {
                        displayService.onZoneCaptured(
                            event.teamId(),
                            team.getName(),
                            event.zone().getDisplayName(),
                            event.oxygenRestored(),
                            new HashSet<>(team.getMembers())
                        );
                    }
                }
                case CONTESTED -> displayService.onZoneContested(event.zone().getId());
                case CAPTURING -> displayService.onZoneCapturing(event.zone().getId(), event.teamId());
            }
        }

        playerOxygenService.tickDrain(
            session,
            (playerId, damage) -> {
                playerService.applySuffocationDamage(playerId, damage);
                displayService.onPlayerSuffocating(playerId);
            },
            displayService::onPlayerLowOxygen
        );

        /* Player State */

        downedService.tick(session, playerId ->
            eliminatePlayer(playerId, "Bled out"));

        reviveService.tick(
            session,
            playerPositionProvider,
            displayService::onReviveProgress,
            this::completeRevive
        );
    }

    private void handleSnapshotAsync(MatchSnapshot snapshot) {
        externalLifecycles.forEach(l -> {
            try {
                l.readGameTickAsync(snapshot);
            } catch (Exception e) {
                log.error("Error in lifecycle async tick reader for '"
                    + l.getClass().getSimpleName() + "' Reason: " , e);
            }
        });
    }

    /**
     * Builds a map of zoneId -> set of team IDs physically present this tick,
     * derived from the last computed ZonePresence
     */
    private Map<String, Set<String>> buildZonePresentTeams() {
        if (lastPresence == null || session == null) return Map.of();

        Map<String, Set<String>> map = new HashMap<>();
        for (CaptureZoneState zone : session.getZones()) {
            Map<String, Integer> teamCounts = lastPresence.getTeamCounts(zone);
            if (!teamCounts.isEmpty()) {
                map.put(zone.getId(), Set.copyOf(teamCounts.keySet()));
            }
        }
        return map;
    }

    private Map<UUID, Integer> buildReviveProgressMap() {
        Map<UUID, Integer> map = new HashMap<>();
        int maxTicks = session.config().downed().reviveTicks();
        if (maxTicks <= 0) return map;

        for (PlayerMatchState player : session.getPlayers()) {
            ReviveSession revive = reviveService.getSession(player.getPlayerId());
            if (revive != null) {
                int percent = (int) Math.min(100, (revive.getProgress() * 100.0) / maxTicks);
                map.put(player.getPlayerId(), percent);
            }
        }
        return map;
    }

    /**
     * Returns UUIDs of all alive players on the same team as the given player,
     * excluding the player themselves
     */
    private Set<UUID> getTeammates(UUID playerId) {
        if (session == null) return Set.of();

        String team = session.getPlayerTeam(playerId);
        if (team == null) return Set.of();

        Set<UUID> teammates = new HashSet<>();
        for (PlayerMatchState p : session.getPlayers()) {
            UUID pid = p.getPlayerId();
            if (!pid.equals(playerId) && p.isAlive() && team.equals(session.getPlayerTeam(pid))) {
                teammates.add(pid);
            }
        }
        return teammates;
    }

    private void checkWinCondition() {
       resolveWinner().ifPresent(this::endMatch);
    }

    private Optional<String> resolveWinner() {
        if (session == null || !session.isPlaying()) return Optional.empty();

        Set<String> aliveTeams = session.getTeamsWithAlivePlayers();

        if (aliveTeams.size() == 1) {
            String winnerId = aliveTeams.iterator().next();
            Team winner = teamService.getTeam(winnerId);
            return Optional.of(winner != null ? winner.getName() : winnerId);
        }

        if (aliveTeams.isEmpty()) {
            return Optional.of(""); // mass disconnect / draw
        }

        return Optional.empty();
    }

    private String resolveWinnerByScore() {
        if (session == null) return "";
        return session.getTeamScores().entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .filter(e -> e.getValue() > 0)
            .map(e -> {
                Team t = teamService.getTeam(e.getKey());
                return t != null ? t.getName() : e.getKey();
            })
            .orElse("");
    }

}
