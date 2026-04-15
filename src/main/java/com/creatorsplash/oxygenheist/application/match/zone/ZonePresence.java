package com.creatorsplash.oxygenheist.application.match.zone;

import com.creatorsplash.oxygenheist.domain.zone.CaptureZoneState;
import lombok.RequiredArgsConstructor;

import java.util.*;

/**
 * Represents the computed presence of teams within capture zones for a single tick
 * <p>Read-only</p>
 */
@RequiredArgsConstructor
public class ZonePresence {

    private final Map<CaptureZoneState, Map<String, Set<UUID>>> zoneTeamPlayers;

    /**
     * Gets team counts for a specific zone
     *
     * @param zone the zone
     * @return team -> player count map
     */
    public Map<String, Integer> getTeamCounts(CaptureZoneState zone) {
        Map<String, Set<UUID>> teamPlayers = zoneTeamPlayers.getOrDefault(zone, Map.of());
        Map<String, Integer> counts = new HashMap<>();
        teamPlayers.forEach((teamId, players) -> counts.put(teamId, players.size()));
        return counts;
    }

    /**
     * Gets present players of a team for a specific zone
     *
     * @param zone the zone
     * @param teamId the team id
     */
    public Set<UUID> getPlayersForTeam(CaptureZoneState zone, String teamId) {
        Map<String, Set<UUID>> teamPlayers = zoneTeamPlayers.get(zone);
        if (teamPlayers == null) return Set.of();
        return teamPlayers.getOrDefault(teamId, Set.of());
    }

    /**
     * @return true if the zone has no players
     */
    public boolean isEmpty(CaptureZoneState zone) {
        Map<String, Set<UUID>> teamPlayers = zoneTeamPlayers.get(zone);
        return teamPlayers == null || teamPlayers.isEmpty();
    }

    /**
     * @return true if multiple teams are present in the zone
     */
    public boolean isContested(CaptureZoneState zone) {
        Map<String, Set<UUID>> teamPlayers = zoneTeamPlayers.get(zone);
        return teamPlayers != null && teamPlayers.size() > 1;
    }

    /**
     * Gets the single team present in the zone
     *
     * @param zone the zone
     * @return optional team id
     */
    public Optional<String> getSingleTeam(CaptureZoneState zone) {
        Map<String, Set<UUID>> teamPlayers = zoneTeamPlayers.get(zone);
        if (teamPlayers == null || teamPlayers.size() != 1) return Optional.empty();
        return Optional.of(teamPlayers.keySet().iterator().next());
    }

    /**
     * Gets all team IDs present in a zone this tick
     *
     * @param zone the zone
     * @return set of team IDs with at least one player present
     */
    public Set<String> getTeamsInZone(CaptureZoneState zone) {
        Map<String, Set<UUID>> teamPlayers = zoneTeamPlayers.get(zone);
        if (teamPlayers == null) return Set.of();
        return Set.copyOf(teamPlayers.keySet());
    }

    /**
     * Checks whether a specific team is present in the zone
     */
    public boolean isTeamPresent(CaptureZoneState zone, String teamId) {
        Map<String, Set<UUID>> teamPlayers = zoneTeamPlayers.get(zone);
        return teamPlayers != null && teamPlayers.containsKey(teamId);
    }

    public boolean isTeamInOwnedZone(String teamId) {
        return false; // TODO if we still need
    }

    /**
     * Gets all zones with presence data
     */
    public Set<CaptureZoneState> getZones() {
        return zoneTeamPlayers.keySet();
    }

}
