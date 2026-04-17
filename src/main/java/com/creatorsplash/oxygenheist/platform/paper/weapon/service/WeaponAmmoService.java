package com.creatorsplash.oxygenheist.platform.paper.weapon.service;

import com.creatorsplash.oxygenheist.platform.paper.util.PDCKeys;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * PDC-based ammo tracking for weapons with per-item-instance ammo counts
 *
 * <p>Each item is assigned a stable UUID written into its
 * {@link org.bukkit.persistence.PersistentDataContainer} the first time ammo is accessed.
 * Ammo counts are then tracked in-memory keyed by that UUID, meaning ammo state
 * survives drop and pickup</p>
 *
 * <p>Not all weapons use this - handlers that manage ammo differently
 * (e.g, VenomSpitters inventory-based system) should not use this service</p>
 *
 * <p>Each handler instance should own its own {@code WeaponAmmoService}
 * so ammo maps are isolated per weapon type</p>
 */
public final class WeaponAmmoService {

    private final int maxAmmo;

    public WeaponAmmoService(int maxAmmo) {
        this.maxAmmo = maxAmmo;
    }

    /**
     * Returns the current ammo count for the given item
     *
     * <p>If the item has no tracked ID yet, it is assigned one and returned at max ammo</p>
     */
    public int getAmmo(ItemStack item) {
        if (!item.hasItemMeta()) return maxAmmo;
        var pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.getOrDefault(PDCKeys.WEAPON_AMMO, PersistentDataType.INTEGER, maxAmmo);
    }

    /**
     * Sets the ammo count for the given item, clamped to [0, maxAmmo]
     */
    public void setAmmo(ItemStack item, int amount) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(
            PDCKeys.WEAPON_AMMO, PersistentDataType.INTEGER,
            Math.max(0, Math.min(amount, maxAmmo))
        );
        item.setItemMeta(meta);
    }

    /**
     * Decrements ammo by one, floored at zero
     */
    public void consumeOne(ItemStack item) {
        setAmmo(item, getAmmo(item) - 1);
    }

    /**
     * Restores ammo to the maximum for the given item
     */
    public void refill(ItemStack item) {
        setAmmo(item, maxAmmo);
    }

    /**
     * Returns true if the given item has at least one ammo remaining
     */
    public boolean hasAmmo(ItemStack item) {
        return getAmmo(item) > 0;
    }

    /**
     * Returns the max ammo capacity for this weapon type
     */
    public int maxAmmo() {
        return maxAmmo;
    }

}
