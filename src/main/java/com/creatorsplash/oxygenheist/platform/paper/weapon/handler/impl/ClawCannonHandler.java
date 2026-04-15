package com.creatorsplash.oxygenheist.platform.paper.weapon.handler.impl;

import com.creatorsplash.oxygenheist.application.common.LogCenter;
import com.creatorsplash.oxygenheist.application.match.Scheduler;
import com.creatorsplash.oxygenheist.application.match.combat.weapon.WeaponCooldownService;
import com.creatorsplash.oxygenheist.domain.match.MatchSession;
import com.creatorsplash.oxygenheist.platform.paper.OxygenHeistPlugin;
import com.creatorsplash.oxygenheist.platform.paper.config.weapon.WeaponTypeConfig;
import com.creatorsplash.oxygenheist.platform.paper.util.MM;
import com.creatorsplash.oxygenheist.platform.paper.util.ParticleUtils;
import com.creatorsplash.oxygenheist.platform.paper.weapon.WeaponContext;
import com.creatorsplash.oxygenheist.platform.paper.weapon.handler.AbstractWeaponHandler;
import com.creatorsplash.oxygenheist.platform.paper.weapon.provider.WeaponItemProvider;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Claw Cannon | right-click to rocket-launch yourself in the facing direction
 * <p>
 * On landing, deal falloff AoE damage to nearby entities.
 * Melee hit applies heavy knockback
 */
public final class ClawCannonHandler extends AbstractWeaponHandler {

    private static final String ID = "claw_cannon";

    private final LogCenter log;
    private final Scheduler scheduler;
    private final WeaponCooldownService cooldown = new WeaponCooldownService();

    private final Set<UUID> inFlight = new HashSet<>();
    private final Set<UUID> bypassMeleeCancel = new HashSet<>();

    public ClawCannonHandler(
        Scheduler scheduler,
        WeaponTypeConfig config,
        WeaponItemProvider provider
    ) {
        super(config, provider);
        this.scheduler = scheduler;
        this.log = OxygenHeistPlugin.log();
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
    public void onRightClick(WeaponContext ctx) {
        if (!ctx.effectsActive()) return;
        Player player = ctx.player();
        UUID id = player.getUniqueId();

        if (cooldown.isOnCooldown(id, config.timing().cooldownMs())) {
            long remaining = (cooldown.remainingMillis(id, config.timing().cooldownMs()) / 1000) + 1;
            player.sendActionBar(MM.msg("<red>Claw Cannon cooldown: " + remaining + "s"));
            return;
        }

        launchPlayer(player, ctx, ctx.session());
        cooldown.recordUse(id);
    }

    @Override
    public void onMeleeHit(WeaponContext ctx, Entity victim) {
        if (!ctx.effectsActive()) return;
        if (!(victim instanceof LivingEntity le)) return;

        Player player = ctx.player();

        // Direction from attacker -> victim for knockback
        var direction = victim.getLocation().toVector()
            .subtract(player.getLocation().toVector())
            .normalize();
        var knockback = direction.multiply(config.physics().meleeKnockback());
        knockback.setY(config.physics().meleeKnockbackY());

        // Apply on next tick so vanilla hit processing completes first
        scheduler.runLater(() -> le.setVelocity(le.getVelocity().add(knockback)), 1L);

        ParticleUtils.spawn(Particle.CRIT, victim.getLocation().add(0, 1, 0),
            15, 0.3, 0.5, 0.3, 0.1, ctx.session());

        var meleeSound = config.sounds().get("melee");
        if (!meleeSound.isEmpty()) meleeSound.playFrom(player);

        player.sendActionBar(MM.msg("<gold><bold>CRITICAL HIT!"));
    }

    /* == Launch == */

    private void launchPlayer(Player player, WeaponContext ctx, @Nullable MatchSession session) {
        UUID id = player.getUniqueId();

        var direction = player.getLocation().getDirection();
        direction.setY(0).normalize();

        var velocity = direction.multiply(config.physics().launchSpeed());
        velocity.setY(config.physics().launchY());
        player.setVelocity(velocity);

        inFlight.add(id);

        if (!config.sounds().fire().isEmpty()) config.sounds().fire().playFrom(player);

        ParticleUtils.spawn(Particle.EXPLOSION, player.getLocation(),
            3, 0.3, 0.3, 0.3, 0.1, session);
        ParticleUtils.spawn(Particle.CLOUD, player.getLocation(),
            20, 0.3, 0.3, 0.3, 0.1, session);

        provider.applyFrame(player.getInventory().getItemInMainHand(), ID, "launch");

        player.sendActionBar(MM.msg("<gold><bold>LAUNCHED!"));

        FlightTask task = new FlightTask(player, session, ctx);
        task.handle = scheduler.runRepeating(task, 0L, 1L);
        trackPlayerTask(player.getUniqueId(), task.handle);
    }

    /* == Flight + Landing == */

    @RequiredArgsConstructor
    private final class FlightTask implements Runnable {
        private final Player player;
        private final @Nullable MatchSession session;
        private int ticks = 0;
        private Location lastLoc;
        Scheduler.Task handle;
        private final WeaponContext ctx;

        @Override
        public void run() {
            UUID id = player.getUniqueId();

            if (!player.isOnline() || !inFlight.contains(id) || ticks > 60) {
                inFlight.remove(id);
                handle.cancel();
                return;
            }

            Location currentLoc = player.getLocation();
            var trail = currentLoc.clone().add(0, 0.5, 0);
            ParticleUtils.spawn(Particle.FLAME, trail,
                8,  0.2, 0.2, 0.2, 0.02, session);
            ParticleUtils.spawn(Particle.SMOKE, trail,
                10, 0.2, 0.2, 0.2, 0.02, session);
            ParticleUtils.spawn(Particle.FIREWORK, trail,
                3,  0.15, 0.15, 0.15, 0.01, session);

            // Landing detection
            boolean dropping = lastLoc != null && currentLoc.getY() < lastLoc.getY() - 0.5;
            if (ticks > 5 && (hasLanded(player)|| dropping)) {
                explodeOnLanding(player, currentLoc, ctx, session);
                inFlight.remove(id);
                handle.cancel();
                return;
            }

            lastLoc = currentLoc.clone();
            ticks++;
        }

        private boolean hasLanded(Player player) {
            // Check for solid block within 0.5 blocks below the players feet
            Location below = player.getLocation().clone().subtract(0, 0.5, 0);
            return below.getBlock().getType().isSolid()
                && player.getVelocity().getY() <= 0.1;
        }

        private void explodeOnLanding(
            Player player,
            Location landLoc,
            WeaponContext ctx,
            @Nullable MatchSession session
        ) {
            provider.applyIdleFrame(ctx.item(), ID);

            UUID shooterId = player.getUniqueId();
            double radius = config.combat().explosionRadius();

            config.sounds().get("land").playFrom(player);

            // AoE damage
            landLoc.getNearbyPlayers(radius, target ->
                !target.getUniqueId().equals(shooterId)
                    && (session == null || !session.isSameTeam(shooterId, target.getUniqueId()))
            ).forEach(target -> {
                double distance = target.getLocation().distance(landLoc);
                double multiplier = 1.0 - (distance / radius);
                double damage = config.combat().damage() * multiplier;
                if (damage < 0.5) return;

                bypassMeleeCancel.add(shooterId);
                target.damage(damage, player);
                bypassMeleeCancel.remove(shooterId);

                var knockback = target.getLocation().toVector()
                    .subtract(landLoc.toVector())
                    .normalize()
                    .multiply(1.8 * multiplier);
                knockback.setY(0.6 * multiplier);
                target.setVelocity(target.getVelocity().add(knockback));

                ParticleUtils.spawn(Particle.CRIT, target.getLocation().add(0, 1, 0),
                    15, 0.3, 0.5, 0.3, 0.1, session);
            });

            ExplosionRingTask ring = new ExplosionRingTask(session, landLoc, radius);
            ring.handle = scheduler.runRepeating(ring, 0L, 1L);
            trackWorldTask(ring.handle);
        }

        @RequiredArgsConstructor
        private static final class ExplosionRingTask implements Runnable {
            private final @Nullable MatchSession session;
            private final Location center;
            private final double maxRadius;
            private double radius = 0;
            Scheduler.Task handle;

            @Override
            public void run() {
                if (radius > maxRadius) {
                    handle.cancel();
                    return;
                }
                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 16) {
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    ParticleUtils.spawn(Particle.FLAME, center.clone().add(x, 0.1, z),
                        1, 0, 0, 0, 0, session);
                }
                radius += 0.5;
            }
        }

    }

    /* == Lifecycle == */

    @Override
    public void onMatchEnd() {
        super.onMatchEnd();

        inFlight.clear();
        cooldown.clearAll();
        bypassMeleeCancel.clear();
    }

    @Override
    public void onPlayerLeave(UUID playerId) {
        super.onPlayerLeave(playerId);

        inFlight.remove(playerId);
        cooldown.clear(playerId);
        bypassMeleeCancel.remove(playerId);
    }

}
