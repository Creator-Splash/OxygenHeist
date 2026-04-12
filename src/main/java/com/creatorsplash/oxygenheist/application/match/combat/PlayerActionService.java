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
     * Determines whether an attacker is permitted to damage a specific victim
     *
     * <p>Combines all eligibility checks into a single call:</p>
     * <ul>
     *   <li>Attacker must be alive and not downed</li>
     *   <li>Victim must be alive</li>
     *   <li>If global friendly fire is disabled, players on the same team cannot attack each other</li>
     * </ul>
     *
     * <p>Returns {@code true} outside of an active match session - no restrictions apply</p>
     *
     * @param attackerId the player attempting to deal damage
     * @param victimId the player receiving damage
     */
    public boolean canAttackTarget(@NotNull UUID attackerId, @NotNull UUID victimId) {
        if (!canAttack(attackerId)) return false;
        if (!canBeDamaged(victimId)) return false;

        return matchService.getSession()
            .map(session -> {
                if (session.config().globalFriendlyFire()) return true;
                return !session.isSameTeam(attackerId, victimId);
            })
            .orElse(true);
    }

    /**
     * Determines whether a player can receive damage
     *
     * @param playerId the player being damaged
     * @return true if the player is alive, or not part of the match
     */
    public boolean canBeDamaged(@NotNull UUID playerId) {
        return canAttack(playerId);
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
