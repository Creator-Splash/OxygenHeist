package com.creatorsplash.oxygenheist.application.bridge;

import com.creatorsplash.oxygenheist.domain.match.MatchSession;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Handles player-facing state changes at match lifecycle boundaries
 *
 * <p>Parallel to {@link GameWorldService} - responsible for teleporting players,
 * setting gamemodes, applying armor, and resetting state at match start and end</p>
 */
public interface GamePlayerService {

    /**
     * Prepares all session players for the match to begin
     *
     * <p>Called at the SETUP -> PLAYING transition, after the countdown expires.
     * Implementations should: teleport to team bases, set Survival mode, clear
     * inventory, restore health and food, and apply team armour.</p>
     *
     * @param session the active match session
     */
    void prepareForMatch(MatchSession session);

    /**
     * Resets all session players after a match ends
     *
     * <p>Called in {@code endMatch()} before the session is nulled.
     * Implementations should: reset gamemode, remove potion effects,
     * clear inventory, restore health and food, and remove team armour.</p>
     *
     * @param session the match session that just ended
     */
    void cleanupAfterMatch(MatchSession session);

    /* Gameplay */

    /**
     * Applies physical downed effects to a player - slowness, sneaking
     */
    void onPlayerDowned(UUID playerId);

    /**
     * Removes downed effects and partially restores health on revive
     */
    void onPlayerRevived(UUID playerId);

    /**
     * Removes downed effects and sets spectator mode on elimination
     */
    void onPlayerEliminated(UUID playerId, @Nullable UUID spectateTargetId);

}
