package com.creatorsplash.oxygenheist.platform.paper.display;

import com.creatorsplash.oxygenheist.application.match.MatchLifecycle;
import com.creatorsplash.oxygenheist.application.match.combat.revive.ReviveSession;
import com.creatorsplash.oxygenheist.domain.match.MatchSnapshot;
import com.creatorsplash.oxygenheist.domain.match.config.DownedConfig;
import com.creatorsplash.oxygenheist.domain.player.PlayerSnapshot;
import com.creatorsplash.oxygenheist.platform.paper.config.ArenaConfigService;
import com.creatorsplash.oxygenheist.platform.paper.util.MM;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
public final class DownedDisplayManager implements MatchLifecycle {

    private static final BlockData BLEEDOUT_PARTICLE = Material.REDSTONE_BLOCK.createBlockData();

    private final Server server;
    private final ArenaConfigService arenaConfigService;

    /** downed player UUID -> label TextDisplay UUID */
    private final Map<UUID, UUID> labels = new HashMap<>();

    /* == Lifecycle == */

    @Override
    public void readGameTick(MatchSnapshot snapshot) {
        DownedConfig cfg = snapshot.config().downed();

        for (PlayerSnapshot ps : snapshot.players().values()) {
            if (!ps.downed()) {
                removeLabel(ps.playerId());
                continue;
            }

            Player player = server.getPlayer(ps.playerId());
            if (player == null || !player.isOnline()) continue;

            ensureLabel(player, cfg);
            updateLabel(player, ps, cfg);
            spawnBleedoutParticles(player, ps);
        }
    }

    @Override
    public void onPlayerLeave(UUID playerId) {
        World world = arenaConfigService.resolveWorld();
        removeLabel(playerId, world);
    }

    @Override
    public void onMatchEnd() {
        removeAll();
    }

    /* == Internals == */

    /* Label management */

    private void ensureLabel(Player player, DownedConfig cfg) {
        if (labels.containsKey(player.getUniqueId())) return;

        TextDisplay td = player.getWorld().spawn(player.getLocation(), TextDisplay.class, d -> {
           d.setBillboard(Display.Billboard.CENTER);
           d.setShadowed(true);
           d.setDefaultBackground(false);
           d.setViewRange(cfg.labelViewRange());
           d.text(Component.empty());
           d.setTransformation(new Transformation(
               new Vector3f(0f, (float) cfg.labelHeightOffset(), 0f),
               new AxisAngle4f(),
               new Vector3f(0.9f),
               new AxisAngle4f()
           ));
           d.setPersistent(false);
        });

        player.addPassenger(td);

        labels.put(player.getUniqueId(), td.getUniqueId());
    }

    private void updateLabel(
        Player player,
        PlayerSnapshot ps,
        DownedConfig cfg
    ) {
        UUID labelId = labels.get(player.getUniqueId());
        if (labelId == null) return;

        Entity entity = player.getWorld().getEntity(labelId);
        if (!(entity instanceof TextDisplay td)) return;

        Component text = ps.reviveProgressPercent() > 0
            ? buildReviveText(ps.reviveProgressPercent())
            : buildBleedoutText(ps, cfg);

        td.text(text);
    }

    private void removeLabel(UUID playerId) {
        if (!labels.containsKey(playerId)) return;

        Player player = server.getPlayer(playerId);

        World world = player != null
            ? player.getWorld()
            : arenaConfigService.resolveWorld();

        removeLabel(playerId, world);
    }

    private void removeLabel(UUID playerId, World world) {
        UUID labelId = labels.remove(playerId);
        if (labelId == null || world == null) return;

        Entity label = world.getEntity(labelId);
        if (label != null) label.remove();
    }

    private void removeAll() {
        World world = arenaConfigService.resolveWorld();

        for (var entry : labels.entrySet()) {
            Player player = server.getPlayer(entry.getKey());
            World w = player != null ? player.getWorld() : world;
            if (w == null) continue;
            Entity label = w.getEntity(entry.getValue());
            if (label != null) label.remove();
        }
        labels.clear();
    }

    /* Text Builder */

    private Component buildBleedoutText(PlayerSnapshot ps, DownedConfig cfg) {
        int bleedoutMax = cfg.bleedoutSeconds() * 20;
        int secondsLeft = Math.max(0, ps.bleedoutTicks() / 20);
        int filled = bleedoutMax > 0
            ? (int) Math.round((double) ps.bleedoutTicks() / bleedoutMax * 10)
            : 0;
        filled = Math.clamp(filled, 0, 10);

        return MM.msg(
            "<red>⚠ DOWNED\n" +
            "<red>" + "▪".repeat(filled) +
            "<dark_red>" + "▫".repeat(10 - filled) +
            " <white>" + secondsLeft + "s"
        );
    }

    private Component buildReviveText(int percent) {
        int filled = (int) Math.round(percent / 100.0 * 10);
        filled = Math.clamp(filled, 0, 10);
        return MM.msg(
            "<green>⬆ REVIVING\n" +
            "<green>" + "▪".repeat(filled) +
            "<dark_gray>" + "▫".repeat(10 - filled) +
            " <white>" + percent + "%"
        );
    }

    /* Particles */

    private void spawnBleedoutParticles(Player player, PlayerSnapshot ps) {
        if (ps.reviveProgressPercent() > 0) return;

        // Spawn every 4 ticks
        if (ps.bleedoutTicks() % 4 != 0) return;

        var loc = player.getLocation().add(0, 0.3, 0);
        for (int i = 0; i < 3; i++) {
            double ox = (Math.random() - 0.5) * 0.5;
            double oz = (Math.random() - 0.5) * 0.5;
            player.getWorld().spawnParticle(
                Particle.BLOCK,
                loc.clone().add(ox, Math.random() * 0.4, oz),
                1, 0, 0.02, 0, 0,
                BLEEDOUT_PARTICLE
            );
        }
    }

}
