package com.creatorsplash.oxygenheist.platform.paper.listener;

import com.creatorsplash.oxygenheist.application.match.combat.CombatService;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Bukkit event listener for combat-related events
 */
@RequiredArgsConstructor
public final class CombatListener implements Listener {

    private final CombatService combatService;

    /**
     * Handles player-to-player damage events
     *
     * @param event the damage event
     */
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        double finalHealth = victim.getHealth() - event.getFinalDamage();

        if (finalHealth > 0) {
            combatService.handleDamage(
                victim.getUniqueId(),
                attacker.getUniqueId()
            );
        }

        event.setDamage(0);

        // todo lethal?
        combatService.handleDamage(
            victim.getUniqueId(),
            attacker.getUniqueId()
        );
    }

}
