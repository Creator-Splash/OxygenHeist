package com.creatorsplash.oxygenheist.platform.paper.display;

import com.creatorsplash.oxygenheist.application.common.LogCenter;
import com.creatorsplash.oxygenheist.application.match.MatchLifecycle;
import com.creatorsplash.oxygenheist.application.match.MatchSnapshotProvider;
import com.creatorsplash.oxygenheist.application.match.Scheduler;
import com.creatorsplash.oxygenheist.domain.match.MatchSession;
import com.creatorsplash.oxygenheist.domain.player.PlayerMatchState;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
public final class DownedDisplayManager implements MatchLifecycle {

    // TODO cfg?
    private static final float VIEW_RANGE = 16f;
    private static final double LABEL_HEIGHT = 2.4;
    private static final Color BLEEDOUT_PARTICLE_COLOR = Color.RED;

    private final Server server;
    private final MatchSnapshotProvider snapshotProvider;
    private final Scheduler scheduler;
    private final LogCenter log;

    /** downed player UUID -> label TextDisplay UUID */
    private final Map<UUID, UUID> labels = new HashMap<>();

    /* == Lifecycle == */

    @Override
    public void onGameTick(MatchSession session) {
        for (PlayerMatchState ps : session.getPlayers()) {
            if (!ps.isDowned()) {
                // remove label
                continue;
            }

            Player player = server.getPlayer(ps.getPlayerId());
            if (player == null || !player.isOnline()) continue;

            ensureLabel(player);

            UUID labelId = labels.get(ps.getPlayerId());
            if (labelId == null) continue;

            Entity labelEntity = player.getWorld().getEntity(labelId);

            // add as passenger?

            if (labelEntity instanceof TextDisplay td) {
                // build text
            }

            Location loc = player.getLocation().add(0, 0.5, 0);
            // todo revive progress
        }
    }

    @Override
    public void onPlayerLeave(UUID playerId) {
        removeLabel(playerId);
    }

    @Override
    public void onMatchEnd() {
        removeAll();
    }

    /* == Internals == */

    /* Label management */

    private void ensureLabel(Player player) {
        if (labels.containsKey(player.getUniqueId())) return;

        TextDisplay td = player.getWorld().spawn(player.getLocation(), TextDisplay.class, d -> {
           d.setBillboard(Display.Billboard.CENTER);
           d.setShadowed(true);
           d.setDefaultBackground(false);
           d.setViewRange(VIEW_RANGE);
           d.text(Component.empty());
           d.setPersistent(false);
        });

        labels.put(player.getUniqueId(), td.getUniqueId());
    }

    private void removeLabel(UUID playerId) {
        UUID labelId = labels.remove(playerId);
        if (labelId == null) return;

        Player player = server.getPlayer(playerId);
        if (player == null) return;

        Entity label = player.getWorld().getEntity(labelId);
        if (label != null) label.remove();
    }

    private void removeAll() {
        for (var entry : labels.entrySet()) {
            UUID playerId = entry.getKey();
            Player player = server.getPlayer(playerId);
            if (player == null) continue;

            UUID labelId = entry.getValue();
            Entity label = player.getWorld().getEntity(labelId);
            if (label != null) label.remove();
        }
        labels.clear();
    }

    /* Text Builder */

    private Component buildText(PlayerMatchState ps) {
        // TODO
    }

    private Component buildBar(int percent, String color, String label) {

    }

}
