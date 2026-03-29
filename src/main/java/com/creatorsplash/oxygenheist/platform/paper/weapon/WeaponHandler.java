package com.creatorsplash.oxygenheist.platform.paper.weapon;

import com.creatorsplash.oxygenheist.application.match.MatchLifecycle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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
     * Called when a projectile fired by this weapon hits something
     */
    default void onProjectileHit(WeaponProjectileContext ctx) {}

    /**
     * Called every 2 ticks for each player currently holding this weapon
     *
     * <p>Used for reload animation, ammo HUD updates, and continuous-fire logic</p>
     */
    default void tick(Player player, ItemStack item) {}

}
