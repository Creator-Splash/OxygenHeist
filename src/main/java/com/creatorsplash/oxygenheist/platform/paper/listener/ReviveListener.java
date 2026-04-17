package com.creatorsplash.oxygenheist.platform.paper.listener;

import com.creatorsplash.oxygenheist.application.match.MatchService;
import com.creatorsplash.oxygenheist.application.match.combat.PlayerActionService;
import com.creatorsplash.oxygenheist.application.match.combat.revive.ReviveService;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.UUID;

/**
 * Bukkit listener for revive interactions
 *
 * <p>This listener detects player interactions and forwards them into the
 * revive system via the match service</p>
 */
@RequiredArgsConstructor
public final class ReviveListener implements Listener {

    private final MatchService matchService;
    private final ReviveService reviveService;
    private final PlayerActionService actionService;

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Player target)) return;

        Player reviver = event.getPlayer();
        UUID reviverId = reviver.getUniqueId();

        if (!actionService.canInteract(reviverId)) {
            event.setCancelled(true);
            return;
        }

        matchService.getSession().ifPresent(session ->
            reviveService.startOrRefreshRevive(
                session,
                reviverId,
                target.getUniqueId()
            )
        );
    }

}
