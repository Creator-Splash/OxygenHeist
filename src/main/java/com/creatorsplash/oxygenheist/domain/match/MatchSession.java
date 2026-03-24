package com.creatorsplash.oxygenheist.domain.match;

import com.creatorsplash.oxygenheist.application.match.zone.CaptureZoneState;
import com.creatorsplash.oxygenheist.domain.player.PlayerMatchState;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

/**
 * Represents a single active match instance
 *
 * <p>This class holds all runtime state for a match, including players and
 * the current match lifecycle state</p>
 */
public final class MatchSession {

    @Getter @Setter
    private MatchState state = MatchState.WAITING;

    private final Map<String, CaptureZoneState> zones = new HashMap<>();

    private final Map<UUID, PlayerMatchState> players = new HashMap<>();

    private final Map<UUID, String> playerTeams = new HashMap<>();

    /** Shorthand to check if the session state is {@link MatchState#PLAYING} */
    public boolean isPlaying() {
        return getState() == MatchState.PLAYING;
    }

    /* Zones */

    public Collection<CaptureZoneState> getZones() {
        return zones.values();
    }

    public void addZone(CaptureZoneState zone) {
        zones.put(zone.getId(), zone);
    }

    public Optional<CaptureZoneState> getZone(String id) {
        return Optional.ofNullable(zones.get(id));
    }

    /* Player */

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

    /* Teams */

    public String getPlayerTeam(UUID playerId) {
        return playerTeams.get(playerId);
    }

    public void setPlayerTeam(UUID playerId, String teamId) {
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

}
