package com.creatorsplash.oxygenheist.application.match.zone;

import java.util.UUID;

/**
 * Provides zone related queries for match systems
 */
public interface ZoneService {

    /**
     * Checks whether a player is currently inside a zone owned by their team
     *
     * @param playerId the player UUID
     * @return true if the player is inside a friendly owned zone
     */
    boolean isPlayerInOwnedZone(UUID playerId);

}
