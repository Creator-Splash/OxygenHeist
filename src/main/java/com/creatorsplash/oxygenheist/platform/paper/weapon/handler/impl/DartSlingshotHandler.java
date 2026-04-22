package com.creatorsplash.oxygenheist.platform.paper.weapon.handler.impl;

import com.creatorsplash.oxygenheist.domain.match.MatchSession;
import com.creatorsplash.oxygenheist.platform.paper.config.weapon.WeaponTypeConfig;
import com.creatorsplash.oxygenheist.platform.paper.util.ParticleUtils;
import com.creatorsplash.oxygenheist.platform.paper.weapon.WeaponContext;
import com.creatorsplash.oxygenheist.platform.paper.weapon.WeaponUtils;
import com.creatorsplash.oxygenheist.platform.paper.weapon.handler.ReloadableWeaponHandler;
import com.creatorsplash.oxygenheist.platform.paper.weapon.provider.WeaponItemProvider;
import com.creatorsplash.oxygenheist.platform.paper.weapon.service.WeaponProjectileContext;
import com.creatorsplash.oxygenheist.platform.paper.weapon.service.WeaponProjectileTracker;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Dart Slingshot - semi-auto projectile weapon
 *
 * <p>Left-click or melee to fire a poison dart. Sneak to enter aim mode for
 * tighter spread. Right-click to manually reload</p>
 *
 * <p>Darts deal direct damage and apply stacking poison on hit</p>
 */
public final class DartSlingshotHandler extends ReloadableWeaponHandler {

    public static final String ID = "dart_slingshot";

    private final WeaponProjectileTracker projectileTracker;

    private final Set<UUID> aiming = new HashSet<>();
    private final Map<UUID, Long> shotCooldowns = new HashMap<>();
    private final Set<UUID> bypassMeleeCancel = new HashSet<>();

    public DartSlingshotHandler(
        @NotNull WeaponTypeConfig config,
        @NotNull WeaponItemProvider provider,
        @NotNull WeaponProjectileTracker projectileTracker
    ) {
        super(config, provider);
        this.projectileTracker = projectileTracker;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public boolean preventsBlockBreak(Player player) {
        return true;
    }

    @Override
    public boolean skipMeleeCancel(UUID attackerId) {
        return bypassMeleeCancel.contains(attackerId);
    }

    /* == Inputs == */

    @Override
    public void onLeftClick(WeaponContext ctx) {
        if (!ctx.effectsActive()) return;
        if (!canFire(ctx.player(), ctx.item())) return;
        if (isOnCooldown(ctx.player().getUniqueId())) return;
        fire(ctx.player(), ctx.item(), ctx.session());
    }

    @Override
    public void onMeleeHit(WeaponContext ctx, Entity victim) {
        if (bypassMeleeCancel.contains(ctx.player().getUniqueId())) return;
        if (!ctx.effectsActive()) return;
        if (!canFire(ctx.player(), ctx.item())) return;
        if (isOnCooldown(ctx.player().getUniqueId())) return;
        fire(ctx.player(), ctx.item(), ctx.session());
    }

    @Override
    public void onRightClick(WeaponContext ctx) {
        if (!ctx.effectsActive()) return;
        if (reload.isReloading(ctx.player().getUniqueId())) return;
        if (ammo.getAmmo(ctx.item()) >= config.ammo().maxAmmo()) return;
        startReload(ctx.player(), ctx.item());
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
    public void onSlotChange(Player player) {
        super.onSlotChange(player);
        aiming.remove(player.getUniqueId());
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

        // Damage
        bypassMeleeCancel.add(shooterId);
        target.damage(config.combat().damage(), shooter);
        bypassMeleeCancel.remove(shooterId);

        // Stacking poison - extend duration if already poisoned
        int duration = config.effects().poisonDurationTicks();
        PotionEffect existing = target.getPotionEffect(PotionEffectType.POISON);
        if (existing != null) {
            duration += existing.getDuration();
            target.removePotionEffect(PotionEffectType.POISON);
        }
        target.addPotionEffect(new PotionEffect(
            PotionEffectType.POISON, duration, 0, false, false, true));

        Location hitLoc = target.getLocation().add(0, 1, 0);
        ParticleUtils.spawn(Particle.DAMAGE_INDICATOR, hitLoc,
            5, 0.2, 0.3, 0.2, 0, ctx.session());
        ParticleUtils.spawn(Particle.ITEM_SLIME, hitLoc,
            10, 0.3, 0.4, 0.3, 0.05, ctx.session());
        config.sounds().hit().playFrom(shooter);
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

    private void fire(Player player, ItemStack item, @Nullable MatchSession session) {
        UUID id = player.getUniqueId();
        boolean isAiming = aiming.contains(id);

        Snowball projectile = player.launchProjectile(Snowball.class);
        projectile.setVelocity(projectile.getVelocity().multiply(config.physics().launchSpeed()));

        double spreadMultiplier = isAiming ? config.physics().aimSpreadMultiplier() : 1.0;
        double spread = 0.08 * spreadMultiplier;
        Vector vel = projectile.getVelocity();
        vel.add(new Vector(
            (Math.random() - 0.5) * spread,
            (Math.random() - 0.5) * spread,
            (Math.random() - 0.5) * spread
        ));
        projectile.setVelocity(vel);

        ItemStack visual = provider.getFrameItem(ID, "projectile");
        if (visual != null) projectile.setItem(visual);

        projectileTracker.track(projectile.getUniqueId(), ID, id);

        ammo.consumeOne(item);
        shotCooldowns.put(id, System.currentTimeMillis());
        config.sounds().fire().playFrom(player);

        player.sendActionBar(WeaponUtils.ammoBar(ammo.getAmmo(item), config.ammo().maxAmmo()));

        if (!ammo.hasAmmo(item)) startReload(player, item);
    }

    private boolean isOnCooldown(UUID id) {
        Long last = shotCooldowns.get(id);
        return last != null && System.currentTimeMillis() - last < config.timing().shotCooldownMs();
    }

}
