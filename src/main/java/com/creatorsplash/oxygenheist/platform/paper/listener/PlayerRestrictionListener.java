package com.creatorsplash.oxygenheist.platform.paper.listener;

import com.creatorsplash.oxygenheist.application.match.combat.PlayerActionService;
import com.creatorsplash.oxygenheist.platform.paper.util.TeamUtils;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Bukkit listener responsible for enforcing player action restrictions
 */
@RequiredArgsConstructor
public final class PlayerRestrictionListener implements Listener {

    private final PlayerActionService actionService;

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!actionService.canInteract(player.getUniqueId())) return;

        if (TeamUtils.isTeamArmor(event.getCurrentItem()) || TeamUtils.isTeamArmor(event.getCursor())) {
            event.setCancelled(true);
            return;
        }

        if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY &&
            TeamUtils.isTeamArmor(event.getCurrentItem())
        ) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!actionService.canInteract(player.getUniqueId())) return;
        if (TeamUtils.isTeamArmor(event.getOldCursor())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent event) {
        if (!actionService.canInteract(event.getPlayer().getUniqueId())) return;
        if (TeamUtils.isTeamArmor(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    /**
     * Prevents players from interacting with the world if they are restricted
     */
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!actionService.canInteract(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    /**
     * Prevents players from breaking blocks if they are restricted
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        if (!actionService.canInteract(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    /**
     * Prevents players from placing blocks if they are restricted
     */
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        if (!actionService.canInteract(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

}
