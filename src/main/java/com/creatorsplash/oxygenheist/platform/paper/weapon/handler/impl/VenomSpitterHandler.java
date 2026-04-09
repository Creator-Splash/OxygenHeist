package com.creatorsplash.oxygenheist.platform.paper.weapon.handler.impl;

import com.creatorsplash.oxygenheist.application.match.MatchService;
import com.creatorsplash.oxygenheist.platform.paper.config.weapon.WeaponTypeConfig;
import com.creatorsplash.oxygenheist.platform.paper.util.MM;
import com.creatorsplash.oxygenheist.platform.paper.weapon.handler.ReloadableWeaponHandler;
import com.creatorsplash.oxygenheist.platform.paper.weapon.provider.WeaponItemProvider;
import com.creatorsplash.oxygenheist.platform.paper.weapon.WeaponContext;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class VenomSpitterHandler extends ReloadableWeaponHandler {

    private static final String ID = "venom_spitter";

    private final MatchService matchService;

    /** Players currently in continuous-fire mode */
    private final Set<UUID> shooting = new HashSet<>();
    /** Bypass set for programmatic damage calls */
    private final Set<UUID> bypassMeleeCancel = new HashSet<>();

    public VenomSpitterHandler(
        @NotNull WeaponTypeConfig config,
        @NotNull WeaponItemProvider provider,
        @NotNull MatchService matchService
    ) {
        super(config, provider);
        this.matchService = matchService;
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
        if (sneaking) {
            provider.applyAimFrame(ctx.item(), ID);
        } else {
            shooting.remove(ctx.player().getUniqueId());
            provider.applyBaseFrame(ctx.item(), ID);
        }
    }

    /* == Tick == */

    /* == Lifecycle == */

    @Override
    public void onMatchEnd() {
        super.onMatchEnd(); // clears reload tracker
        shooting.clear();
        bypassMeleeCancel.clear();
    }

    @Override
    public void onPlayerLeave(UUID playerId) {
        super.onPlayerLeave(playerId);
        shooting.remove(playerId);
        bypassMeleeCancel.remove(playerId);
    }

    /* == Reload Hooks == */

    @Override
    protected void onReloadStart(Player player) {
        // sound
    }

    @Override
    protected void onReloadComplete(Player player) {
        // sound
    }

}
