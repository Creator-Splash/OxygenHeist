package com.creatorsplash.oxygenheist.platform.paper.display;

import com.creatorsplash.oxygenheist.application.common.LogCenter;
import com.creatorsplash.oxygenheist.application.match.MatchLifecycle;
import com.creatorsplash.oxygenheist.platform.paper.config.GlobalConfigService;
import com.creatorsplash.oxygenheist.platform.paper.config.weapon.WeaponTypeConfig;
import com.creatorsplash.oxygenheist.platform.paper.util.PDCKeys;
import com.creatorsplash.oxygenheist.platform.paper.weapon.provider.impl.AbstractWeaponProvider;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the physical ammo display in the players offhand slot
 */
@RequiredArgsConstructor
public final class WeaponAmmoDisplayService implements MatchLifecycle {

    private final LogCenter log;
    private final GlobalConfigService globals;
    private final AbstractWeaponProvider<?> itemProvider;

    /** Last ammo count written to each player's offhand, to skip redundant writes */
    private final Map<UUID, Integer> lastDisplayed = new HashMap<>();

    public void update(Player player, ItemStack weaponItem, WeaponTypeConfig config) {
        if (!globals.get().physicalAmmoDisplay()) return;

        String frameKey = config.ammo().displayItem();
        if (frameKey == null) return;

        int ammo = readAmmo(weaponItem, config);

        Integer last = lastDisplayed.get(player.getUniqueId());
        if (last != null && last == ammo) return; // no change - skip inventory write

        ItemStack display = resolveDisplayItem(config);
        if (display == null) return;

        display.setAmount(Math.clamp(ammo, 1, 99));
        player.getInventory().setItemInOffHand(display);
        lastDisplayed.put(player.getUniqueId(), ammo);
    }

    /**
     * Clears the offhand slot and removes tracking for the given player
     */
    public void clear(Player player) {
        if (!globals.get().physicalAmmoDisplay()) return;
        if (!lastDisplayed.containsKey(player.getUniqueId())) return;

        player.getInventory().setItemInOffHand(null);
        lastDisplayed.remove(player.getUniqueId());
    }

    /* == Lifecycle == */

    @Override
    public void onMatchEnd() {
        lastDisplayed.forEach((uuid, ignored) -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) player.getInventory().setItemInOffHand(null);
        });
        lastDisplayed.clear();
    }

    @Override
    public void onPlayerLeave(UUID playerId) {
        lastDisplayed.remove(playerId); // player is offline, no need to clear inventory
    }

    /* == Internals == */

    private @Nullable ItemStack resolveDisplayItem(WeaponTypeConfig config) {
        String displayItem = config.ammo().displayItem();
        if (displayItem == null) return null;

        String frameItemId = config.frames().get(displayItem);
        if (frameItemId != null) {
            ItemStack frame = itemProvider.getFrameItem(frameItemId);
            if (frame != null) return frame;
        }

        try {
            return new ItemStack(Material.valueOf(displayItem.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private int readAmmo(ItemStack item, WeaponTypeConfig config) {
        if (!item.hasItemMeta()) return config.ammo().maxAmmo();
        return item.getItemMeta()
            .getPersistentDataContainer()
            .getOrDefault(PDCKeys.WEAPON_AMMO, PersistentDataType.INTEGER, config.ammo().maxAmmo());
    }

}
