package com.creatorsplash.oxygenheist.platform.paper.weapon.provider.impl;

import com.creatorsplash.oxygenheist.platform.paper.config.weapon.WeaponConfigService;
import com.creatorsplash.oxygenheist.platform.paper.weapon.provider.WeaponItemProvider;
import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.api.events.NexoItemsLoadedEvent;
import com.nexomc.nexo.items.ItemBuilder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

/**
 * {@link WeaponItemProvider} implementation backed by Nexo
 */
public final class NexoWeaponItemProvider extends AbstractWeaponProvider<ItemStack> implements Listener {

    private volatile boolean ready = false;

    public NexoWeaponItemProvider(@NotNull WeaponConfigService weaponConfig) {
        super(weaponConfig);
    }

    @EventHandler
    public void onNexoLoaded(NexoItemsLoadedEvent event) {
        this.ready = true;
    }

    @Override
    public boolean isReady() {
        return this.ready;
    }

    /* Internals */

    @Override
    protected @NotNull ItemStack requireItem(ItemStack customItem) {
        return customItem;
    }

    @Override
    protected @NonNull ItemStack requireCustomItem(String sourceId) {
        ItemBuilder builder = NexoItems.itemFromId(sourceId);
        if (builder == null) throw new IllegalStateException(
            "Nexo item not found: '" + sourceId + "' - is it defined in Nexo config?"
        );
        return builder.build();
    }

}
