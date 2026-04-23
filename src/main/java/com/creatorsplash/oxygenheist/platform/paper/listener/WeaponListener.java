package com.creatorsplash.oxygenheist.platform.paper.listener;

import com.creatorsplash.oxygenheist.application.common.LogCenter;
import com.creatorsplash.oxygenheist.application.match.MatchService;
import com.creatorsplash.oxygenheist.application.match.combat.PlayerActionService;
import com.creatorsplash.oxygenheist.domain.match.MatchSession;
import com.creatorsplash.oxygenheist.platform.paper.config.GlobalConfigService;
import com.creatorsplash.oxygenheist.platform.paper.display.WeaponAmmoDisplayService;
import com.creatorsplash.oxygenheist.platform.paper.weapon.*;
import com.creatorsplash.oxygenheist.platform.paper.weapon.handler.WeaponHandler;
import com.creatorsplash.oxygenheist.platform.paper.weapon.service.WeaponDropService;
import com.creatorsplash.oxygenheist.platform.paper.weapon.service.WeaponProjectileContext;
import com.creatorsplash.oxygenheist.platform.paper.weapon.service.WeaponProjectileTracker;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Central Bukkit listener that routes player events into the {@link WeaponRegistry}.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Dispatch right-click, left-click, sneak, melee, projectile, and reload
 *       interruption events to the correct {@link WeaponHandler}</li>
 *   <li>Build {@link WeaponContext} and {@link WeaponProjectileContext} for each dispatch</li>
 *   <li>Guard melee dispatch against action and team eligibility rules</li>
 *   <li>Cancel vanilla melee damage for weapon holders</li>
 *   <li>Redirect movement for players under inverted-controls effect (SiltBlaster)</li>
 * </ul>
 *
 * <p>This listener does not apply damage or effects itself</p>
 */
@RequiredArgsConstructor
public final class WeaponListener implements Listener {

    private final LogCenter log;
    private final GlobalConfigService globals;
    private final WeaponRegistry registry;
    private final WeaponProjectileTracker projectileTracker;
    private final WeaponDropService dropService;
    private final WeaponEffectsState effectsState;
    private final WeaponAmmoDisplayService ammoDisplay;
    private final MatchService matchService;
    private final PlayerActionService actionService;

    /* Right click */

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR
            && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (!actionService.canInteract(player.getUniqueId())) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        WeaponHandler handler = registry.find(item);
        if (handler == null) return;

        handler.onRightClick(buildContext(player, item));
    }

    /* Left click */

    @EventHandler(priority = EventPriority.NORMAL)
    public void onArmSwing(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) return;

        Player player = event.getPlayer();
        if (!actionService.canInteract(player.getUniqueId())) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        WeaponHandler handler = registry.find(item);
        if (handler == null) return;

        event.setCancelled(true);
        handler.onLeftClick(buildContext(player, item));
    }

    /* Sneak toggle */

    @EventHandler(priority = EventPriority.NORMAL)
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (!actionService.canInteract(player.getUniqueId())) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        WeaponHandler handler = registry.find(item);
        if (handler == null) return;

        if (handler.suppressSneakAnimation()) {
            event.setCancelled(true); // prevents server broadcasting crouch
        }

        handler.onSneakToggle(buildContext(player, item), event.isSneaking());
    }

    /* Melee hit */

    @EventHandler(priority = EventPriority.HIGH)
    public void onMeleeHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;

        ItemStack item = attacker.getInventory().getItemInMainHand();
        WeaponHandler handler = registry.find(item);
        if (handler == null) return;

        if (!handler.skipMeleeCancel(attacker.getUniqueId())) {
            event.setCancelled(true);
        }

        Entity victim = event.getEntity();

        if (victim instanceof Player victimPlayer) {
            // Full check: alive state + friendly fire
            if (!actionService.canAttackTarget(attacker.getUniqueId(), victimPlayer.getUniqueId())) return;
        } else {
            // Non-player entity - no friendly fire check, just attacker state
            if (!actionService.canAttack(attacker.getUniqueId())) return;
        }

        handler.onMeleeHit(buildContext(attacker, item), victim);
    }

    /* Projectile hit */

    @EventHandler(priority = EventPriority.NORMAL)
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();

        WeaponProjectileTracker.TrackedProjectile tracked =
            projectileTracker.consume(projectile.getUniqueId());
        if (tracked == null) return;

        WeaponHandler handler = registry.get(tracked.weaponId());
        if (handler == null) return;

        Player shooter = Bukkit.getPlayer(tracked.shooterId());
        if (shooter == null) return;
        if (!actionService.canAttack(shooter.getUniqueId())) return;

        MatchSession session = matchService.getSession().orElse(null);
        boolean effectsActive = matchService.isPlayerInActiveMatch(shooter.getUniqueId());

        handler.onProjectileHit(new WeaponProjectileContext(
            shooter,
            projectile,
            event.getHitEntity(),
            session,
            effectsActive
        ));
    }

    /* Slot change - interrupt reload */

    @EventHandler(priority = EventPriority.NORMAL)
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        PlayerInventory inv = player.getInventory();

        ItemStack previous = player.getInventory().getItem(event.getPreviousSlot());
        WeaponHandler prevHandler = registry.find(previous);
        if (prevHandler != null) {
            log.warn("Clearing ammo display");
            prevHandler.onSlotChange(player);
            ammoDisplay.clear(player);
        }

        ItemStack next = inv.getItem(event.getNewSlot());
        WeaponHandler nextHandler = registry.find(next);

        if (nextHandler != null && !next.isEmpty()) {
            log.warn("Updating ammo display for next item " + next);
            ammoDisplay.update(player, next, nextHandler.getConfig());
        } else if (prevHandler != null) {
            player.sendActionBar(Component.empty());
        }
    }

    /* Hand swap - prevent during reload */

    @EventHandler(priority = EventPriority.NORMAL)
    public void onHandSwap(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        WeaponHandler handler = registry.find(item);
        if (handler == null) return;

        if (handler.preventsSwapDuringReload(player)) {
            event.setCancelled(true);
        }
    }

    /* Item drop */

    @EventHandler(priority = EventPriority.NORMAL)
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack dropped = event.getItemDrop().getItemStack();
        WeaponHandler handler = registry.find(dropped);
        if (handler == null) return;

        if (handler.preventsDropDuringReload(event.getPlayer())) {
            event.setCancelled(true);
        }

        dropService.registerDroppedItem(event.getItemDrop());
    }

    /* Block break */

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        WeaponHandler handler = registry.find(item);
        if (handler == null) return;

        if (handler.preventsBlockBreak(player)) {
            event.setCancelled(true);
        }
    }

    /* Inverted controls */

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!effectsState.hasInvertedControls(player.getUniqueId())) return;

        var to = event.getTo();
        var from = event.getFrom();

        // Ignore head-turn-only packets (no XZ positional change)
        if (from.getBlockX() == to.getBlockX() && from.getBlockZ() == to.getBlockZ()) return;

        double deltaX = to.getX() - from.getX();
        double deltaZ = to.getZ() - from.getZ();

        var newTo = from.clone();
        newTo.setX(from.getX() - deltaX);
        newTo.setZ(from.getZ() - deltaZ);
        newTo.setY(to.getY());
        newTo.setYaw(to.getYaw());
        newTo.setPitch(to.getPitch());

        event.setTo(newTo);
    }

    /* Internals */

    private WeaponContext buildContext(Player player, ItemStack item) {
        MatchSession session = matchService.getSession().orElse(null);
        boolean effectsActive = globals.get().weaponDebugBypass()
            || matchService.isPlayerInActiveMatch(player.getUniqueId());
        return new WeaponContext(player, item, session, effectsActive);
    }

}
