package com.creatorsplash.oxygenheist.platform.paper.weapon.handler.impl;

import com.creatorsplash.oxygenheist.application.match.combat.weapon.WeaponCooldownService;
import com.creatorsplash.oxygenheist.platform.paper.OxygenHeistPlugin;
import com.creatorsplash.oxygenheist.platform.paper.config.weapon.WeaponTypeConfig;
import com.creatorsplash.oxygenheist.platform.paper.weapon.handler.AbstractWeaponHandler;
import com.creatorsplash.oxygenheist.platform.paper.weapon.provider.WeaponItemProvider;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Claw Cannon | right-click to rocket-launch yourself in the facing direction.
 * <p>
 * On landing, deal falloff AoE damage to nearby entities.
 * Melee hit applies heavy knockback.
 */
public final class ClawCannonHandler extends AbstractWeaponHandler {

    private static final String ID = "claw_cannon";
    private static final Material MATERIAL = Material.GOLDEN_HORSE_ARMOR;

    private final OxygenHeistPlugin plugin;
    private final WeaponCooldownService cooldown = new WeaponCooldownService();

    private final Set<UUID> inFlight = new HashSet<>();
    private final Set<UUID> bypassMeleeCancel = new HashSet<>();

    public ClawCannonHandler(
        OxygenHeistPlugin plugin,
        WeaponTypeConfig config,
        WeaponItemProvider provider
    ) {
        super(config, provider);
        this.plugin = plugin;
    }

    @Override public String id() { return ID; }

    @Override
    public boolean preventsBlockBreak(Player player) { return true; }

    @Override
    public boolean skipMeleeCancel(UUID attackerId) {
        return bypassMeleeCancel.contains(attackerId);
    }

    /* == Input == */

    /* == Lifecycle == */

    @Override
    public void onMatchEnd() {
        inFlight.clear();
        cooldown.clearAll();
        bypassMeleeCancel.clear();
    }

    @Override
    public void onPlayerLeave(UUID playerId) {
        inFlight.remove(playerId);
        cooldown.clear(playerId);
        bypassMeleeCancel.remove(playerId);
    }

}
