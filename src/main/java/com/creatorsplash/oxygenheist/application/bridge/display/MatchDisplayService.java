package com.creatorsplash.oxygenheist.application.bridge.display;

import com.creatorsplash.oxygenheist.domain.match.MatchSnapshot;

import java.util.UUID;

/**
 * Application-level display orchestrator
 *
 * <p>Consumes match snapshots and determines what should be rendered</p>
 * <p>Delegates rendering to a platform-specific gateway</p>
 */
public interface MatchDisplayService {

    /**
     * Called every tick after a new snapshot is produced
     */
    void render(MatchSnapshot snapshot);

    /**
     * Called when a player leaves or is removed from the match
     */
    void removePlayer(UUID playerId);

    /**
     * Called when match ends
     */
    void onMatchEnd();

    /**
     * Reset all display state
     */
    void reset();

}
