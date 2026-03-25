package com.creatorsplash.oxygenheist.domain.match;

import com.creatorsplash.oxygenheist.domain.match.config.MatchConfig;
import com.creatorsplash.oxygenheist.domain.player.PlayerSnapshot;
import com.creatorsplash.oxygenheist.domain.zone.CaptureZoneState;
import com.creatorsplash.oxygenheist.domain.player.PlayerMatchState;
import com.creatorsplash.oxygenheist.domain.zone.ZoneSnapshot;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Represents a single active match instance
 *
 * <p>This class holds all runtime state for a match, including players and
 * the current match lifecycle state</p>
 */
@Accessors(fluent = true)
public final class MatchSession {

    @Getter
    private final MatchConfig config;

    @Getter @Setter
    private MatchState state;

    private int remainingTicks;
    private boolean instantDeath;
    private boolean borderShrinkStarted;

    private final Map<String, CaptureZoneState> zones = new HashMap<>();

    private final Map<UUID, PlayerMatchState> players = new HashMap<>();

    private final Map<UUID, String> playerTeams = new HashMap<>();

    public MatchSession(@NotNull final MatchConfig config) {
        this.config = config;
        this.state = MatchState.WAITING;
        this.instantDeath = false;
        this.borderShrinkStarted = false;
    }

    /* == Gameplay == */

    public boolean isInstantDeath() {
        return instantDeath;
    }

    public boolean shouldEnterInstantDeath() {
        return state == MatchState.PLAYING
            && !isInstantDeath()
            && getRemainingSeconds() <= config.instantDeathSecondsRemaining();
    }

    public void enterInstantDeath() {
        this.instantDeath = true;
    }

    public boolean borderShrinkStarted() {
        return borderShrinkStarted;
    }

    public boolean shouldStartBorderShrink() {
        return state == MatchState.PLAYING
            && !borderShrinkStarted
            && getRemainingSeconds()
                <= (config.durationSeconds() - config.border().shrinkDelaySeconds());
    }

    public void markBorderShrinkStarted() {
        this.borderShrinkStarted = true;
    }

    /** Shorthand to check if the session state is {@link MatchState#PLAYING} */
    public boolean isPlaying() {
        return state() == MatchState.PLAYING;
    }

    /** Shorthand to check if the session state is {@link MatchState#STARTING} */
    public boolean isCountdown() { return state() == MatchState.STARTING; }

    /* == Zones == */

    public Collection<CaptureZoneState> getZones() {
        return zones.values();
    }

    public void addZone(CaptureZoneState zone) {
        zones.put(zone.getId(), zone);
    }

    public Optional<CaptureZoneState> getZone(String id) {
        return Optional.ofNullable(zones.get(id));
    }

    /* == Player == */

    /**
     * @return all player states in this match
     */
    public Collection<PlayerMatchState> getPlayers() {
        return players.values();
    }

    /**
     * Gets or creates a player state
     *
     * @param playerId the players UUID
     * @return the existing or newly created player state
     */
    public PlayerMatchState getOrCreatePlayer(UUID playerId) {
        return players.computeIfAbsent(playerId, PlayerMatchState::new);
    }

    /**
     * Gets a player state if present
     *
     * @param playerId the players UUID
     * @return optional containing the player state if it exists
     */
    public Optional<PlayerMatchState> getPlayer(UUID playerId) {
        return Optional.ofNullable(players.get(playerId));
    }

    /**
     * Removes a player from the match
     *
     * @param playerId the players UUID
     */
    public void removePlayer(UUID playerId) {
        players.remove(playerId);
    }

    /* == Teams temp == */

    public String getPlayerTeam(UUID playerId) {
        return playerTeams.get(playerId);
    }

    public void assignPlayerTeam(UUID playerId, String teamId) {
        playerTeams.put(playerId, teamId);
    }

    public Map<String, Integer> countPlayersPerTeam(Collection<PlayerMatchState> players) {
        Map<String, Integer> counts = new HashMap<>();

        for (PlayerMatchState player : players) {
            String team = playerTeams.get(player.getPlayerId());
            if (team != null) {
                counts.merge(team, 1, Integer::sum);
            }
        }

        return counts;
    }

    /* == Timing == */

    /* Countdown */

    public void startCooldown() {
        this.state = MatchState.STARTING;
        this.remainingTicks = config.countdownSeconds() * 20;
    }

    /* Game */

    public void startMatch() {
        this.state = MatchState.STARTING;
        this.remainingTicks = config.durationSeconds() * 20;
    }

    public int getRemainingTicks() {
        return this.remainingTicks;
    }

    public int getRemainingSeconds() {
        return Math.max(0, remainingTicks / 20);
    }

    public boolean isTimeExpired() {
        return remainingTicks <= 0;
    }

    public void resetTimer() {
        this.remainingTicks = config.durationSeconds() * 20;
    }

    public void tickTimer() {
        if (remainingTicks > 0) {
            remainingTicks--;
        }
    }

    /* == Snapshot == */

    public MatchSnapshot createSnapshot(long tick) {
        Map<UUID, PlayerSnapshot> playerSnapshots = new HashMap<>();
        for (PlayerMatchState player : getPlayers()) {
            playerSnapshots.put(player.getPlayerId(), player.toSnapshot());
        }

        Map<String, ZoneSnapshot> zonesSnapshots = new HashMap<>();
        for (CaptureZoneState zone : getZones()) {
            zonesSnapshots.put(zone.getId(), zone.toSnapshot());
        }

        return new MatchSnapshot(
            tick,
            state(),
            config(),
            getRemainingTicks(),
            isInstantDeath(),
            borderShrinkStarted(),
            playerSnapshots,
            zonesSnapshots
        );
    }

}
