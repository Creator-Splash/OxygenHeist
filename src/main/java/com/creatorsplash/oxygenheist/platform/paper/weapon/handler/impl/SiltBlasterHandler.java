package com.creatorsplash.oxygenheist.platform.paper.weapon.handler.impl;

import com.creatorsplash.oxygenheist.application.match.combat.weapon.WeaponCooldownService;
import com.creatorsplash.oxygenheist.platform.paper.OxygenHeistPlugin;
import com.creatorsplash.oxygenheist.platform.paper.config.weapon.WeaponTypeConfig;
import com.creatorsplash.oxygenheist.platform.paper.util.MM;
import com.creatorsplash.oxygenheist.platform.paper.weapon.WeaponContext;
import com.creatorsplash.oxygenheist.platform.paper.weapon.WeaponEffectsState;
import com.creatorsplash.oxygenheist.platform.paper.weapon.handler.AbstractWeaponHandler;
import com.creatorsplash.oxygenheist.platform.paper.weapon.provider.WeaponItemProvider;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Silt Blaster - right-click to detonate a silt cloud
 * <p>
 * Nearby enemies receive blindness, slowness, and nausea,
 * and have their movement controls inverted for the effect duration
 * <p>The weapon is hidden from the shooters hand during the cooldown window</p>
 */
public final class SiltBlasterHandler extends AbstractWeaponHandler {

    private static final String ID = "silt_blaster";

    private final OxygenHeistPlugin plugin;
    private final WeaponEffectsState effectsState;
    private final WeaponCooldownService cooldown = new WeaponCooldownService();

    protected SiltBlasterHandler(
            OxygenHeistPlugin plugin,
            WeaponTypeConfig config,
            WeaponItemProvider provider,
            WeaponEffectsState effectsState
    ) {
        super(config, provider);
        this.plugin = plugin;
        this.effectsState = effectsState;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean preventsBlockBreak(Player player) {
        return true;
    }

    /* == Input == */

    @Override
    public void onRightClick(WeaponContext ctx) {
        if (!ctx.effectsActive()) return;
        Player player = ctx.player();
        UUID id = player.getUniqueId();

        if (cooldown.isOnCooldown(id, config.timing().cooldownMs())) {
            long remaining = (cooldown.remainingMillis(id, config.timing().cooldownMs()) / 1000) + 1;
            player.sendActionBar(MM.msg("<yellow>Silt Blaster cooldown: " + remaining + "s"));
            return;
        }

        activate(player, ctx.item());
        cooldown.recordUse(id);
    }

    /* == Activation == */

    private void activate(Player player, ItemStack item) {
        player.sendRichMessage("<green>SILT BLAST POOOPY");
    }

    /* == Lifecycle == */

    @Override
    public void onMatchEnd() {
        cooldown.clearAll();
        // effectsState.onMatchEnd() clears invertedControls - called separately by lifecycle wiring?
    }

    @Override
    public void onPlayerLeave(UUID playerId) {
        cooldown.clear(playerId);
    }

}
