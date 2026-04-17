package com.creatorsplash.oxygenheist.application.match.combat;

import com.creatorsplash.oxygenheist.application.match.MatchService;
import com.creatorsplash.oxygenheist.application.match.combat.revive.ReviveService;
import com.creatorsplash.oxygenheist.domain.player.PlayerMatchState;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/**
 * Handles combat-related gameplay logic within a match
 *
 * <p>This service is responsible for processing player damage interactions,
 * tracking attackers, and resolving player deaths into game-specific outcomes
 * such as eliminations and score updates</p>
 *
 * <p>All validation and rule enforcement (e.g. match state, player eligibility,
 * team rules) should be handled within this service rather than in listeners</p>
 */
@RequiredArgsConstructor
public class CombatService {

    private final MatchService matchService;
    private final ReviveService reviveService;

    public boolean isCombatRelevant(UUID victimId, UUID attackerId) {
        return matchService.isPlayerInActiveMatch(victimId)
            && matchService.isPlayerInActiveMatch(attackerId);
    }

    /**
     * Handles a damage event between two players
     *
     * <p>This method records the attacker as the last known source of damage
     * for the victim. It does not apply any damage logic itself</p>
     *
     * @param victimId the UUID of the damaged player
     * @param attackerId the UUID of the attacking player
     */
    public void handleDamage(UUID victimId, UUID attackerId) {
        matchService.getSession().ifPresent(session -> {
            if (!session.isPlaying()) return;

            PlayerMatchState victim = session.getOrCreatePlayer(victimId);

            if (!victim.isAlive()) return;

            matchService.getLog().debug("combat.damage", "Handling damage for "
                    + victimId + " killed by " + attackerId);

            // todo future checks

            // todo if config has revive cancelling damage - put here for both

            victim.setLastAttacker(attackerId);
        });
    }

    public void handleLethalDamage(UUID victimId, UUID attackerId) {
        matchService.getSession().ifPresent(session -> {
            if (!session.isPlaying()) return;

            PlayerMatchState state = session.getOrCreatePlayer(victimId);

            if (!state.isAlive() || state.isDowned()) return;

            matchService.getLog().debug("combat.kill", "Handling lethal damage for "
                + victimId + " killed by " + attackerId);

            state.setLastAttacker(attackerId);

            reviveService.cancelRevive(victimId);

            matchService.downPlayer(victimId);
        });
    }

}
