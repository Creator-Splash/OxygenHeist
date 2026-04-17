package com.creatorsplash.oxygenheist.platform.paper.weapon.provider.impl;

import com.creatorsplash.oxygenheist.platform.paper.config.weapon.WeaponConfigService;
import com.creatorsplash.oxygenheist.platform.paper.config.weapon.WeaponTypeConfig;
import com.creatorsplash.oxygenheist.platform.paper.util.MM;
import com.creatorsplash.oxygenheist.platform.paper.util.PDCKeys;
import com.creatorsplash.oxygenheist.platform.paper.weapon.provider.WeaponItemProvider;
import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * {@link WeaponItemProvider} implementation backed by ItemsAdder
 */
public final class ItemsAdderWeaponItemProvider implements WeaponItemProvider, Listener {

    private final WeaponConfigService weaponConfig;
    private volatile boolean ready = false;

    public ItemsAdderWeaponItemProvider(
        @NotNull WeaponConfigService weaponConfig
    ) {
        this.weaponConfig = weaponConfig;
    }

    @EventHandler
    public void onItemsAdderLoad(ItemsAdderLoadDataEvent event) {
        this.ready = true;
    }

    @Override
    public boolean isReady() {
        return this.ready;
    }

    /* Weapon Item Provider */

    @Override
    public ItemStack createWeaponItem(String weaponId, @Nullable String displayName) {
        checkReady();

        WeaponTypeConfig config = requireConfig(weaponId);

        String idleId = config.frames().get("idle");
        if (idleId == null) throw new IllegalStateException(
            "Weapon '" + weaponId + "' has no 'idle' frame defined in weapons.yml"
        );

        CustomStack stack = requireCustomStack(idleId);

        ItemStack item = stack.getItemStack();
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
        if (itemId == null) return;
        CustomStack stack = CustomStack.getInstance(itemId);
        if (stack == null) return;
        copyModelData(item, stack);
    }

    /* Internals */

    private void checkReady() {
        if (!this.ready) throw new IllegalStateException(
            "ItemsAdderWeaponItemProvider is not ready - ItemsAdder has not finished loading"
        );
    }

    private WeaponTypeConfig requireConfig(String weaponId) {
        WeaponTypeConfig config = weaponConfig.getConfig(weaponId);
        if (config == null) throw new IllegalStateException(
            "No weapon config found for id '" + weaponId + "'"
        );
        return config;
    }

    private CustomStack requireCustomStack(String itemId) {
        CustomStack stack = CustomStack.getInstance(itemId);
        if (stack == null) throw new IllegalStateException(
            "ItemsAdder item not found: '" + itemId + "' - is it defined in IA config?"
        );
        return stack;
    }

    private void copyModelData(ItemStack target, CustomStack source) {
        ItemMeta sourceMeta = source.getItemStack().getItemMeta();
        if (sourceMeta == null) return;

        target.editMeta(meta -> {
            meta.setItemModel(sourceMeta.getItemModel());

            if (sourceMeta.hasCustomModelDataComponent()) {
                meta.setCustomModelDataComponent(sourceMeta.getCustomModelDataComponent());
            } else {
                meta.setCustomModelData(null);
            }
        });
    }

}
