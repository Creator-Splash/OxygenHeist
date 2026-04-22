package com.creatorsplash.oxygenheist.platform.paper.weapon.handler.impl;

import com.creatorsplash.oxygenheist.platform.paper.config.weapon.WeaponTypeConfig;
import com.creatorsplash.oxygenheist.platform.paper.weapon.WeaponContext;
import com.creatorsplash.oxygenheist.platform.paper.weapon.WeaponUtils;
import com.creatorsplash.oxygenheist.platform.paper.weapon.handler.ReloadableWeaponHandler;
import com.creatorsplash.oxygenheist.platform.paper.weapon.provider.WeaponItemProvider;
import com.creatorsplash.oxygenheist.platform.paper.weapon.service.WeaponProjectileContext;
import com.creatorsplash.oxygenheist.platform.paper.weapon.service.WeaponProjectileTracker;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Spike Shooter - burst-fire projectile weapon
 *
 * <p>Sneak to enter aim mode. Left-click while aiming to fire a burst of
 * {@code burst-count} spikes in quick succession. Moving or unsneaking
 * during the burst cancels it and forces a reload</p>
 *
 * <p>Each spike deals damage and applies stacking poison on hit</p>
 */
public final class SpikeShooterHandler extends ReloadableWeaponHandler {

    public static final String ID = "spike_shooter";

    private final WeaponProjectileTracker projectileTracker;

    private final Set<UUID> aiming = new HashSet<>();
    /** Shots remaining in the current burst per player */
    private final Map<UUID, Integer> burstShotsRemaining = new HashMap<>();
    /** Timestamp of the last spike fired in current burst */
    private final Map<UUID, Long> lastBurstShotTime = new HashMap<>();
    /** Position when burst started - movement beyond threshold cancels burst */
    private final Map<UUID, Location> burstStartPositions = new HashMap<>();
    private final Set<UUID> bypassMeleeCancel = new HashSet<>();

    public SpikeShooterHandler(
        @NotNull WeaponTypeConfig config,
        @NotNull WeaponItemProvider provider,
        @NotNull WeaponProjectileTracker projectileTracker
    ) {
        super(config, provider);
        this.projectileTracker = projectileTracker;
    }

    @Override public String id() { return ID; }

    @Override
    public boolean preventsBlockBreak(Player player) { return true; }

    @Override
    public boolean skipMeleeCancel(UUID attackerId) {
        return bypassMeleeCancel.contains(attackerId);
    }

    /* == Input == */

    @Override
    public void onLeftClick(WeaponContext ctx) {
        if (!ctx.effectsActive()) return;
        Player player = ctx.player();
        UUID id = player.getUniqueId();

        if (!aiming.contains(id)) return;
        if (!canFire(player, ctx.item())) return;
        if (burstShotsRemaining.containsKey(id)) return; // already mid-burst

        startBurst(player, ctx.item());
    }

    @Override
    public void onSneakToggle(WeaponContext ctx, boolean sneaking) {
        UUID id = ctx.player().getUniqueId();
        if (sneaking) {
            aiming.add(id);
            provider.applyFrame(ctx.item(), ID, "charged");
        } else {
            if (burstShotsRemaining.containsKey(id)) {
                // Cancel burst on unsneak
                cancelBurst(id);
                startReload(ctx.player(), ctx.item());
            }
            aiming.remove(id);
            provider.applyIdleFrame(ctx.item(), ID);
        }
    }

    @Override
    public void onRightClick(WeaponContext ctx) {
        if (!ctx.effectsActive()) return;
        if (reload.isReloading(ctx.player().getUniqueId())) return;
        if (ammo.getAmmo(ctx.item()) >= config.ammo().maxAmmo()) return;
        startReload(ctx.player(), ctx.item());
    }

    @Override
    public void onSlotChange(Player player) {
        super.onSlotChange(player);
        UUID id = player.getUniqueId();
        cancelBurst(id);
        aiming.remove(id);
        provider.applyIdleFrame(player.getInventory().getItemInMainHand(), ID);
    }

    /* == Projectile Hit == */

    @Override
    public void onProjectileHit(WeaponProjectileContext ctx) {
        if (!(ctx.hitEntity() instanceof LivingEntity target)) return;

        Player shooter = ctx.shooter();
        UUID shooterId = shooter.getUniqueId();

        if (target instanceof Player targetPlayer) {
            if (!ctx.effectsActive()) return;
            if (ctx.session() != null
                && ctx.session().isSameTeam(shooterId, targetPlayer.getUniqueId())) return;
        }

        bypassMeleeCancel.add(shooterId);
        target.damage(config.combat().damage(), shooter);
        bypassMeleeCancel.remove(shooterId);

        // Stacking poison
        int duration = config.effects().poisonDurationTicks();
        PotionEffect existing = target.getPotionEffect(PotionEffectType.POISON);
        if (existing != null) {
            duration += existing.getDuration();
            target.removePotionEffect(PotionEffectType.POISON);
        }
        target.addPotionEffect(new PotionEffect(
            PotionEffectType.POISON, duration, 0, false, false, true));

        config.sounds().hit().playFrom(shooter);
    }

    /* == Tick == */

    @Override
    public void tick(WeaponContext ctx) {
        super.tick(ctx);

        Player player = ctx.player();
        UUID id = player.getUniqueId();
        ItemStack item = ctx.item();

        // Burst tick
        if (burstShotsRemaining.containsKey(id)) {
            // Cancel if player moved
            Location burstStart = burstStartPositions.get(id);
            if (burstStart != null
                && burstStart.distanceSquared(player.getLocation()) > 0.05) {
                cancelBurst(id);
                startReload(player, item);
                return;
            }

            long now = System.currentTimeMillis();
            long lastShot = lastBurstShotTime.getOrDefault(id, 0L);
            if (now - lastShot >= config.timing().burstCooldownMs()) {
                fireSpike(player);
                lastBurstShotTime.put(id, now);

                int remaining = burstShotsRemaining.get(id) - 1;
                if (remaining <= 0) {
                    cancelBurst(id);
                    startReload(player, item);
                } else {
                    burstShotsRemaining.put(id, remaining);
                }
            }
            return;
        }

        if (!reload.isReloading(id)) {
            player.sendActionBar(
                WeaponUtils.ammoBar(ammo.getAmmo(item), config.ammo().maxAmmo()));
        }
    }

    /* == Lifecycle == */

    @Override
    public void onMatchEnd() {
        super.onMatchEnd();
        aiming.clear();
        burstShotsRemaining.clear();
        lastBurstShotTime.clear();
        burstStartPositions.clear();
    }

    @Override
    public void onPlayerLeave(UUID playerId) {
        aiming.remove(playerId);
        cancelBurst(playerId);
    }

    /* == Internals == */

    private void startBurst(Player player, ItemStack item) {
        UUID id = player.getUniqueId();
        ammo.consumeOne(item);
        burstShotsRemaining.put(id, config.combat().burstCount());
        lastBurstShotTime.put(id, 0L); // fire immediately on first tick
        burstStartPositions.put(id, player.getLocation().clone());
    }

    private void fireSpike(Player player) {
        Snowball projectile = player.launchProjectile(Snowball.class);
        projectile.setVelocity(projectile.getVelocity().multiply(config.physics().launchSpeed()));

        double spread = 0.08;
        Vector vel = projectile.getVelocity();
        vel.add(new Vector(
            (Math.random() - 0.5) * spread,
            (Math.random() - 0.5) * spread,
            (Math.random() - 0.5) * spread
        ));
        projectile.setVelocity(vel);

        ItemStack visual = provider.getFrameItem(ID, "projectile");
        if (visual != null) projectile.setItem(visual);

        projectileTracker.track(projectile.getUniqueId(), ID, player.getUniqueId());
        config.sounds().fire().playFrom(player);
    }

    private void cancelBurst(UUID id) {
        burstShotsRemaining.remove(id);
        lastBurstShotTime.remove(id);
        burstStartPositions.remove(id);
    }

}
