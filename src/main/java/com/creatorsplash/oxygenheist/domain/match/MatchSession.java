package com.creatorsplash.oxygenheist.domain.match;

import com.creatorsplash.oxygenheist.application.match.zone.ZonePresence;
import com.creatorsplash.oxygenheist.domain.match.config.MatchConfig;
import com.creatorsplash.oxygenheist.domain.player.PlayerSnapshot;
import com.creatorsplash.oxygenheist.domain.team.TeamSnapshot;
import com.creatorsplash.oxygenheist.domain.zone.CaptureZoneState;
import com.creatorsplash.oxygenheist.domain.player.PlayerMatchState;
import com.creatorsplash.oxygenheist.domain.zone.ZoneSnapshot;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    private final Map<String, Integer> teamScores = new HashMap<>();

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
            && getRemainingSeconds() <= config.border().shrinkDelaySeconds();
    }

    public void markBorderShrinkStarted() {
        this.borderShrinkStarted = true;
    }

    /** Shorthand to check if the session state is {@link MatchState#PLAYING} */
    public boolean isPlaying() {
        return state() == MatchState.PLAYING;
    }

    /** Shorthand to check if the session state is {@link MatchState#SETUP} */
    public boolean isCountdown() { return state() == MatchState.SETUP; }

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

    /* == Teams == */

    /**
     * Returns true if both players are assigned to the same team
     *
     * <p>Returns false if either player has no team assignment,
     * or if they are on different teams</p>
     */
    public boolean isSameTeam(UUID a, UUID b) {
        String teamA = playerTeams.get(a);
        if (teamA == null) return false;
        return teamA.equals(playerTeams.get(b));
    }

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

    public int getTeamScore(String teamId) {
        return teamScores.getOrDefault(teamId, 0);
    }

    public void addTeamScore(String teamId, int amount) {
        teamScores.merge(teamId, amount, Integer::sum);
    }

    public Map<String, Integer> getTeamScores() {
        return Collections.unmodifiableMap(teamScores);
    }

    /**
     * Returns the set of team IDs that still have at least one living (non-eliminated) player.
     * <p>Downed players count - their team is not out until every member is fully eliminated</p>
     */
    public Set<String> getTeamsWithAlivePlayers() {
        Set<String> alive = new HashSet<>();
        for (PlayerMatchState player : players.values()) {
            if (!player.isAlive()) continue;
            String teamId = playerTeams.get(player.getPlayerId());
            if (teamId != null) alive.add(teamId);
        }
        return alive;
    }

    public void addTeamScoreAndPlayerScore(
        String teamId,
        int amount,
        ZonePresence presence,
        CaptureZoneState zone
    ) {
        addTeamScore(teamId, amount);
        Map<String, Integer> counts = presence.getTeamCounts(zone);
        int playerCount = counts.getOrDefault(teamId, 1);
        int perPlayer = Math.max(1, amount / playerCount);

        for (PlayerMatchState player : getPlayers()) {
            if (teamId.equals(getPlayerTeam(player.getPlayerId()))) {
                if (presence.getTeamCounts(zone).containsKey(teamId)) {
                    player.addScore(perPlayer);
                }
            }
        }
    }

    /**
     * Resolves the best spectate target for a player being eliminated -
     * the first alive, non-downed teammate
     *
     * @param eliminatedId the player being eliminated
     * @return a teammate UUID to spectate, or null if none are available
     */
    public @Nullable UUID resolveSpectateTarget(UUID eliminatedId) {
        String teamId = getPlayerTeam(eliminatedId);
        if (teamId == null) return null;

        // Find a living, non-downed teammate
        return getPlayers().stream()
            .filter(p -> !p.getPlayerId().equals(eliminatedId))
            .filter(p -> p.isAlive() && !p.isDowned())
            .map(PlayerMatchState::getPlayerId)
            .filter(playerId -> teamId.equals(getPlayerTeam(playerId)))
            .findFirst()
            .orElse(null);
    }

    /* == Timing == */

    /* Countdown */

    public void startCooldown() {
        this.state = MatchState.SETUP;
        this.remainingTicks = config.countdownSeconds() * 20;
    }

    /* Game */

    public void startMatch() {
        this.state = MatchState.PLAYING;
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

    public MatchSnapshot createSnapshot(
        long tick,
        Map<UUID, Integer> reviveProgressMap,
        Map<String, TeamSnapshot> teams,
        Map<String, Set<String>> zonePresentTeams
    ) {
        Map<UUID, PlayerSnapshot> playerSnapshots = new HashMap<>();
        for (PlayerMatchState player : getPlayers()) {
            int reviveProgress = reviveProgressMap.getOrDefault(player.getPlayerId(), 0);
            playerSnapshots.put(player.getPlayerId(), player.toSnapshot(reviveProgress));
        }

        Map<String, ZoneSnapshot> zonesSnapshots = new HashMap<>();
        for (CaptureZoneState zone : getZones()) {
            Set<String> presentTeams = zonePresentTeams.getOrDefault(zone.getId(), Set.of());
            zonesSnapshots.put(zone.getId(), zone.toSnapshot(presentTeams));
        }

        return new MatchSnapshot(
            tick,
            state(),
            config(),
            getRemainingTicks(),
            isInstantDeath(),
            borderShrinkStarted(),
            playerSnapshots,
            zonesSnapshots,
            teams
        );
    }

}
