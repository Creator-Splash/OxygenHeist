package com.creatorsplash.oxygenheist.platform.paper.weapon.provider.impl;

import com.creatorsplash.oxygenheist.platform.paper.OxygenHeistPlugin;
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
        @NotNull OxygenHeistPlugin plugin,
        @NotNull WeaponConfigService weaponConfig
    ) {
        this.weaponConfig = weaponConfig;

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
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
        CustomStack stack = requireCustomStack(config.itemId());

        ItemStack item = stack.getItemStack();
        item.editMeta(meta -> {
            if (displayName != null) {
                meta.displayName(MM.item(displayName));
            }
            meta.getPersistentDataContainer().set(
                PDCKeys.WEAPON_ID, PersistentDataType.STRING, weaponId
            );
        });
        return item;
    }

    @Override
    public void applyReloadFrame(ItemStack item, String weaponId, int frameIndex) {
        WeaponTypeConfig config = requireConfig(weaponId);
        String frameItemId = config.itemId() + "_reload_" + frameIndex;
        CustomStack frameStack = CustomStack.getInstance(frameItemId);
        if (frameStack == null) return;

        copyModelData(item, frameStack);
    }

    @Override
    public void applyAimFrame(ItemStack item, String weaponId) {
        WeaponTypeConfig config = requireConfig(weaponId);
        CustomStack aimStack = CustomStack.getInstance(config.itemId() + "_aim");
        if (aimStack == null) return;
        copyModelData(item, aimStack);
    }

    @Override
    public void applyBaseFrame(ItemStack item, String weaponId) {
        WeaponTypeConfig config = requireConfig(weaponId);
        CustomStack stack = requireCustomStack(config.itemId());

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
