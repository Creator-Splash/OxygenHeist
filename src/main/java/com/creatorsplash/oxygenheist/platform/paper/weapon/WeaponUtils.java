package com.creatorsplash.oxygenheist.platform.paper.weapon;

import com.creatorsplash.oxygenheist.platform.paper.OxygenHeistPlugin;
import com.creatorsplash.oxygenheist.platform.paper.util.MM;
import com.creatorsplash.oxygenheist.platform.paper.util.PDCKeys;
import com.creatorsplash.oxygenheist.platform.paper.weapon.provider.WeaponItemProvider;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * Static helpers shared across all weapon handler implementations
 */
@UtilityClass
public class WeaponUtils {

    /**
     * @return true if the given item carries our weapon PDC tag matching {@code id}
     */
    public boolean doWeaponHandle(ItemStack item, String id) {
        if (item == null || !item.hasItemMeta()) return false;
        String stored = item.getItemMeta()
            .getPersistentDataContainer()
            .get(PDCKeys.WEAPON_ID, PersistentDataType.STRING);
        return id.equals(stored);
    }

    /**
     * Calculates the zero-based reload frame index for the current elapsed time
     *
     * <p>Pure calculation - callers are responsible for passing the result to
     * {@link WeaponItemProvider#applyReloadFrame}</p>
     *
     * @param elapsedMs time elapsed since reload started
     * @param reloadMs total reload duration
     * @param totalFrames total number of reload frames defined for this weapon
     * @return clamped zero-based frame index
     */
    public int calculateReloadFrameIndex(long elapsedMs, long reloadMs, int totalFrames) {
        if (totalFrames <= 0) return 0;
        long phase = Math.max(1, reloadMs / totalFrames);
        int index = (int) (elapsedMs / phase);
        return Math.min(index, totalFrames - 1);
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
     * Converts a snake_case weapon id to a Title Case display name
     * <p>e.g. {@code "claw_cannon"} -> {@code "Claw Cannon"}</p>
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
