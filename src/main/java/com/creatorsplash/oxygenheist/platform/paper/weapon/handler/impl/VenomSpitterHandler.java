package com.creatorsplash.oxygenheist.platform.paper.weapon.handler.impl;

import com.creatorsplash.oxygenheist.domain.match.MatchSession;
import com.creatorsplash.oxygenheist.platform.paper.config.weapon.WeaponTypeConfig;
import com.creatorsplash.oxygenheist.platform.paper.util.MM;
import com.creatorsplash.oxygenheist.platform.paper.util.ParticleUtils;
import com.creatorsplash.oxygenheist.platform.paper.weapon.handler.ReloadableWeaponHandler;
import com.creatorsplash.oxygenheist.platform.paper.weapon.provider.WeaponItemProvider;
import com.creatorsplash.oxygenheist.platform.paper.weapon.WeaponContext;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Venom Spitter - continuous-fire raycast weapon
 * <p>
 * Left-click or melee to begin firing; each tick drains 1 ammo and deals
 * damage + stacking poison to any hit entity along the ray
 * <p>Sneak to enter aim mode. Right-click to manually reload</p>
 */
public final class VenomSpitterHandler extends ReloadableWeaponHandler {

    public static final String ID = "venom_spitter";

    /** Players currently in continuous-fire mode */
    private final Set<UUID> shooting = new HashSet<>();
    /** Bypass set for programmatic damage calls */
    private final Set<UUID> bypassMeleeCancel = new HashSet<>();

    public VenomSpitterHandler(
        @NotNull WeaponTypeConfig config,
        @NotNull WeaponItemProvider provider
    ) {
        super(config, provider);
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
        if (reload.isReloading(id)) return;
        if (ammo.getAmmo(ctx.item()) >= config.ammo().maxAmmo()) return;
        startReload(player, ctx.item());
    }

    @Override
    public void onMeleeHit(WeaponContext ctx, Entity victim) {
        if (bypassMeleeCancel.contains(ctx.player().getUniqueId())) return;
        if (!ctx.effectsActive()) return;
        UUID id = ctx.player().getUniqueId();
        if (reload.isReloading(id)) return;
        if (ammo.getAmmo(ctx.item()) > 0) {
            shooting.add(id);
        } else {
            ctx.player().sendActionBar(MM.msg("<red>No ammo!"));
        }
    }

    @Override
    public void onRightClick(WeaponContext ctx) {
        if (!ctx.effectsActive()) return;
        if (!canFire(ctx.player(), ctx.item())) return;
        shooting.add(ctx.player().getUniqueId());
    }

    @Override
    public void onSneakToggle(WeaponContext ctx, boolean sneaking) {
        if (sneaking) {
            provider.applyFrame(ctx.item(), ID, "charged");
        } else {
            shooting.remove(ctx.player().getUniqueId());
            provider.applyIdleFrame(ctx.item(), ID);
        }
    }

    @Override
    public boolean suppressSneakAnimation() { return true; }

    @Override
    public void onSlotChange(Player player) {
        super.onSlotChange(player);
        shooting.remove(player.getUniqueId());
        // Restore idle - player may have switched away while charged
        ItemStack item = player.getInventory().getItemInMainHand();
        provider.applyIdleFrame(item, ID);
    }

    /* == Tick == */

    @Override
    public void tick(WeaponContext ctx) {
        super.tick(ctx);

        Player player = ctx.player();
        ItemStack item = ctx.item();

        UUID id = player.getUniqueId();
        if (!shooting.contains(id)) return;

        int currentAmmo = ammo.getAmmo(item);
        if (currentAmmo <= 0) {
            shooting.remove(id);
            startReload(player, item);
            return;
        }

        shoot(player, ctx.session());
        ammo.setAmmo(item, currentAmmo - 1);
        player.sendActionBar(MM.msg("<yellow>Ammo: <white>" + (currentAmmo - 1) + "/" + config.ammo().maxAmmo()));
    }

    private void shoot(Player player, @Nullable MatchSession session) {
        Location start = player.getEyeLocation();
        var dir = start.getDirection().normalize();
        double maxRange = config.combat().maxRange();
        UUID shooterId = player.getUniqueId();

        Set<UUID> hitThisTick = new HashSet<>();

        for (double i = 1.0; i <= maxRange; i += 1.0) {
            Location loc = start.clone().add(dir.clone().multiply(i));

            ParticleUtils.spawn(
                Particle.DUST, loc, 4, 0, 0, 0, 0,
                new Particle.DustOptions(Color.GREEN, 1.2f),
                session
            );

            loc.getNearbyPlayers(1.5, 2.0, 1.5, target -> {
                if (target.equals(player)) return false;
                if (hitThisTick.contains(target.getUniqueId())) return false;
                if (session != null && session.isSameTeam(shooterId, target.getUniqueId())) return false;
                return true;
            }).forEach(target -> {
                hitThisTick.add(target.getUniqueId());

                bypassMeleeCancel.add(shooterId);
                target.damage(config.combat().damagePerShot(), player);
                bypassMeleeCancel.remove(shooterId);

                int addedDuration = config.effects().poisonDurationTicks();
                PotionEffect current = target.getPotionEffect(PotionEffectType.POISON);
                int newDuration = addedDuration + (current != null ? current.getDuration() : 0);
                if (current != null) target.removePotionEffect(PotionEffectType.POISON);
                target.addPotionEffect(
                    new PotionEffect(PotionEffectType.POISON, newDuration,
                        0, false, false, true));
            });
        }

        config.sounds().fire().playFrom(player);
    }

    /* == Lifecycle == */

    @Override
    public void cleanUp() {
        shooting.clear();
        bypassMeleeCancel.clear();
        super.cleanUp();
    }

    @Override
    public void onPlayerLeave(UUID playerId) {
        super.onPlayerLeave(playerId);
        shooting.remove(playerId);
        bypassMeleeCancel.remove(playerId);
    }

}
