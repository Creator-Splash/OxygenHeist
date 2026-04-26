package com.creatorsplash.oxygenheist.platform.paper.listener;

import com.creatorsplash.oxygenheist.application.bridge.display.MatchDisplayService;
import com.creatorsplash.oxygenheist.application.match.MatchService;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

@RequiredArgsConstructor
public final class MatchJoinListener implements Listener {

    private final MatchService matchService;
    private final MatchDisplayService displayService;

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (matchService.getSession().isEmpty()) return;
        displayService.showBarsToNewPlayer(event.getPlayer().getUniqueId());
    }

}
