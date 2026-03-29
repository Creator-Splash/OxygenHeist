package com.creatorsplash.oxygenheist.platform.paper.weapon;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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

    private static final String PDC_KEY = "weapon_item_id";

    private final NamespacedKey key;
    private final Map<String, Integer> ammoByItemId = new HashMap<>();
    private final int maxAmmo;

    public WeaponAmmoService(JavaPlugin plugin, int maxAmmo) {
        this.maxAmmo = maxAmmo;
        this.key = new NamespacedKey(plugin, PDC_KEY);
    }

    /**
     * Returns the current ammo count for the given item
     *
     * <p>If the item has no tracked ID yet, it is assigned one and returned at max ammo</p>
     */
    public int getAmmo(ItemStack item) {
        String id = resolveId(item);
        return ammoByItemId.getOrDefault(id, maxAmmo);
    }

    /**
     * Sets the ammo count for the given item, clamped to [0, maxAmmo]
     */
    public void setAmmo(ItemStack item, int amount) {
        String id = resolveId(item);
        ammoByItemId.put(id, Math.max(0, Math.min(amount, maxAmmo)));
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

    /**
     * Clears all tracked ammo state
     *
     * <p>Call from {@link WeaponHandler#onMatchEnd()}</p>
     */
    public void clearAll() {
        ammoByItemId.clear();
    }

    /* Internals */

    private String resolveId(ItemStack item) {
        if (!item.hasItemMeta()) return UUID.randomUUID().toString();

        ItemMeta meta = item.getItemMeta();

        if (meta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
            return meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        }

        String id = UUID.randomUUID().toString();
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, id);
        item.setItemMeta(meta);

        return id;
    }

}
