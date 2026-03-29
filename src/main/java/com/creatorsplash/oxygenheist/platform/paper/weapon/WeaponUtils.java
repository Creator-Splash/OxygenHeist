package com.creatorsplash.oxygenheist.platform.paper.weapon;

import com.creatorsplash.oxygenheist.platform.paper.OxygenHeistPlugin;
import com.creatorsplash.oxygenheist.platform.paper.config.weapon.WeaponTypeConfig;
import com.creatorsplash.oxygenheist.platform.paper.util.MM;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * Static helpers shared across all weapon handler implementations
 */
@UtilityClass
public class WeaponUtils {

    public ItemStack createWeaponItem(String id, Material material, WeaponTypeConfig config) {
        ItemStack item = new ItemStack(material);
        item.editMeta(meta -> {
            meta.displayName(MM.item(WeaponUtils.formatDisplayName(id)));
            meta.setCustomModelData(config.cmds().get("base"));
            meta.getPersistentDataContainer().set(
                OxygenHeistPlugin.weaponIdKey(), PersistentDataType.STRING, id
            );
        });

        return item;
    }

    public boolean doWeaponHandle(ItemStack item, String id) {
        if (item == null || !item.hasItemMeta()) return false;
        String stored = item.getItemMeta()
            .getPersistentDataContainer()
            .get(OxygenHeistPlugin.weaponIdKey(), PersistentDataType.STRING);
        return id.equals(stored);
    }

    /**
     * Returns the custom model data of the given item, or -1 if absent.
     */
    public int getCmd(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return -1;
        ItemMeta meta = item.getItemMeta();
        return meta.hasCustomModelData() ? meta.getCustomModelData() : -1;
    }

    /**
     * Sets the custom model data on the given item.
     */
    public void setCmd(ItemStack item, int cmd) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.setCustomModelData(cmd);
        item.setItemMeta(meta);
    }

    /**
     * Returns an action-bar component showing ammo as {@code Ammo: current/max}.
     */
    public Component ammoBar(int current, int max) {
        return MM.msg("<yellow>Ammo: <white>" + current + "/" + max);
    }

    /**
     * Returns an action-bar component showing reload progress as a filled bar.
     */
    public Component reloadBar(double progress) {
        int filled = (int) Math.round(20 * progress);
        String bar = "<green>" + "█".repeat(filled) + "<gray>" + "░".repeat(20 - filled);
        return MM.msg(bar);
    }

    /**
     * Advances the weapons CMD to the correct reload animation frame based on elapsed time.
     *
     * <p>Reads reload frame keys from config in order ({@code reload-0}, {@code reload-1}, etc.)
     * and sets the items CMD to the appropriate frame.</p>
     */
    public void applyReloadFrame(ItemStack item, long elapsedMs, WeaponTypeConfig config) {
        List<String> frames = config.cmds().reloadFrameKeys();
        if (frames.isEmpty()) return;
        long reloadMs = config.timing().reloadMs();
        long phase = Math.max(1, reloadMs / frames.size());
        int index = (int) (elapsedMs / phase);
        if (index >= frames.size()) index = frames.size() - 1;
        setCmd(item, config.cmds().get(frames.get(index)));
    }

    /**
     * Converts a snake_case weapon id to a Title Case display name.
     * e.g. {@code "claw_cannon"} -> {@code "Claw Cannon"}
     */
    public String formatDisplayName(String weaponId) {
        String[] words = weaponId.split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(Character.toUpperCase(word.charAt(0)));
            sb.append(word.substring(1).toLowerCase());
        }
        return sb.toString();
    }

}
