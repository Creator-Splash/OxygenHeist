package com.creatorsplash.oxygenheist.platform.paper.weapon.provider;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

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

    @Nullable
    default ItemStack getFrameItem(String weaponId, String frameName) {
        return null;
    }

    /**
     * Applies a named frame to the item, e.g, "idle", "charged", "launch"
     * <p>No-ops silently if the frame is not defined for this weapon</p>
     */
    void applyFrame(ItemStack item, String weaponId, String frameName);

    /**
     * Transitions the given item to the correct reload animation frame
     *
     * <p>Mutates the item in place. {@code frameIndex} is zero-based.
     * Implementations should clamp silently if {@code frameIndex} exceeds
     * the number of frames defined for this weapon</p>
     *
     * @param item the item currently in the players hand
     * @param weaponId the weapons config id
     * @param frameIndex zero-based frame index
     */
    default void applyReloadFrame(ItemStack item, String weaponId, int frameIndex) {
        applyFrame(item, weaponId, "reload_" + frameIndex);
    }

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
    default void applyAimFrame(ItemStack item, String weaponId) {
        applyFrame(item, weaponId, "aim");
    }

    /**
     * Transitions the given item back to its base visual state
     *
     * <p>Called when a reload completes or is interrupted</p>
     *
     * @param item the item currently in the player's hand
     * @param weaponId the weapons config id
     */
    default void applyIdleFrame(ItemStack item, String weaponId) {
        applyFrame(item, weaponId, "idle");
    }

    /**
     * Returns true if the provider has fully loaded and is safe to call
     *
     * <p>Providers backed by plugins with async loading (e.g, ItemsAdder)
     * must return false until their load event has fired</p>
     */
    boolean isReady();

}
