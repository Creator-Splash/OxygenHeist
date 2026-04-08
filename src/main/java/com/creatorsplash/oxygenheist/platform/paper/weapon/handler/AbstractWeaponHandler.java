package com.creatorsplash.oxygenheist.platform.paper.weapon.handler;

import com.creatorsplash.oxygenheist.platform.paper.config.weapon.WeaponTypeConfig;
import com.creatorsplash.oxygenheist.platform.paper.weapon.WeaponUtils;
import com.creatorsplash.oxygenheist.platform.paper.weapon.provider.WeaponItemProvider;
import org.bukkit.inventory.ItemStack;

/**
 * Base class for all weapon handlers
 *
 * <p>Provides identity resolution and item creation, both of which are
 * identical across every weapon and should never be overridden</p>
 */
public abstract class AbstractWeaponHandler implements WeaponHandler {

    protected final WeaponTypeConfig config;
    protected final WeaponItemProvider provider;

    protected AbstractWeaponHandler(
        WeaponTypeConfig config,
        WeaponItemProvider provider
    ) {
        this.config = config;
        this.provider = provider;
    }

    @Override
    public final boolean handles(ItemStack item) {
        return WeaponUtils.doWeaponHandle(item, id());
    }

    @Override
    public final ItemStack createItemStack() {
        return provider.createWeaponItem(id(), WeaponUtils.formatDisplayName(id()));
    }

}
