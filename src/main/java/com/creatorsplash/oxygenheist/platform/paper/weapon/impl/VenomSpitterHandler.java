package com.creatorsplash.oxygenheist.platform.paper.weapon.impl;

import com.creatorsplash.oxygenheist.application.match.MatchService;
import com.creatorsplash.oxygenheist.application.match.combat.weapon.ReloadTracker;
import com.creatorsplash.oxygenheist.platform.paper.config.weapon.WeaponTypeConfig;
import com.creatorsplash.oxygenheist.platform.paper.util.MM;
import com.creatorsplash.oxygenheist.platform.paper.weapon.WeaponAmmoService;
import com.creatorsplash.oxygenheist.platform.paper.weapon.WeaponContext;
import com.creatorsplash.oxygenheist.platform.paper.weapon.WeaponHandler;
import com.creatorsplash.oxygenheist.platform.paper.weapon.WeaponUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class VenomSpitterHandler implements WeaponHandler {

    private static final String ID = "venom_spitter";
    private static final Material MATERIAL = Material.GOLDEN_HORSE_ARMOR;

    private final JavaPlugin plugin;
    private final WeaponTypeConfig config;
    private final MatchService matchService;
    private final WeaponAmmoService ammo;
    private final ReloadTracker reload = new ReloadTracker();

    /** Players currently in continuous-fire mode */
    private final Set<UUID> shooting = new HashSet<>();
    /** Bypass set for programmatic damage calls */
    private final Set<UUID> bypassMeleeCancel = new HashSet<>();

    public VenomSpitterHandler(
        @NotNull JavaPlugin plugin,
        @NotNull WeaponTypeConfig config,
        @NotNull MatchService matchService
    ) {
        this.plugin = plugin;
        this.config = config;
        this.matchService = matchService;
        this.ammo = new WeaponAmmoService(plugin, config.ammo().maxAmmo());
    }

    @Override public String id() { return ID; }

    @Override
    public boolean handles(ItemStack item) {
        return WeaponUtils.doWeaponHandle(item, ID);
    }

    @Override
    public ItemStack createItemStack() {
        return WeaponUtils.createWeaponItem(ID, MATERIAL, config);
    }

    /* Input handlers */

    @Override
    public void onLeftClick(WeaponContext ctx) {
        if (!ctx.effectsActive()) return;
        UUID id = ctx.player().getUniqueId();
        if (reload.isReloading(id)) return;
        if (ammo.getAmmo(ctx.item()) <= 0) {
            ctx.player().sendActionBar(MM.msg("<red>No ammo! Reloading..."));
            return;
        }
        shooting.add(id);
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
        Player player = ctx.player();
        UUID id = player.getUniqueId();
        if (reload.isReloading(id)) return;
        if (ammo.getAmmo(ctx.item()) >= config.ammo().maxAmmo()) return;
        startReload(player, ctx.item());
    }

    @Override
    public void onSneakToggle(WeaponContext ctx, boolean sneaking) {
        UUID id = ctx.player().getUniqueId();
        if (sneaking && WeaponUtils.getCmd(ctx.item()) == config.cmds().get("base")) {
            WeaponUtils.setCmd(ctx.item(), config.cmds().get("aim"));
        } else if (!sneaking) {
            // Stop shooting on un-sneak
            shooting.remove(id);
        }
    }

    /* Internals */

    private void startReload(Player player, ItemStack item) {
        reload.begin(player.getUniqueId(), player.getInventory().getHeldItemSlot());
        WeaponUtils.applyReloadFrame(item, 0, config);
        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 1f, 1f);
        player.sendActionBar(MM.msg("<red>Reloading..."));
    }

    private void startAutoReload(Player player, ItemStack item) {
        startReload(player, item);
    }

    private void cancelReload(Player player, ItemStack item) {
        reload.cancel(player.getUniqueId());
        if (item != null) WeaponUtils.setCmd(item, config.cmds().get("base"));
        player.sendActionBar(MM.msg("<red>Reload canceled!"));
    }

    private void completeReload(Player player, ItemStack item, UUID id) {
        ammo.setAmmo(item, config.ammo().maxAmmo());
        WeaponUtils.setCmd(item, config.cmds().get("base"));
        reload.cancel(id);
        player.sendActionBar(MM.msg("<green>Weapon Reloaded!"));
        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 0.6f, 1.3f);
    }

}
