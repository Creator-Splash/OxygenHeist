package com.creatorsplash.oxygenheist.platform.paper.world;

import com.creatorsplash.oxygenheist.application.match.MatchLifecycle;
import com.creatorsplash.oxygenheist.application.match.Scheduler;
import com.creatorsplash.oxygenheist.domain.match.MatchSnapshot;
import com.creatorsplash.oxygenheist.platform.paper.util.nms.DownedPacketInterceptor;
import com.creatorsplash.oxygenheist.platform.paper.util.nms.PoseUtil;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
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

    /** Downed player UUID -> location of their current fake block (always player blockY + 1) */
    private final Map<UUID, Location> fakeBlocks = new HashMap<>();
    private final Set<Integer> trackedEntityIds = ConcurrentHashMap.newKeySet();

    private final DownedPacketInterceptor interceptor = new DownedPacketInterceptor(this);

    /* Public API */

    /**
     * Sends the crawl-forcing fake block above the given player and begins tracking
     */
    public void apply(@NotNull Player player) {
        boolean firstDown = fakeBlocks.isEmpty();

        Location fakeLoc = fakeLocFor(player.getLocation());
        player.sendBlockChange(fakeLoc, FAKE_BLOCK.createBlockData());
        fakeBlocks.put(player.getUniqueId(), fakeLoc);
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
        Location oldFakeLoc = fakeBlocks.get(player.getUniqueId());
        if (oldFakeLoc == null) return;

        Location newFakeLoc = fakeLocFor(newLoc);
        if (sameBlock(oldFakeLoc, newFakeLoc)) return;

        // Restore real block at old position
        player.sendBlockChange(oldFakeLoc, oldFakeLoc.getBlock().getBlockData());
        // Send fake block at new position
        player.sendBlockChange(newFakeLoc, FAKE_BLOCK.createBlockData());

        fakeBlocks.put(player.getUniqueId(), newFakeLoc);
    }

    /**
     * @return true if this player currently has a fake block tracked
     */
    public boolean isTracked(UUID playerId) {
        return fakeBlocks.containsKey(playerId);
    }

    /**
     * @return true if this entity currently is down tracked
     */
    public boolean isTrackedEntity(int entityId) {
        return trackedEntityIds.contains(entityId);
    }

    public void injectIfNeeded(Player player) {
        if (!fakeBlocks.isEmpty()) {
            injectInterceptor(player);
        }
    }

    /* Lifecycle */

    @Override
    public void onPlayerLeave(UUID playerId) {
        Player player = server.getPlayer(playerId);
        if (player != null) trackedEntityIds.remove(player.getEntityId());
        fakeBlocks.remove(playerId);
        cleanupInterceptorIfEmpty();
    }

    @Override
    public void onMatchEnd() {
        // Snapshot keys to avoid ConcurrentModificationException
        for (UUID id : new ArrayList<>(fakeBlocks.keySet())) {
            restoreById(id, server.getPlayer(id));
        }
        fakeBlocks.clear();
        trackedEntityIds.clear();
        server.getOnlinePlayers().forEach(this::removeInterceptor);
    }

    /* Internals */

    private void restoreById(UUID playerId, @Nullable Player player) {
        Location fakeLoc = fakeBlocks.remove(playerId);
        if (fakeLoc == null) return;

        cleanupInterceptorIfEmpty();

        if (player != null && player.isOnline()) {
            player.sendBlockChange(fakeLoc, fakeLoc.getBlock().getBlockData());
            player.setPose(Pose.STANDING);
        }
    }

    private static Location fakeLocFor(Location playerLoc) {
        return new Location(
            playerLoc.getWorld(),
            playerLoc.getBlockX(),
            playerLoc.getBlockY() + 1.0,
            playerLoc.getBlockZ()
        );
    }

    private static boolean sameBlock(Location a, Location b) {
        return a.getBlockX() == b.getBlockX()
            && a.getBlockY() == b.getBlockY()
            && a.getBlockZ() == b.getBlockZ();
    }

    /* Interceptor */

    private void cleanupInterceptorIfEmpty() {
        if (fakeBlocks.isEmpty()) {
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
