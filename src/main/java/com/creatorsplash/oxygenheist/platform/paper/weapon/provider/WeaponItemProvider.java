package com.creatorsplash.oxygenheist.platform.paper.weapon.provider;

import org.bukkit.inventory.ItemStack;

/**
 * Abstraction over whichever custom item plugin is providing weapon items
 *
 * <p>Implementations are responsible for creating weapon ItemStacks with the
 * correct appearance, and for transitioning items between visual states
 * (base, reload frames). Item identity is always resolved via our own PDC key -
 * this provider only owns appearance and construction</p>
 *
 * <p>Implementations should not be used until {@link #isReady()} returns true</p>
 */
public interface WeaponItemProvider {

    /**
     * Creates a fresh ItemStack for the given weapon ID at its base visual state
     *
     * <p>Implementations must stamp the weapon ID into the item's PDC using
     * {@code OxygenHeistPlugin.weaponIdKey()} so that {@code WeaponUtils#doWeaponHandle}
     * can resolve it later</p>
     *
     * @param weaponId the weapons config id, e.g, {@code "silt_blaster"}
     * @param displayName the formatted display name to apply to the item
     * @throws IllegalStateException if the provider is not ready or the item id is unknown
     */
    ItemStack createWeaponItem(String weaponId, String displayName);

    /**
     * Transitions the given item to the correct reload animation frame
     *
     * <p>Mutates the item in place. {@code frameIndex} is zero-based.
     * Implementations should clamp silently if {@code frameIndex} exceeds
     * the number of frames defined for this weapon</p>
     *
     * @param item the item currently in the player's hand
     * @param weaponId the weapons config id
     * @param frameIndex zero-based frame index
     */
    void applyReloadFrame(ItemStack item, String weaponId, int frameIndex);

    /**
     * Transitions the given item to its aim visual state
     *
     * <p>Mutates the item in place. No-ops silently if the aim variant
     * is not defined for this weapon in the providers config</p>
     *
     * <p>Convention: {@code {itemId}_aim}</p>
     *
     * @param item the item currently in the players hand
     * @param weaponId the weapons config id
     */
    void applyAimFrame(ItemStack item, String weaponId);

    /**
     * Transitions the given item back to its base visual state
     *
     * <p>Called when a reload completes or is interrupted</p>
     *
     * @param item the item currently in the player's hand
     * @param weaponId the weapons config id
     */
    void applyBaseFrame(ItemStack item, String weaponId);

    /**
     * Returns true if the provider has fully loaded and is safe to call
     *
     * <p>Providers backed by plugins with async loading (e.g, ItemsAdder)
     * must return false until their load event has fired</p>
     */
    boolean isReady();

}
