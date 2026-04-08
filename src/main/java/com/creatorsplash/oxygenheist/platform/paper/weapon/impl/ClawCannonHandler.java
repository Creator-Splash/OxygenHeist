package com.creatorsplash.oxygenheist.platform.paper.weapon.impl;

import com.creatorsplash.oxygenheist.application.match.combat.weapon.WeaponCooldownService;
import com.creatorsplash.oxygenheist.platform.paper.OxygenHeistPlugin;
import com.creatorsplash.oxygenheist.platform.paper.config.weapon.WeaponTypeConfig;
import com.creatorsplash.oxygenheist.platform.paper.weapon.handler.WeaponHandler;
import com.creatorsplash.oxygenheist.platform.paper.weapon.WeaponUtils;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Claw Cannon | right-click to rocket-launch yourself in the facing direction.
 * <p>
 * On landing, deal falloff AoE damage to nearby entities.
 * Melee hit applies heavy knockback.
 */
@RequiredArgsConstructor
public final class ClawCannonHandler implements WeaponHandler {

    private static final String ID = "claw_cannon";
    private static final Material MATERIAL = Material.GOLDEN_HORSE_ARMOR;

    private final OxygenHeistPlugin plugin;
    private final WeaponTypeConfig config;
    private final WeaponCooldownService cooldown = new WeaponCooldownService();

    @Override public String id() { return ID; }

    @Override
    public boolean handles(ItemStack item) {
        return WeaponUtils.doWeaponHandle(item, ID);
    }

    @Override
    public ItemStack createItemStack() {
        return WeaponUtils.createWeaponItem(ID, MATERIAL, config);
    }
}
