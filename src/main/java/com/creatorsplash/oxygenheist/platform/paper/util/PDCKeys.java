package com.creatorsplash.oxygenheist.platform.paper.util;

import lombok.experimental.UtilityClass;
import org.bukkit.NamespacedKey;

@UtilityClass
public class PDCKeys {

    // == Weapon ==

    /**
     * Stamped onto every weapon ItemStack at creation time
     * <p>Value: the weapon's config id, e.g, {@code "silt_blaster"}</p>
     */
    public final NamespacedKey WEAPON_ID = key("weapon_id");

    /**
     * Tracks per-item ammo count for PDC-based ammo weapons
     * <p>Value: integer ammo count, clamped to [0, maxAmmo]</p>
     */
    public final NamespacedKey WEAPON_AMMO = key("weapon_ammo");

    // == World / Arena ==

    /**
     * Marks an item as the zone selection wand
     * <p>Value: boolean true</p>
     */
    public final NamespacedKey SELECTION_WAND = key("selection_wand");

    /* == Internal == */

    private NamespacedKey key(String value) {
        return new NamespacedKey("oxygenheist", value);
    }

}
