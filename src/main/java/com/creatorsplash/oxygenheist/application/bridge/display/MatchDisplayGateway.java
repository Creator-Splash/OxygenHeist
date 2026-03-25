package com.creatorsplash.oxygenheist.application.bridge.display;

import com.creatorsplash.oxygenheist.domain.match.MatchSnapshot;

import java.util.UUID;

/**
 * Platform-facing gateway for rendering match UI
 */
public interface MatchDisplayGateway {

    /**
     * Render the current match snapshot
     */
    void renderSnapshot(MatchSnapshot snapshot);

    /**
     * Remove all UI for a specific player
     */
    void removePlayer(UUID playerId);

    /**
     * Clear all UI
     */
    void clearAll();

    /**
     * Show a title to a player
     */
    void showTitle(UUID playerId, String title, String subtitle);

    /**
     * Play a sound for a player
     */
    void playSound(UUID playerId, String soundKey);

}
