package com.creatorsplash.oxygenheist.platform.paper.listener;

import com.creatorsplash.oxygenheist.application.match.combat.CombatService;
import com.creatorsplash.oxygenheist.application.match.combat.PlayerActionService;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.UUID;

/**
 * Bukkit event listener for combat-related events
 */
@RequiredArgsConstructor
public final class CombatListener implements Listener {

    private final CombatService combatService;
    private final PlayerActionService actionService;

    /**
     * Handles player-to-player damage events
     *
     * @param event the damage event
     */
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        UUID victimId = victim.getUniqueId();
        UUID attackerId = attacker.getUniqueId();

        if (!combatService.isCombatRelevant(victimId, attackerId)) return;

        if (!actionService.canAttack(attackerId)) {
            event.setCancelled(true);
            return;
        }

        if (!actionService.canBeDamaged(victimId)) {
            event.setCancelled(true);
            return;
        }

        double finalHealth = victim.getHealth() - event.getFinalDamage();

        if (finalHealth > 0) {
            combatService.handleDamage(
                victim.getUniqueId(),
                attacker.getUniqueId()
            );
        } else {
            event.setDamage(0); // todo move out? or keep in
            combatService.handleLethalDamage(
                victim.getUniqueId(),
                attacker.getUniqueId()
            );
        }
    }

}
