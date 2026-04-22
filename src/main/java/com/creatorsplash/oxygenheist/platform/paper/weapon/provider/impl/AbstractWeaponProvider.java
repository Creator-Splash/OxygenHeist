package com.creatorsplash.oxygenheist.platform.paper.weapon.provider.impl;

import com.creatorsplash.oxygenheist.application.common.LogCenter;
import com.creatorsplash.oxygenheist.platform.paper.config.weapon.WeaponConfigService;
import com.creatorsplash.oxygenheist.platform.paper.config.weapon.WeaponTypeConfig;
import com.creatorsplash.oxygenheist.platform.paper.util.MM;
import com.creatorsplash.oxygenheist.platform.paper.util.PDCKeys;
import com.creatorsplash.oxygenheist.platform.paper.weapon.provider.WeaponItemProvider;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractWeaponProvider<T> implements WeaponItemProvider {

    private final LogCenter log;
    private final WeaponConfigService weaponConfig;

    protected AbstractWeaponProvider(
        @NotNull final WeaponConfigService weaponConfig,
        @NotNull final LogCenter log
    ) {
        this.log = log;
        this.weaponConfig = weaponConfig;
    }

    @Override
    public ItemStack createWeaponItem(String weaponId, @Nullable String displayName) {
        if (!isReady()) throw new IllegalStateException(getClass().getSimpleName() + " is not ready");

        WeaponTypeConfig config = requireConfig(weaponId);

        String idleId = config.frames().get("idle");
        if (idleId == null) throw new IllegalStateException(
                "Weapon '" + weaponId + "' has no 'idle' frame defined in weapons.yml"
        );

        T customItem = requireCustomItem(idleId);
        ItemStack item = requireItem(customItem);

        item.editMeta(meta -> {
            if (displayName != null) meta.displayName(MM.item(displayName));
            meta.getPersistentDataContainer().set(
                    PDCKeys.WEAPON_ID, PersistentDataType.STRING, weaponId
            );
        });
        return item;
    }

    @Override
    public void applyFrame(ItemStack item, String weaponId, String frameName) {
        WeaponTypeConfig config = requireConfig(weaponId);
        String itemId = config.frames().get(frameName);

        // Fall back to convention: derive from idle frame prefix
        if (itemId == null) {
            String idleId = config.frames().get("idle");
            if (idleId != null) {
                int lastUnderscore = idleId.lastIndexOf('_');
                if (lastUnderscore != -1) {
                    itemId = idleId.substring(0, lastUnderscore + 1) + frameName;
                }
            }
        }

        if (itemId == null) return;

        T customItem = findCustomItem(itemId);
        if (customItem == null) {
            log.warn("Could not find custom item from frame id: " + itemId);
            return;
        }

        copyModelData(item, customItem);
    }

    public @Nullable ItemStack getFrameItem(String frameItemId) {
        T customItem = findCustomItem(frameItemId);
        if (customItem == null) return null;
        return requireItem(customItem).clone();
    }

    /* Internals */

    protected WeaponTypeConfig requireConfig(String weaponId) {
        WeaponTypeConfig config = weaponConfig.getConfig(weaponId);
        if (config == null) throw new IllegalStateException(
                "No weapon config found for id '" + weaponId + "'"
        );
        return config;
    }

    /**
     * Looks up a custom item by id, returning null if not found
     *
     * <p>Default implementation wraps {@link #requireCustomItem} - override
     * if the backing API has a cheaper nullable lookup path</p>
     */
    protected @Nullable T findCustomItem(String sourceId) {
        try {
            return requireCustomItem(sourceId);
        } catch (IllegalStateException e) {
            return null;
        }
    }

    protected abstract @NotNull ItemStack requireItem(T customItem);

    protected abstract @NotNull T requireCustomItem(String sourceId);

    protected void copyModelData(ItemStack target, T source) {
        ItemMeta sourceMeta = requireItem(source).getItemMeta();
        if (sourceMeta == null) return;

        target.editMeta(meta -> {
            if (sourceMeta.hasItemModel()) {
                meta.setItemModel(sourceMeta.getItemModel());
            }

            if (sourceMeta.hasCustomModelDataComponent()) {
                meta.setCustomModelDataComponent(sourceMeta.getCustomModelDataComponent());
            } else if (sourceMeta.hasCustomModelData()) {
                meta.setCustomModelData(sourceMeta.getCustomModelData());
            } else {
                meta.setCustomModelData(null);
            }
        });
    }

}
