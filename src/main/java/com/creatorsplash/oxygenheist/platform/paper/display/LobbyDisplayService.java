package com.creatorsplash.oxygenheist.platform.paper.display;

import com.creatorsplash.oxygenheist.platform.paper.config.message.MessageConfigService;
import com.creatorsplash.oxygenheist.platform.paper.util.MM;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the per-player waiting boss bar shown to players not yet assigned to a team
 *
 * <p>Shown on join if the player has no team. Hidden when they are added to a team
 * or when they leave</p>
 */
@RequiredArgsConstructor
public final class LobbyDisplayService {

    private final MessageConfigService messageConfigService;

    private final Map<UUID, BossBar> waitingBars = new HashMap<>();

    public void showWaitingBar(Player player) {
        if (waitingBars.containsKey(player.getUniqueId())) return;

        BossBar bar = BossBar.bossBar(
            MM.msg(messageConfigService.get().ui().waitingBar()),
            1f,
            BossBar.Color.YELLOW,
            BossBar.Overlay.PROGRESS
        );

        player.showBossBar(bar);
        waitingBars.put(player.getUniqueId(), bar);
    }

    public void hideWaitingBar(Player player) {
        BossBar bar = waitingBars.remove(player.getUniqueId());
        if (bar != null) player.hideBossBar(bar);
    }

    public void hideWaitingBar(UUID playerId) {
        // Called when player is offline (e.g. team command run by admin)
        waitingBars.remove(playerId);
    }

}
