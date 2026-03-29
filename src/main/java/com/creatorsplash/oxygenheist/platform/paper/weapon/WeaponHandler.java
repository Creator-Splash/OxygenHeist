package com.creatorsplash.oxygenheist.platform.paper.weapon;

import com.creatorsplash.oxygenheist.application.match.MatchLifecycle;
import com.creatorsplash.oxygenheist.platform.paper.listener.WeaponListener;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Contract for a single weapons behaviour
 *
 * <p>Each weapon implements this interface and registers itself with {@link WeaponRegistry}</p>
 */
public interface WeaponHandler extends MatchLifecycle {

    /**
     * Unique string identifier for this weapon, used in config and logging
     *
     * <p>Should be snake_case, e.g, {@code "claw_cannon"}</p>
     */
    String id();

    /**
     * Returns true if this handler owns the given item
     *
     * <p>Implementations should check material and custom model data.
     * This must cover all visual states - base, aim, and all reload frames</p>
     */
    boolean handles(ItemStack item);

    /**
     * Creates a fresh item stack for this weapon at full ammo
     *
     * <p>Called by the distribution and spawner systems to give weapons to players</p>
     */
    ItemStack createItemStack();

    /**
     * Called when the player right-clicks while holding this weapon
     */
    default void onRightClick(WeaponContext ctx) {}

    /**
     * Called when the player left-clicks (arm swing) while holding this weapon
     */
    default void onLeftClick(WeaponContext ctx) {}

    /**
     * Called when the player toggles sneak while holding this weapon
     *
     * @param sneaking true if the player is now sneaking, false if standing up
     */
    default void onSneakToggle(WeaponContext ctx, boolean sneaking) {}

    /**
     * Called when the player lands a melee hit while holding this weapon
     *
     * <p>Friendly fire and match-state checks are already applied by the listener
     * before this is called</p>
     *
     * @param victim the entity that was hit
     */
    default void onMeleeHit(WeaponContext ctx, Entity victim) {}

    /**
     * Return true to allow a weapon-sourced programmatic {@code entity.damage()} call
     * to pass through {@link WeaponListener}
     * without being canceled.
     *
     * <p>Handlers that deal damage directly (explosion, raycast, projectile hit) must
     * add the attacker UUID to their bypass set before calling {@code entity.damage()},
     * return true here while it is present, and remove it immediately after the call.</p>
     */
    default boolean skipMeleeCancel(UUID attackerId) { return false; }

    /**
     * Called when a projectile fired by this weapon hits something
     */
    default void onProjectileHit(WeaponProjectileContext ctx) {}

    /**
     * Called when the player switches hotbar slot while holding this weapon.
     *
     * <p>Used to interrupt in-progress reloads. The player reference is the
     * one who WAS holding this weapon before the switch.</p>
     */
    default void onSlotChange(Player player) {}

    /**
     * @return true to cancel a hand-swap event while this player is mid-reload.
     */
    default boolean preventsSwapDuringReload(Player player) { return false; }

    /**
     * @return  true to cancel a drop event for this weapon item while reloading.
     */
    default boolean preventsDropDuringReload(Player player) { return false; }

    /**
     * @return  true to cancel block-break events while this weapon is held.
     */
    default boolean preventsBlockBreak(Player player) { return false; }

    /**
     * Called every 2 ticks for each player currently holding this weapon
     *
     * <p>Used for reload animation, ammo HUD updates, and continuous-fire logic</p>
     */
    default void tick(Player player, ItemStack item) {}

}
