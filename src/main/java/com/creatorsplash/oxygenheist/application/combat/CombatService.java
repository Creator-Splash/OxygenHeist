package com.creatorsplash.oxygenheist.application.combat;

import com.creatorsplash.oxygenheist.application.match.MatchService;
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
    private final DownedService downedService;

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

            // todo future checks

            victim.setLastAttacker(attackerId);
        });
    }

    /**
     * Handles a player death event
     *
     * <p>This method converts a Bukkit death into a game-specific elimination,
     * awarding points to the last known attacker if applicable</p>
     *
     * @param victimId the UUID of the player who died
     */
    public void handleDeath(UUID victimId) {
        matchService.getSession().ifPresent(session -> {
            if (!session.isPlaying()) return;

            PlayerMatchState victim = session.getOrCreatePlayer(victimId);

            if (!victim.isAlive()) return;

            downedService.downPlayer(session, victimId);
        });
    }

}
