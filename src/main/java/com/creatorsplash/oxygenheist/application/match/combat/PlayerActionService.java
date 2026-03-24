package com.creatorsplash.oxygenheist.application.match.combat;

import com.creatorsplash.oxygenheist.application.match.MatchService;
import com.creatorsplash.oxygenheist.domain.player.PlayerMatchState;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Provides centralized rules for determining what actions a player is allowed to perform
 * within the context of an active match
 */
@RequiredArgsConstructor
public class PlayerActionService {

    private final MatchService matchService;

    /**
     * Determines whether a player is allowed to perform attack actions
     *
     * @param playerId the player attempting to attack
     * @return true if the player is alive and not downed, or not part of a match
     */
    public boolean canAttack(@NotNull UUID playerId) {
        PlayerMatchState state = getState(playerId);
        if (state == null) return true;

        return state.isAlive() && !state.isDowned();
    }

    /**
     * Determines whether a player can receive damage
     *
     * @param playerId the player being damaged
     * @return true if the player is alive, or not part of the match
     */
    public boolean canBeDamaged(@NotNull UUID playerId) {
        PlayerMatchState state = getState(playerId);
        if (state == null) return true;

        return state.isAlive();
    }

    /**
     * Determines whether a player can interact with the world (blocks, entities, items)
     *
     * @param playerId the player attempting to interact
     * @return true if the player is alive and not downed, or not part of a match
     */
    public boolean canInteract(@NotNull UUID playerId) {
        PlayerMatchState state = getState(playerId);
        if (state == null) return true;

        return state.isAlive() && !state.isDowned();
    }

    /* Internals */

    /**
     * Retrieves the players match state if they are part of an active session
     *
     * @param playerId the player UUID
     * @return the players match state, or null if not in a match
     */
    private PlayerMatchState getState(@NotNull UUID playerId) {
        return matchService.getSession()
            .flatMap(session -> session.getPlayer(playerId))
            .orElse(null);
    }

}
