package com.creatorsplash.oxygenheist.platform.paper.weapon.handler.impl;

import com.creatorsplash.oxygenheist.platform.paper.config.weapon.WeaponTypeConfig;
import com.creatorsplash.oxygenheist.platform.paper.util.MM;
import com.creatorsplash.oxygenheist.platform.paper.util.PDCKeys;
import com.creatorsplash.oxygenheist.platform.paper.util.ParticleUtils;
import com.creatorsplash.oxygenheist.platform.paper.weapon.WeaponContext;
import com.creatorsplash.oxygenheist.platform.paper.weapon.WeaponUtils;
import com.creatorsplash.oxygenheist.platform.paper.weapon.handler.ReloadableWeaponHandler;
import com.creatorsplash.oxygenheist.platform.paper.weapon.provider.WeaponItemProvider;
import com.creatorsplash.oxygenheist.platform.paper.weapon.service.WeaponProjectileContext;
import com.creatorsplash.oxygenheist.platform.paper.weapon.service.WeaponProjectileTracker;
import org.bukkit.Particle;
import org.bukkit.util.Vector;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Reclaimer Crossbow - single-shot steal weapon
 *
 * <p>Sneak to aim, left-click while aiming to fire. On hit, steals the targets
 * held weapon and adds it to the shooters inventory. Starts unloaded and must
 * be manually reloaded via right-click before first use</p>
 */
public final class ReclaimerCrossbowHandler extends ReloadableWeaponHandler {

    public static final String ID = "reclaimer_crossbow";

    private final WeaponProjectileTracker projectileTracker;
    private final Set<UUID> aiming = new HashSet<>();
    private final Map<UUID, Long> shotCooldowns = new HashMap<>();
    private final Set<UUID> bypassMeleeCancel = new HashSet<>();

    public ReclaimerCrossbowHandler(
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
        if (isOnCooldown(id)) return;

        fire(player, ctx.item());
    }

    @Override
    public void onRightClick(WeaponContext ctx) {
        if (!ctx.effectsActive()) return;
        if (reload.isReloading(ctx.player().getUniqueId())) return;
        if (ammo.getAmmo(ctx.item()) >= config.ammo().maxAmmo()) {
            ctx.player().sendActionBar(MM.msg("<yellow>Crossbow already loaded!"));
            return;
        }
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
        if (!(ctx.hitEntity() instanceof Player target)) return;

        Player shooter = ctx.shooter();
        UUID shooterId = shooter.getUniqueId();

        if (!ctx.effectsActive()) return;
        if (ctx.session() != null
                && ctx.session().isSameTeam(shooterId, target.getUniqueId())) return;

        // Damage
        bypassMeleeCancel.add(shooterId);
        target.damage(config.combat().damage(), shooter);
        bypassMeleeCancel.remove(shooterId);

        // Steal held weapon if target has one
        ItemStack heldItem = target.getInventory().getItemInMainHand();
        if (heldItem.hasItemMeta()
                && heldItem.getItemMeta().getPersistentDataContainer()
                .has(PDCKeys.WEAPON_ID, PersistentDataType.STRING)) {

            ItemStack stolen = heldItem.clone();
            target.getInventory().setItemInMainHand(null);
            shooter.getInventory().addItem(stolen);

            target.sendActionBar(MM.msg("<red><bold>YOUR WEAPON WAS STOLEN by " + shooter.getName() + "!"));
            shooter.sendActionBar(MM.msg("<green><bold>STOLEN: " + target.getName() + "'s weapon!"));

            ParticleUtils.spawn(Particle.WITCH, target.getLocation().add(0, 1, 0),
                20, 0.5, 0.5, 0.5, 0.1, ctx.session());
            config.sounds().get("steal").playFrom(target);
        }
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

    private void fire(Player player, ItemStack item) {
        UUID id = player.getUniqueId();

        Snowball projectile = player.launchProjectile(Snowball.class);
        projectile.setVelocity(projectile.getVelocity().multiply(config.physics().launchSpeed()));

        boolean isAiming = aiming.contains(id);
        double spreadMultiplier = isAiming ? config.physics().aimSpreadMultiplier() : 1.0;
        double spread = 0.02 * spreadMultiplier;
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

        // Single shot - switch to idle (unloaded appearance) after firing
        provider.applyIdleFrame(item, ID);
        aiming.remove(id);
    }

    private boolean isOnCooldown(UUID id) {
        Long last = shotCooldowns.get(id);
        return last != null && System.currentTimeMillis() - last < config.timing().shotCooldownMs();
    }

}
