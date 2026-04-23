package com.creatorsplash.oxygenheist.platform.paper.weapon.handler.impl;

import com.creatorsplash.oxygenheist.platform.paper.config.weapon.WeaponTypeConfig;
import com.creatorsplash.oxygenheist.platform.paper.weapon.WeaponContext;
import com.creatorsplash.oxygenheist.platform.paper.weapon.WeaponUtils;
import com.creatorsplash.oxygenheist.platform.paper.weapon.handler.ReloadableWeaponHandler;
import com.creatorsplash.oxygenheist.platform.paper.weapon.provider.WeaponItemProvider;
import com.creatorsplash.oxygenheist.platform.paper.weapon.service.WeaponProjectileContext;
import com.creatorsplash.oxygenheist.platform.paper.weapon.service.WeaponProjectileTracker;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Reef Harpoon Gun - single-shot high-damage projectile weapon
 *
 * <p>Sneak to enter aim mode. Left-click while aiming to fire a harpoon
 * that deals heavy damage and applies slowness. Automatically reloads
 * after each shot. Firing applies recoil to the shooter</p>
 */
public final class ReefHarpoonGunHandler extends ReloadableWeaponHandler {

    public static final String ID = "reef_harpoon_gun";

    private final WeaponProjectileTracker projectileTracker;
    private final Set<UUID> aiming = new HashSet<>();
    private final Set<UUID> bypassMeleeCancel = new HashSet<>();

    public ReefHarpoonGunHandler(
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
    public void onRightClick(WeaponContext ctx) {
        if (!ctx.effectsActive()) return;
        Player player = ctx.player();
        if (!canFire(player, ctx.item())) return;

        fire(player, ctx.item());
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

        target.addPotionEffect(new PotionEffect(
            PotionEffectType.SLOWNESS,
            config.effects().effectDurationTicks(),
            2, false, false, true));

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
    }

    @Override
    public void onPlayerLeave(UUID playerId) {
        aiming.remove(playerId);
    }

    /* == Internals == */

    private void fire(Player player, ItemStack item) {
        UUID id = player.getUniqueId();

        applyRecoil(player);

        Snowball projectile = player.launchProjectile(Snowball.class);
        projectile.setVelocity(projectile.getVelocity().multiply(config.physics().launchSpeed()));

        ItemStack visual = provider.getFrameItem(ID, "projectile");
        if (visual != null) projectile.setItem(visual);

        projectileTracker.track(projectile.getUniqueId(), ID, id);

        ammo.consumeOne(item);
        config.sounds().fire().playFrom(player);

        // Single-shot weapon - always auto-reloads after firing
        startReload(player, item);
    }

    private void applyRecoil(Player player) {
        double strength = config.physics().recoil();
        if (strength <= 0) return;
        Vector dir = player.getLocation().getDirection().clone().multiply(-strength);
        player.setVelocity(player.getVelocity().add(
            new Vector(dir.getX(), strength / 2.0, dir.getZ())));
    }

}
