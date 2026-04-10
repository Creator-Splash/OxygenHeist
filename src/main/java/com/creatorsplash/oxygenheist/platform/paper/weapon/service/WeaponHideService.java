package com.creatorsplash.oxygenheist.platform.paper.weapon.service;

import com.creatorsplash.oxygenheist.application.match.MatchLifecycle;
import com.creatorsplash.oxygenheist.application.match.Scheduler;
import com.creatorsplash.oxygenheist.platform.paper.OxygenHeistPlugin;
import com.creatorsplash.oxygenheist.platform.paper.util.MM;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

/**
 * Temporarily hides a weapon from the players main hand and restores it after a delay
 *
 * <p>Tracks the original slot the weapon was in. On restore, the item is returned
 * to that slot directly rather than blindly setting main hand - preventing overwrites
 * if the player has switched slots during the hide window.</p>
 *
 * <p>Restore is also triggered early by {@link #restoreNow} which handlers should
 * call from {@link MatchLifecycle#onMatchEnd} and {@link MatchLifecycle#onPlayerLeave}
 * to prevent item loss</p>
 */
public final class WeaponHideService implements MatchLifecycle {

    private record HideState(ItemStack item, int slot, Scheduler.Task task) {}

    private final Scheduler scheduler;
    private final Map<UUID, HideState> hidden = new HashMap<>();

    public WeaponHideService(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    /**
     * Hides the players main hand item and schedules its return after {@code delayTicks}
     *
     * <p>If the player already has a hide in progress, it is cancelled and
     * the item is restored immediately before starting a new one.</p>
     */
    public void hide(Player player, ItemStack item, long delayTicks) {
        UUID id = player.getUniqueId();

        // Clean up any existing hide for this player first
        restoreNow(player);

        int slot = player.getInventory().getHeldItemSlot();
        player.getInventory().setItemInMainHand(null);

        Scheduler.Task task = scheduler.runLater(() -> restore(id), delayTicks);

        hidden.put(id, new HideState(item.clone(), slot, task));
    }

    /**
     * Cancels the scheduled restore and returns the item immediately
     *
     * <p>Safe to call even if no hide is in progress</p>
     */
    public void restoreNow(Player player) {
        restore(player.getUniqueId());
    }

    /* == Internals == */

    private void restore(UUID id) {
        HideState state = hidden.remove(id);
        if (state == null) return;

        state.task().cancel();

        Player player = org.bukkit.Bukkit.getPlayer(id);
        if (player == null || !player.isOnline()) return;

        ItemStack current = player.getInventory().getItem(state.slot());

        if (current == null || current.isEmpty()) {
            // Slot is free - restore directly
            player.getInventory().setItem(state.slot(), state.item());
            return;
        }

        if (current.isSimilar(state.item())) return;

        int freeSlot = player.getInventory().firstEmpty();
        if (freeSlot != -1) {
            player.getInventory().setItem(freeSlot, current);
        } else {
            // Inventory completely full - drop the displaced item at the players feet
            player.getWorld().dropItemNaturally(player.getLocation(), current);
            player.sendActionBar(MM.msg("<yellow>Your inventory was full - an item was dropped"));
        }

        player.getInventory().setItem(state.slot(), state.item());
    }

    /* == Lifecycle == */

    @Override
    public void onMatchEnd() {
        // Restore all hidden items - match cleanup removes items from players
        new HashSet<>(hidden.keySet()).forEach(id -> {
            Player player = org.bukkit.Bukkit.getPlayer(id);
            if (player != null) restore(id);
            else hidden.remove(id);
        });
    }

    @Override
    public void onPlayerLeave(UUID playerId) {
        // Item is lost on disconnect regardless - just clean up state
        HideState state = hidden.remove(playerId);
        if (state != null) state.task().cancel();
    }

}
