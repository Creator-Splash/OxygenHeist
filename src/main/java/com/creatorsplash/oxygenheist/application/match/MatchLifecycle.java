package com.creatorsplash.oxygenheist.application.match;

import com.creatorsplash.oxygenheist.domain.match.MatchSession;
import com.creatorsplash.oxygenheist.domain.match.MatchSnapshot;

import java.util.UUID;

/**
 * Implemented by any service that holds match-scoped or player-scoped state
 *
 * <p>Services register themselves with {@link MatchService} at startup</p>
 */
public interface MatchLifecycle {

    /**
     * Called when a new match begins
     *
     * <p>Use this to initialize any match-scoped state that should be
     * fresh at the start of every match</p>
     */
    default void onMatchStart() {}

    /**
     * Called when the current match ends
     *
     * <p>Implementations must wipe all match-scoped state here.
     * This will always be called before the next {@link #onMatchStart()}</p>
     */
    default void onMatchEnd() {}

    /**
     * Called when a player joins an active match
     *
     * @param playerId the UUID of the player who joined
     */
    default void onPlayerJoin(UUID playerId) {}

    /**
     * Called when a player leaves or is removed from an active match
     *
     * @param playerId the UUID of the player who left
     */
    default void onPlayerLeave(UUID playerId) {}

    /**
     * Called every match tick
     *
     * @param session the match session relevant to this match lifecycle
     */
    default void onGameTick(MatchSession session) {}

    /**
     * Called every match tick
     *
     * @param session the match snapshot relevant to this match lifecycle
     */
    default void readGameTick(MatchSnapshot session) {}

}
