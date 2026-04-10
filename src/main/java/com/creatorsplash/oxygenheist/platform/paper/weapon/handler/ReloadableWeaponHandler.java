package com.creatorsplash.oxygenheist.platform.paper.weapon.handler;


import com.creatorsplash.oxygenheist.application.match.combat.weapon.ReloadTracker;
import com.creatorsplash.oxygenheist.platform.paper.config.weapon.WeaponTypeConfig;
import com.creatorsplash.oxygenheist.platform.paper.util.MM;
import com.creatorsplash.oxygenheist.platform.paper.weapon.WeaponContext;
import com.creatorsplash.oxygenheist.platform.paper.weapon.service.WeaponAmmoService;
import com.creatorsplash.oxygenheist.platform.paper.weapon.WeaponUtils;
import com.creatorsplash.oxygenheist.platform.paper.weapon.provider.WeaponItemProvider;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Base class for weapons with ammo and a reload cycle
 */
public abstract class ReloadableWeaponHandler extends AbstractWeaponHandler {

    protected final WeaponAmmoService ammo;
    protected final ReloadTracker reload = new ReloadTracker();

    protected ReloadableWeaponHandler(
        WeaponTypeConfig config,
        WeaponItemProvider provider
    ) {
        super(config, provider);
        this.ammo = new WeaponAmmoService(config.ammo().maxAmmo());
    }

    protected void startReload(Player player, ItemStack item) {
        reload.begin(player.getUniqueId(), player.getInventory().getHeldItemSlot());
        provider.applyReloadFrame(item, id(), 0);
        player.sendActionBar(MM.msg("<red>Reloading..."));
        onReloadStart(player);
    }

    protected void cancelReload(Player player, ItemStack item) {
        reload.cancel(player.getUniqueId());
        if (item != null) provider.applyIdleFrame(item, id());
        player.sendActionBar(MM.msg("<red>Reload canceled!"));
        onReloadCancel(player);
    }

    protected void completeReload(Player player, ItemStack item) {
        ammo.setAmmo(item, config.ammo().maxAmmo());
        provider.applyIdleFrame(item, id());
        reload.cancel(player.getUniqueId());
        player.sendActionBar(MM.msg("<green>Weapon Reloaded!"));
        onReloadComplete(player);
    }

    /** Called when a reload starts - override to add sounds or effects */
    protected void onReloadStart(Player player) {
        config.sounds().reloadStart().playTo(player);
    }

    /** Called when a reload is canceled - override to add sounds or effects */
    protected void onReloadCancel(Player player) {
        config.sounds().reloadCancel().playTo(player);
    }

    /** Called when a reload completes - override to add sounds or effects */
    protected  void onReloadComplete(Player player) {
        config.sounds().reloadComplete().playTo(player);
    }

    /* == Default tick - reload animation + auto-complete == */

    @Override
    public void tick(WeaponContext ctx) {
        UUID id = ctx.player().getUniqueId();
        if (!reload.isReloading(id)) return;

        long elapsed = reload.elapsedMs(id);
        long reloadMs = config.timing().reloadMs();

        if (elapsed >= reloadMs) {
            completeReload(ctx.player(), ctx.item());
            return;
        }

        int frameIndex = WeaponUtils.calculateReloadFrameIndex(
            elapsed, reloadMs, config.reloadFrames()
        );
        provider.applyReloadFrame(ctx.item(), id(), frameIndex);
        ctx.player().sendActionBar(WeaponUtils.reloadBar((double) elapsed / reloadMs));
    }

    /* == Default slot/swap/drop behaviour == */

    @Override
    public void onSlotChange(Player player) {
        if (reload.isReloading(player.getUniqueId())) {
            ItemStack item = player.getInventory().getItemInMainHand();
            cancelReload(player, item);
        }
    }

    @Override
    public boolean preventsSwapDuringReload(Player player) {
        return reload.isReloading(player.getUniqueId());
    }

    @Override
    public boolean preventsDropDuringReload(Player player) {
        return reload.isReloading(player.getUniqueId());
    }

    /* == Lifecycle == */

    @Override
    public void onMatchEnd() {
        reload.onMatchEnd();
    }

}
