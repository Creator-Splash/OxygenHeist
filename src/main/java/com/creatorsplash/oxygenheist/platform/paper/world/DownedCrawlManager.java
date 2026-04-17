package com.creatorsplash.oxygenheist.platform.paper.world;

import com.creatorsplash.oxygenheist.application.common.math.Position3;
import com.creatorsplash.oxygenheist.application.match.MatchLifecycle;
import com.creatorsplash.oxygenheist.application.match.Scheduler;
import com.creatorsplash.oxygenheist.platform.paper.util.LocationUtils;
import com.creatorsplash.oxygenheist.platform.paper.util.nms.DownedPacketInterceptor;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the clientside-only fake block placed one block above downed players
 * to force the crawling (SWIMMING)
 */
@RequiredArgsConstructor
public final class DownedCrawlManager implements MatchLifecycle {

    private static final Material FAKE_BLOCK = Material.BARRIER;

    private final Server server;
    private final Scheduler scheduler;

    /** Downed player UUID -> 3x3 block grid (always player blockY + 1) */
    private final Map<UUID, Set<Position3>> fakeBlockGrids = new HashMap<>();
    private final Set<Integer> trackedEntityIds = ConcurrentHashMap.newKeySet();

    private final DownedPacketInterceptor interceptor = new DownedPacketInterceptor(this);

    /* Public API */

    /**
     * Sends the crawl-forcing fake block above the given player and begins tracking
     */
    public void apply(@NotNull Player player) {
        boolean firstDown = fakeBlockGrids.isEmpty();

        Set<Position3> grid = computeGrid(player.getLocation());
        sendGrid(player, grid);
        fakeBlockGrids.put(player.getUniqueId(), grid);
        trackedEntityIds.add(player.getEntityId());

        if (firstDown) {
            server.getOnlinePlayers().forEach(this::injectInterceptor);
        }

        player.setPose(Pose.SWIMMING);
    }

    /**
     * Restores the real block data at the players tracked fake block position
     * and stops tracking
     */
    public void restore(Player player) {
        restoreById(player.getUniqueId(), player);
    }

    /**
     * Sends a new fake block if
     * the players block coordinates have changed, and restores the old one
     */
    public void onPlayerMoved(Player player, Location newLoc) {
        Set<Position3> oldGrid = fakeBlockGrids.get(player.getUniqueId());
        if (oldGrid == null) return;

        Set<Position3> newGrid = computeGrid(newLoc);
        if (oldGrid.equals(newGrid)) return;

        World world = player.getWorld();

        // Send fake blocks for positions entering the grid
        for (Position3 pos : newGrid) {
            if (!oldGrid.contains(pos)) {
                Location loc = LocationUtils.toLoc(world, pos);
                if (!loc.getBlock().getType().isSolid()) {
                   sendFakeBlock(player, loc);
                }
            }
        }

        // Restore real blocks for positions leaving the grid
        for (Position3 pos : oldGrid) {
            if (!newGrid.contains(pos)) {
                Location loc = LocationUtils.toLoc(world, pos);
                restoreFakeBlock(player, loc);
            }
        }

        fakeBlockGrids.put(player.getUniqueId(), newGrid);
    }

    /**
     * @return true if this player currently has a fake block tracked
     */
    public boolean isTracked(UUID playerId) {
        return fakeBlockGrids.containsKey(playerId);
    }

    /**
     * @return true if this entity currently is down tracked
     */
    public boolean isTrackedEntity(int entityId) {
        return trackedEntityIds.contains(entityId);
    }

    public void injectIfNeeded(Player player) {
        if (!fakeBlockGrids.isEmpty()) {
            injectInterceptor(player);
        }
    }

    /* Lifecycle */

    @Override
    public void onPlayerLeave(UUID playerId) {
        Player player = server.getPlayer(playerId);
        if (player != null) trackedEntityIds.remove(player.getEntityId());
        fakeBlockGrids.remove(playerId);
        cleanupInterceptorIfEmpty();
    }

    @Override
    public void onMatchEnd() {
        // Snapshot keys to avoid ConcurrentModificationException
        for (UUID id : new ArrayList<>(fakeBlockGrids.keySet())) {
            restoreById(id, server.getPlayer(id));
        }
        cleanUp();
    }

    @Override
    public void cleanUp() {
        fakeBlockGrids.clear();
        trackedEntityIds.clear();
        server.getOnlinePlayers().forEach(this::removeInterceptor);
    }

    /* Internals */

    /* Grid */

    private void sendGrid(Player player, Set<Position3> grid) {
        World world = player.getWorld();
        for (Position3 pos : grid) {
            Location loc = LocationUtils.toLoc(world, pos);
            if (!loc.getBlock().getType().isSolid()) {
                sendFakeBlock(player, loc);
            }
        }
    }

    private static Set<Position3> computeGrid(Location playerLoc) {
        int cx = playerLoc.getBlockX();
        int cy = playerLoc.getBlockY() + 1;
        int cz = playerLoc.getBlockZ();

        Set<Position3> grid = HashSet.newHashSet(9);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                grid.add(new Position3(cx + dx, cy, cz + dz));
            }
        }
        return grid;
    }

    private void restoreById(UUID playerId, @Nullable Player player) {
        var grid = fakeBlockGrids.remove(playerId);
        if (grid == null) return;

        if (player != null) trackedEntityIds.remove(player.getEntityId());

        if (player != null && player.isOnline()) {
            World world = player.getWorld();
            for (Position3 pos : grid) {
                Location loc = LocationUtils.toLoc(world, pos);
                player.sendBlockChange(loc, loc.getBlock().getBlockData());
            }
            scheduler.runLater(() -> player.setPose(Pose.STANDING), 2L);
        }

        cleanupInterceptorIfEmpty();
    }

    private static void sendFakeBlock(Player player, Location base) {
        player.sendBlockChange(base, FAKE_BLOCK.createBlockData());
    }

    private static void restoreFakeBlock(Player player, Location base) {
        player.sendBlockChange(base, base.getBlock().getBlockData());
    }

    /* Interceptor */

    private void cleanupInterceptorIfEmpty() {
        if (fakeBlockGrids.isEmpty()) {
            server.getOnlinePlayers().forEach(this::removeInterceptor);
        }
    }

    private void injectInterceptor(Player player) {
        Channel channel = ((CraftPlayer) player).getHandle().connection.connection.channel;
        if (channel.pipeline().get(DownedPacketInterceptor.HANDLER_NAME) == null) {
            channel.pipeline().addBefore(
                "packet_handler",
                DownedPacketInterceptor.HANDLER_NAME,
                interceptor
            );
        }
    }

    private void removeInterceptor(Player player) {
        Channel channel = ((CraftPlayer) player).getHandle().connection.connection.channel;
        if (channel.pipeline().get(DownedPacketInterceptor.HANDLER_NAME) != null) {
            channel.pipeline().remove(DownedPacketInterceptor.HANDLER_NAME);
        }
    }

}
