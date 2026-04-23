package com.creatorsplash.oxygenheist.platform.paper.listener;

import com.creatorsplash.oxygenheist.application.match.MatchService;
import com.creatorsplash.oxygenheist.application.match.combat.CombatService;
import com.creatorsplash.oxygenheist.application.match.combat.PlayerActionService;
import com.creatorsplash.oxygenheist.platform.paper.config.weapon.WeaponConfigService;
import com.creatorsplash.oxygenheist.platform.paper.config.weapon.WeaponTypeConfig;
import com.creatorsplash.oxygenheist.platform.paper.weapon.WeaponUtils;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Bukkit event listener for combat-related events
 */
@RequiredArgsConstructor
public final class CombatListener implements Listener {

    private final MatchService matchService;
    private final CombatService combatService;
    private final PlayerActionService actionService;
    private final WeaponConfigService weaponConfigService;

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

        if (!actionService.canAttackTarget(attackerId, victimId)) {
            event.setCancelled(true);
            return;
        }

        // Weapon resolve
        String weaponName = null;
        ItemStack held = attacker.getInventory().getItemInMainHand();
        String weaponId = WeaponUtils.getWeaponId(held);
        if (weaponId != null) {
            WeaponTypeConfig weaponConfig = weaponConfigService.getConfig(weaponId);
            if (weaponConfig != null) weaponName = weaponConfig.displayName();
        }

        double finalHealth = victim.getHealth() - event.getFinalDamage();

        if (finalHealth > 0) {
            combatService.handleDamage(victimId, attackerId, weaponName);
        } else {
            event.setDamage(0);
            combatService.handleLethalDamage(victimId, attackerId, weaponName);
        }
    }

    /**
     * Catches ALL fatal damage to active match players from any source.
     * Prevents vanilla death and redirects to the downed system
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFatalDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        UUID playerId = player.getUniqueId();

        // Only intercept for active (alive, not downed) match players
        if (!matchService.isPlayerInActiveMatch(playerId)) return;

        if (player.getHealth() - event.getFinalDamage() > 0) return;

        // Cancel the kill - redirect to downed system
        event.setDamage(0);
        event.setCancelled(true);
        matchService.downPlayer(playerId);
    }

}
