package com.creatorsplash.oxygenheist.domain.zone;

import java.util.Map;
import java.util.Set;

/**
 * Read-only data of match zone state
 */
public record ZoneSnapshot(
   String id,
   String ownerTeamId,
   String capturingTeamId,
   boolean contested,
   double captureProgress,
   Map<String, Double> teamOxygen,
   Set<String> presentTeamIds,
   Set<String> evacuatingTeamIds,
   Set<String> refillingTeamIds,
   Map<String, Integer> teamCooldownTicks
) {}
