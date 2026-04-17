package com.creatorsplash.oxygenheist.platform.paper.listener;

import com.creatorsplash.oxygenheist.application.bridge.display.MatchDisplayService;
import com.creatorsplash.oxygenheist.application.match.MatchService;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.UUID;

@RequiredArgsConstructor
public final class MatchJoinListener implements Listener {

    private final MatchService matchService;
    private final MatchDisplayService displayService;

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (matchService.getSession().isEmpty()) return;
        displayService.showBarsToNewPlayer(event.getPlayer().getUniqueId());

        event.getPlayer().removeResourcePacks();
        addResourcePack(
            event.getPlayer(),
            "c21f48fcef6f3d9d78faa0385f109c2bd4fa896f",
            "763f53d1-8100-3feb-bdc3-307c6e228b75"
        );
    }

    private void addResourcePack(Player player, String shaHash, String id) {
        player.addResourcePack(
            UUID.fromString(id),
            "https://download.mc-packs.net/pack/" + shaHash + ".zip",
            hexToBytes(shaHash),
            "Adding Resource Pack",
            true
        );
    }

    private byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

}
