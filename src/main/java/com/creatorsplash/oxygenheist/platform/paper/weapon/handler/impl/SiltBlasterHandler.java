package com.creatorsplash.oxygenheist.platform.paper.weapon.handler.impl;

import com.creatorsplash.oxygenheist.application.match.Scheduler;
import com.creatorsplash.oxygenheist.application.match.combat.weapon.WeaponCooldownService;
import com.creatorsplash.oxygenheist.domain.match.MatchSession;
import com.creatorsplash.oxygenheist.platform.paper.config.weapon.WeaponTypeConfig;
import com.creatorsplash.oxygenheist.platform.paper.util.MM;
import com.creatorsplash.oxygenheist.platform.paper.util.ParticleUtils;
import com.creatorsplash.oxygenheist.platform.paper.weapon.WeaponContext;
import com.creatorsplash.oxygenheist.platform.paper.weapon.WeaponEffectsState;
import com.creatorsplash.oxygenheist.platform.paper.weapon.handler.AbstractWeaponHandler;
import com.creatorsplash.oxygenheist.platform.paper.weapon.provider.WeaponItemProvider;
import com.creatorsplash.oxygenheist.platform.paper.weapon.service.WeaponHideService;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
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

    private final WeaponEffectsState effectsState;

    private final Scheduler scheduler;
    private final WeaponHideService hideService;
    private final WeaponCooldownService cooldown = new WeaponCooldownService();

    public SiltBlasterHandler(
        Scheduler scheduler,
        WeaponTypeConfig config,
        WeaponItemProvider provider,
        WeaponEffectsState effectsState,
        WeaponHideService hideService
    ) {
        super(config, provider);
        this.scheduler = scheduler;
        this.effectsState = effectsState;
        this.hideService = hideService;
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
        var loc = player.getLocation();

        if (!config.sounds().fire().isEmpty()) config.sounds().fire().playTo(player);

        ParticleUtils.spawn(Particle.CAMPFIRE_COSY_SMOKE, loc,
            1, 0.1, 0.1, 0.1, 0.01, session);
        ParticleUtils.spawn(Particle.CLOUD, loc,
            2, 0.1, 0.1, 0.1, 0.01, session);

        // Hide weapon for cooldown duration then restore
        hideService.hide(player, item, config.timing().cooldownMs() / 50L);

        applyEffects(player.getUniqueId(), loc);
        spawnCloud(loc, session);
    }

    private void applyEffects(UUID casterId, Location center) {
        int duration = config.effects().effectDurationTicks();
        double radius = config.physics().cloudRadius();
        double radiusSq = radius * radius;

        center.getNearbyPlayers(radius, target ->
            !target.getUniqueId().equals(casterId)
                && target.getLocation().distanceSquared(center) <= radiusSq
        ).forEach(target -> {
            target.addPotionEffect(new PotionEffect(
                PotionEffectType.BLINDNESS, duration, 1, false, true));
            target.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS, duration, 2, false, true));
            target.addPotionEffect(new PotionEffect(
                PotionEffectType.NAUSEA, duration, 1, false, true));

            effectsState.addInvertedControls(target.getUniqueId());
            target.sendActionBar(MM.msg("<dark_gray><bold>DISORIENTED!"));

            UUID targetUUID = target.getUniqueId();
            scheduler.runLater(() -> effectsState.removeInvertedControls(targetUUID), duration);
        });
    }

    private void spawnCloud(Location center, @Nullable MatchSession session) {
        CloudTask task = new CloudTask(center, session);
        task.handle = scheduler.runRepeating(task, 0L, 1L);
        trackWorldTask(task.handle);
    }

    @RequiredArgsConstructor
    private final class CloudTask implements Runnable {
        private final Location center;
        private final MatchSession session;
        private int ticks = 0;
        Scheduler.Task handle;

        @Override
        public void run() {
            if (ticks++ >= config.effects().effectDurationTicks()) {
                handle.cancel();
                return;
            }

            double radius = config.physics().cloudRadius();
            for (int i = 0; i < 10; i++) {
                double angle = Math.random() * Math.PI * 2;
                double r = Math.random() * radius;
                double x = Math.cos(angle) * r;
                double z = Math.sin(angle) * r;
                double y = Math.random() * 2.5;
                var particleLoc = center.clone().add(x, y, z);
                ParticleUtils.spawn(Particle.CAMPFIRE_COSY_SMOKE, particleLoc,
                    1, 0.1, 0.1, 0.1, 0.01, session);
                ParticleUtils.spawn(Particle.CLOUD, particleLoc,
                    2, 0.1, 0.1, 0.1, 0.01, session);
            }
        }
    }

    /* == Lifecycle == */

    @Override
    public void onMatchEnd() {
        cooldown.clearAll();
    }

    @Override
    public void onPlayerLeave(UUID playerId) {
        cooldown.clear(playerId);
        effectsState.removeInvertedControls(playerId);
    }

}
