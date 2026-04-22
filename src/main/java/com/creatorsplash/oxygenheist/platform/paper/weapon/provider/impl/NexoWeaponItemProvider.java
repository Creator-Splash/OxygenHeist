package com.creatorsplash.oxygenheist.platform.paper.weapon.provider.impl;

import com.creatorsplash.oxygenheist.application.common.LogCenter;
import com.creatorsplash.oxygenheist.platform.paper.config.weapon.WeaponConfigService;
import com.creatorsplash.oxygenheist.platform.paper.weapon.provider.WeaponItemProvider;
import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.api.events.NexoItemsLoadedEvent;
import com.nexomc.nexo.items.ItemBuilder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

/**
 * {@link WeaponItemProvider} implementation backed by Nexo
 */
public final class NexoWeaponItemProvider extends AbstractWeaponProvider<ItemStack> implements Listener {

    private volatile boolean ready = false;

    public NexoWeaponItemProvider(
        @NotNull WeaponConfigService weaponConfig,
        @NotNull LogCenter logCenter
    ) {
        super(weaponConfig, logCenter);
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
    protected @Nullable ItemStack findCustomItem(String sourceId) {
        ItemBuilder builder = NexoItems.itemFromId(stripNamespace(sourceId)); // already returns null if not found
        return builder == null ? null : builder.build();
    }

    @Override
    protected @NotNull ItemStack requireItem(ItemStack customItem) {
        return customItem;
    }

    @Override
    protected @NonNull ItemStack requireCustomItem(String sourceId) {
        ItemBuilder builder = NexoItems.itemFromId(stripNamespace(sourceId));
        if (builder == null) throw new IllegalStateException(
            "Nexo item not found: '" + sourceId + "' - is it defined in Nexo config?"
        );
        return builder.build();
    }

    /**
     * Strips the namespace prefix from a namespaced ID
     */
    private String stripNamespace(String id) {
        int colon = id.indexOf(':');
        return colon != -1 ? id.substring(colon + 1) : id;
    }

}
