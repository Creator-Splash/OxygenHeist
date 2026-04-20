package com.creatorsplash.oxygenheist.platform.paper.weapon.provider.impl;

import com.creatorsplash.oxygenheist.platform.paper.config.weapon.WeaponConfigService;
import com.creatorsplash.oxygenheist.platform.paper.config.weapon.WeaponTypeConfig;
import com.creatorsplash.oxygenheist.platform.paper.weapon.provider.WeaponItemProvider;
import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

/**
 * {@link WeaponItemProvider} implementation backed by ItemsAdder
 */
public final class ItemsAdderWeaponItemProvider extends AbstractWeaponProvider<CustomStack> implements Listener {

    private volatile boolean ready = false;

    public ItemsAdderWeaponItemProvider(
        @NotNull WeaponConfigService weaponConfig
    ) {
        super(weaponConfig);
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
    public void applyFrame(ItemStack item, String weaponId, String frameName) {
        WeaponTypeConfig config = requireConfig(weaponId);
        String itemId = config.frames().get(frameName);
        if (itemId == null) return;
        CustomStack stack = CustomStack.getInstance(itemId);
        if (stack == null) return;
        copyModelData(item, stack);
    }

    /* Internals */

    @Override
    protected @NotNull ItemStack requireItem(CustomStack customItem) {
        return customItem.getItemStack();
    }

    @Override
    protected @NonNull CustomStack requireCustomItem(String sourceId) {
        CustomStack stack = CustomStack.getInstance(sourceId);
        if (stack == null) throw new IllegalStateException(
            "ItemsAdder item not found: '" + sourceId + "' - is it defined in IA config?"
        );
        return stack;
    }

}
