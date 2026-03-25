package com.creatorsplash.oxygenheist.platform.paper.display;

import com.creatorsplash.oxygenheist.application.bridge.display.MatchDisplayGateway;
import com.creatorsplash.oxygenheist.domain.match.MatchSnapshot;
import com.creatorsplash.oxygenheist.platform.paper.OxygenHeistPlugin;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Paper implementation for MatchDisplayGateway
 *
 * <p>Responsible for:
 * <ul>
 *     <li>Bossbars</li>
 *     <li>Titles</li>
 *     <li>Sounds</li>
 *     <li>ActionBars</li>
 *     <li>Messages</li>
 *     <li>Other UI</li>
 * </ul>
 * </p>
 */
@RequiredArgsConstructor
public class PaperMatchDisplayGateway implements MatchDisplayGateway {

    private final OxygenHeistPlugin plugin;
    private final PaperAirBarController airBarController;

    @Override
    public void renderSnapshot(MatchSnapshot snapshot) {
        // TODO general

        // Players
        snapshot.players().forEach(playerSnapshot -> {
            Player bukkitPlayer = plugin.getServer().getPlayer(playerSnapshot.playerId());
            if (bukkitPlayer == null || !bukkitPlayer.isOnline()) return;

            double oxygen = playerSnapshot.oxygen();
            double maxOxygen = playerSnapshot.maxOxygen();

            airBarController.update(bukkitPlayer, oxygen, maxOxygen);
        });

        // TODO zones?
    }

    // TODO handle players method

    @Override
    public void removePlayer(UUID playerId) {
        Player bukkitPlayer = plugin.getServer().getPlayer(playerId);
        if (bukkitPlayer != null) {
            airBarController.remove(bukkitPlayer);
        }
    }

    @Override
    public void clearAll() {
        airBarController.clearAll();
    }

    @Override
    public void showTitle(UUID playerId, String title, String subtitle) {

    }

    @Override
    public void playSound(UUID playerId, String soundKey) {

    }
}
