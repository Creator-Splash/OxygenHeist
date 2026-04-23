package com.creatorsplash.oxygenheist.platform.paper.weapon.handler.impl;

import com.creatorsplash.oxygenheist.application.common.task.Scheduler;
import com.creatorsplash.oxygenheist.domain.match.MatchSession;
import com.creatorsplash.oxygenheist.platform.paper.config.weapon.WeaponTypeConfig;
import com.creatorsplash.oxygenheist.platform.paper.util.ParticleUtils;
import com.creatorsplash.oxygenheist.platform.paper.weapon.WeaponContext;
import com.creatorsplash.oxygenheist.platform.paper.weapon.WeaponUtils;
import com.creatorsplash.oxygenheist.platform.paper.weapon.handler.ReloadableWeaponHandler;
import com.creatorsplash.oxygenheist.platform.paper.weapon.provider.WeaponItemProvider;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Manta Ray - electric blast weapon
 *
 * <p>Left-click or melee to fire an electric blast. The blast raycasts forward,
 * finds an impact point, then expands a cone of electric particles that damages
 * and slows any entity caught in it. Sneak to enter aim mode</p>
 *
 * <p>If the blast hits a solid block, the cone reverses direction to simulate
 * the electricity reflecting back toward the shooter</p>
 */
public final class MantaRayHandler extends ReloadableWeaponHandler {

    public static final String ID = "manta_ray";

    private final Scheduler scheduler;

    private final Set<UUID> aiming = new HashSet<>();
    private final Map<UUID, Long> shotCooldowns = new HashMap<>();
    private final Set<UUID> bypassMeleeCancel = new HashSet<>();

    public MantaRayHandler(
        @NotNull WeaponTypeConfig config,
        @NotNull WeaponItemProvider provider,
        @NotNull Scheduler scheduler
    ) {
        super(config, provider);
        this.scheduler = scheduler;
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
        if (!canFire(ctx.player(), ctx.item())) return;
        if (isOnCooldown(ctx.player().getUniqueId())) return;
        fire(ctx.player(), ctx.item(), ctx);
    }

    @Override
    public void onMeleeHit(WeaponContext ctx, Entity victim) {
        if (bypassMeleeCancel.contains(ctx.player().getUniqueId())) return;
        if (!ctx.effectsActive()) return;
        if (!canFire(ctx.player(), ctx.item())) return;
        if (isOnCooldown(ctx.player().getUniqueId())) return;
        fire(ctx.player(), ctx.item(), ctx);
    }

    @Override
    public void onSneakToggle(WeaponContext ctx, boolean sneaking) {
        UUID id = ctx.player().getUniqueId();
        if (sneaking) {
            aiming.add(id);
            provider.applyFrame(ctx.item(), ID, "charged");
        } else {
            aiming.remove(id);
            provider.applyIdleFrame(ctx.item(), ID);
        }
    }

    @Override
    public boolean suppressSneakAnimation() { return true; }

    @Override
    public void onSlotChange(Player player) {
        super.onSlotChange(player);
        aiming.remove(player.getUniqueId());
        provider.applyIdleFrame(player.getInventory().getItemInMainHand(), ID);
    }

    /* == Tick == */

    @Override
    public void tick(WeaponContext ctx) {
        super.tick(ctx);
        if (!reload.isReloading(ctx.player().getUniqueId())) {
            ctx.player().sendActionBar(
                WeaponUtils.ammoBar(ammo.getAmmo(ctx.item()), config.ammo().maxAmmo()));
        }
    }

    /* == Lifecycle == */

    @Override
    public void onMatchEnd() {
        super.onMatchEnd();
        aiming.clear();
        shotCooldowns.clear();
    }

    @Override
    public void onPlayerLeave(UUID playerId) {
        aiming.remove(playerId);
        shotCooldowns.remove(playerId);
    }

    /* == Internals == */

    private void fire(Player player, ItemStack item, WeaponContext ctx) {
        fireElectricBlast(player, ctx.session(), ctx.effectsActive());

        ammo.consumeOne(item);
        shotCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        config.sounds().fire().playFrom(player);

        if (!ammo.hasAmmo(item)) {
            scheduler.runLater(() -> {
                if (player.isOnline()) startReload(player, player.getInventory().getItemInMainHand());
            }, 20L);
        }
    }

    private void fireElectricBlast(Player player, @Nullable MatchSession session, boolean effectsActive) {
        Location eyeLoc = player.getEyeLocation();
        Vector direction = eyeLoc.getDirection().normalize();
        World world = player.getWorld();
        double maxRange = config.combat().maxRange();

        // Raycast to find impact point
        RayTraceResult rayResult = world.rayTraceBlocks(
            eyeLoc, direction, maxRange, FluidCollisionMode.NEVER, true);

        Location impactPoint;
        boolean reverseCone;

        if (rayResult != null && rayResult.getHitBlock() != null) {
            impactPoint = rayResult.getHitPosition().toLocation(world);
            Material hit = rayResult.getHitBlock().getType();
            reverseCone = hit.name().contains("CONCRETE") || hit.name().contains("MUD");
        } else {
            impactPoint = eyeLoc.clone().add(direction.clone().multiply(maxRange));
            reverseCone = false;
        }

        final Location finalImpact = impactPoint;
        final double finalDistance = eyeLoc.distance(finalImpact);
        final Vector coneDir = reverseCone
            ? direction.clone().multiply(-1)
            : direction.clone();
        final Vector perp = getPerpendicularVector(coneDir);
        final Vector cross = perp.clone().crossProduct(coneDir);
        final Set<UUID> hitEntities = new HashSet<>();

        scheduler.runRepeating(task -> {
            int t = task.elapsedTicks();

            // Phase 1 (ticks 0-4): beam travels from shooter to impact
            if (t <= 4) {
                double progress = (t / 4.0) * finalDistance;
                Vector dir = eyeLoc.getDirection().normalize();
                for (double dist = 0; dist <= progress; dist += 0.4) {
                    Location beamLoc = eyeLoc.clone().add(dir.clone().multiply(dist));
                    ParticleUtils.spawn(Particle.ELECTRIC_SPARK, beamLoc,
                        2, 0.05, 0.05, 0.05, 0.01, session);
                    ParticleUtils.spawn(Particle.DUST, beamLoc,
                        1, 0.02, 0.02, 0.02, 0,
                        new Particle.DustOptions(Color.AQUA, 1.8f), session);
                    if (Math.random() < 0.3) {
                        ParticleUtils.spawn(Particle.DUST, beamLoc,
                            1, 0.02, 0.02, 0.02, 0,
                            new Particle.DustOptions(Color.WHITE, 2.2f), session);
                    }
                }
            }

            // Phase 2 (ticks 5-11): cone expands from impact point
            if (t >= 5) {
                int conePhase = t - 5;

                if (conePhase == 0) {
                    ParticleUtils.spawn(Particle.FLASH, finalImpact, 2, 0, 0, 0, 0, session);
                    ParticleUtils.spawn(Particle.ELECTRIC_SPARK, finalImpact,
                        40, 0.5, 0.5, 0.5, 0.2, session);
                    config.sounds().get("impact").playFrom(finalImpact);
                }

                double coneProgress = conePhase / 6.0;
                double halfAngle = Math.toRadians(config.physics().coneAngle() / 2.0);

                for (int ray = 0; ray < 16; ray++) {
                    double angleOffset = (ray / 16.0) * Math.PI * 2;

                    for (double coneDist = 0.5; coneDist <= 6.0 * coneProgress; coneDist += 0.4) {
                        double coneRadius = Math.tan(halfAngle) * coneDist;
                        double rx = Math.cos(angleOffset) * coneRadius * (0.5 + Math.random() * 0.5);
                        double ry = Math.sin(angleOffset) * coneRadius * (0.5 + Math.random() * 0.5);

                        Vector offset = perp.clone().multiply(rx).add(cross.clone().multiply(ry));
                        Location coneLoc = finalImpact.clone()
                            .add(coneDir.clone().multiply(coneDist))
                            .add(offset);

                        if (Math.random() < 0.5) {
                            ParticleUtils.spawn(Particle.ELECTRIC_SPARK, coneLoc,
                                1, 0.15, 0.15, 0.15, 0.03, session);
                        }
                        if (Math.random() < 0.4) {
                            ParticleUtils.spawn(Particle.DUST, coneLoc,
                                1, 0.08, 0.08, 0.08, 0,
                                new Particle.DustOptions(Color.AQUA, 1.5f), session);
                        }

                        for (Entity entity : world.getNearbyEntities(coneLoc, 1.8, 1.8, 1.8)) {
                            if (!(entity instanceof LivingEntity le)) continue;
                            if (le.equals(player)) continue;
                            if (hitEntities.contains(le.getUniqueId())) continue;
                            if (entity instanceof Player targetPlayer) {
                                if (!effectsActive) continue;
                                if (session != null && session.isSameTeam(
                                    player.getUniqueId(), targetPlayer.getUniqueId())) continue;
                            }

                            hitEntities.add(le.getUniqueId());

                            bypassMeleeCancel.add(player.getUniqueId());
                            le.damage(config.combat().damage(), player);
                            bypassMeleeCancel.remove(player.getUniqueId());

                            le.addPotionEffect(new PotionEffect(
                                PotionEffectType.SLOWNESS,
                                config.effects().effectDurationTicks(),
                                1, false, false, true));

                            Location hitLoc = le.getLocation().add(0, 1, 0);
                            ParticleUtils.spawn(Particle.DAMAGE_INDICATOR, hitLoc,
                                8, 0.3, 0.4, 0.3, 0, session);
                            ParticleUtils.spawn(Particle.ELECTRIC_SPARK, hitLoc,
                                20, 0.3, 0.5, 0.3, 0.1, session);
                            ParticleUtils.spawn(Particle.FLASH, hitLoc, 1, 0, 0, 0, 0, session);
                            config.sounds().hit().playFrom(hitLoc);
                        }
                    }
                }
            }

        }, 0L, 1L, 12);
    }

    private boolean isOnCooldown(UUID id) {
        Long last = shotCooldowns.get(id);
        return last != null && System.currentTimeMillis() - last < config.timing().shotCooldownMs();
    }

    private static Vector getPerpendicularVector(Vector v) {
        if (Math.abs(v.getX()) > 0.1) return new Vector(-v.getY(), v.getX(), 0).normalize();
        return new Vector(0, -v.getZ(), v.getY()).normalize();
    }

}
