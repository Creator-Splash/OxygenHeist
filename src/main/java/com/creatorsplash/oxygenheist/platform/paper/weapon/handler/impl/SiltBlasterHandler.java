package com.creatorsplash.oxygenheist.platform.paper.weapon.handler.impl;

import com.creatorsplash.oxygenheist.application.match.combat.weapon.WeaponCooldownService;
import com.creatorsplash.oxygenheist.domain.match.MatchSession;
import com.creatorsplash.oxygenheist.platform.paper.OxygenHeistPlugin;
import com.creatorsplash.oxygenheist.platform.paper.config.weapon.WeaponTypeConfig;
import com.creatorsplash.oxygenheist.platform.paper.util.MM;
import com.creatorsplash.oxygenheist.platform.paper.util.ParticleUtils;
import com.creatorsplash.oxygenheist.platform.paper.weapon.WeaponContext;
import com.creatorsplash.oxygenheist.platform.paper.weapon.WeaponEffectsState;
import com.creatorsplash.oxygenheist.platform.paper.weapon.handler.AbstractWeaponHandler;
import com.creatorsplash.oxygenheist.platform.paper.weapon.provider.WeaponItemProvider;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

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

    public SiltBlasterHandler(
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

        activate(player, ctx.item(), ctx.session());
        cooldown.recordUse(id);
    }

    /* == Activation == */

    private void activate(Player player, ItemStack item, @Nullable MatchSession session) {
        player.sendRichMessage("<green>SILT BLAST POOOPY");

        var loc = player.getLocation();
        var world = player.getWorld();

        if (config.sounds().fire() != null) config.sounds().fire().playTo(player);

        ParticleUtils.spawn(Particle.CAMPFIRE_COSY_SMOKE, loc,
            1, 0.1, 0.1, 0.1, 0.01, session);
        ParticleUtils.spawn(Particle.CLOUD, loc,
            2, 0.1, 0.1, 0.1, 0.01, session);

        // Hide weapon for cooldown duration then restore
        var stored = item.clone();
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
