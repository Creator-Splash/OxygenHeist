package com.creatorsplash.oxygenheist.application.match.zone;

import com.creatorsplash.oxygenheist.domain.zone.CaptureZoneState;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Represents the computed presence of teams within capture zones for a single tick
 * <p>Read-only</p>
 */
public class ZonePresence {

    private final Map<CaptureZoneState, Map<String, Integer>> zoneTeamCounts;

    public ZonePresence(Map<CaptureZoneState, Map<String, Integer>> zoneTeamCounts) {
        this.zoneTeamCounts = zoneTeamCounts;
    }

    /**
     * Gets team counts for a specific zone
     *
     * @param zone the zone
     * @return team -> player count map
     */
    public Map<String, Integer> getTeamCounts(CaptureZoneState zone) {
        return zoneTeamCounts.getOrDefault(zone, Collections.emptyMap());
    }

    /**
     * @return true if the zone has no players
     */
    public boolean isEmpty(CaptureZoneState zone) {
        return !zoneTeamCounts.containsKey(zone) || zoneTeamCounts.get(zone).isEmpty();
    }

    /**
     * @return true if multiple teams are present in the zone
     */
    public boolean isContested(CaptureZoneState zone) {
        Map<String, Integer> counts = zoneTeamCounts.get(zone);
        return counts != null && counts.size() > 1;
    }

    /**
     * Gets the single team present in the zone
     *
     * @param zone the zone
     * @return optional team id
     */
    public Optional<String> getSingleTeam(CaptureZoneState zone) {
        Map<String, Integer> counts = zoneTeamCounts.get(zone);
        if (counts == null || counts.size() != 1) {
            return Optional.empty();
        }

        return Optional.of(counts.keySet().iterator().next());
    }

    /**
     * Checks whether a specific team is present in the zone
     */
    public boolean isTeamPresent(CaptureZoneState zone, String teamId) {
        Map<String, Integer> counts = zoneTeamCounts.get(zone);
        return counts != null && counts.containsKey(teamId);
    }

    public boolean isTeamInOwnedZone(String teamId) {
        for (CaptureZoneState zone : zoneTeamCounts.keySet()) {
            if (isTeamPresent(zone, teamId) &&
                teamId.equals(zone.getOwnerTeamId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets all zones with presence data
     */
    public Set<CaptureZoneState> getZones() {
        return zoneTeamCounts.keySet();
    }

}
