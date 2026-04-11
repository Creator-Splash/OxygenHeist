package com.creatorsplash.oxygenheist.domain.zone;

import java.util.Map;

/**
 * Read-only data of match zone state
 */
public record ZoneSnapshot(
   String id,
   String ownerTeamId,
   String capturingTeamId,
   double captureProgress,
   Map<String, Double> teamOxygen
) {}
