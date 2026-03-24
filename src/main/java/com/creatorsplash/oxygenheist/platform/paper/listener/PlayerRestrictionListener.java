package com.creatorsplash.oxygenheist.platform.paper.listener;

import com.creatorsplash.oxygenheist.application.match.combat.PlayerActionService;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Bukkit listener responsible for enforcing player action restrictions
 */
@RequiredArgsConstructor
public final class PlayerRestrictionListener implements Listener {

    private final PlayerActionService actionService;

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
