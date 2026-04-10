package com.creatorsplash.oxygenheist.platform.paper.weapon.service;

import com.creatorsplash.oxygenheist.application.common.LogCenter;
import com.creatorsplash.oxygenheist.application.match.MatchLifecycle;
import com.creatorsplash.oxygenheist.application.match.MatchService;
import com.creatorsplash.oxygenheist.application.match.Scheduler;
import com.creatorsplash.oxygenheist.platform.paper.config.ArenaConfigService;
import com.creatorsplash.oxygenheist.platform.paper.config.GlobalConfigService;
import com.creatorsplash.oxygenheist.platform.paper.listener.WeaponListener;
import com.creatorsplash.oxygenheist.platform.paper.weapon.WeaponRegistry;
import com.creatorsplash.oxygenheist.platform.paper.world.ArenaSetup;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Spawns weapon pickups as dropped items at random locations during a match
 *
 * <p>Items are real {@link Item} entities - players pick them up naturally
 * and {@link WeaponListener} handles them once in-hand</p>
 */
@RequiredArgsConstructor
public final class WeaponDropService implements MatchLifecycle, Listener {

    private final Server server;
    private final GlobalConfigService globals;
    private final ArenaConfigService arenaConfigService;
    private final WeaponRegistry weaponRegistry;
    private final MatchService matchService;
    private final Scheduler scheduler;
    private final LogCenter log;

    private final Set<UUID> activeItems = new HashSet<>();
    private final Random random = new Random();
    private Scheduler.Task spawnTask;

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        activeItems.remove(event.getItem().getUniqueId());
    }

    /* Lifecycle */

    @Override
    public void onMatchStart() {
        var cfg = globals.get().weaponSpawner();
        for (int i = 0; i < cfg.initialCount(); i++) spawnRandom();
        spawnTask = scheduler.runRepeating(
            this::spawnRandom,
            cfg.spawnIntervalSeconds() * 20L,
            cfg.spawnIntervalSeconds() * 20L
        );
    }

    @Override
    public void onMatchEnd() {
        if (spawnTask != null) { spawnTask.cancel(); spawnTask = null; }
        removeAllItems();
        activeItems.clear();
    }

    /* Spawning */

    private void spawnRandom() {
        if (activeItems.size() >= globals.get().weaponSpawner().maxActive()) return;
        if (weaponRegistry.all().isEmpty()) return;
        if (matchService.getSession().isEmpty()) return;

        Location loc = findRandomLocation();
        if (loc == null) {
            log.warn("WeaponDropManager: could not find a valid spawn location");
            return;
        }

        ItemStack item = weaponRegistry.random().createItemStack();
        if (item == null) return;

        Item dropped = loc.getWorld().dropItem(loc, item);
        dropped.setVelocity(new Vector(0.0, 0.0, 0.0));
        dropped.setPickupDelay(globals.get().weaponSpawner().pickupCooldownSeconds() * 20);
        activeItems.add(dropped.getUniqueId());
    }

    private void removeAllItems() {
        ArenaSetup arena = arenaConfigService.getArena().orElse(null);
        if (arena == null) return;

        World world = server.getWorld(arena.worldName());
        if (world == null) return;

        for (UUID id : activeItems) {
            Entity entity = world.getEntity(id);
            if (entity != null) entity.remove();
        }
    }

    /* Location finding */

    private Location findRandomLocation() {
        ArenaSetup arena = arenaConfigService.getArena().orElse(null);
        if (arena == null) return null;

        World world = server.getWorld(arena.worldName());
        if (world == null) return null;

        double safeRadius = (arena.initialSize() / 2.0) * globals.get().weaponSpawner().pickupRadius();

        for (int attempt = 0; attempt < 10; attempt++) {
            double x = arena.centerX() + (random.nextDouble() * 2 - 1) * safeRadius;
            double z = arena.centerZ() + (random.nextDouble() * 2 - 1) * safeRadius;
            int y = world.getHighestBlockYAt((int) x, (int) z);

            if (y < 1) continue;

            Location loc = new Location(world, x, y + 1, z);
            if (!loc.clone().subtract(0, 1, 0).getBlock().getType().isSolid()) continue;
            if (!loc.getBlock().getType().isAir()) continue;

            // Keep away from existing drops
            boolean tooClose = activeItems.stream()
                .map(world::getEntity)
                .filter(Objects::nonNull)
                .anyMatch(e -> e.getLocation().distanceSquared(loc) < 100);

            if (!tooClose) return loc;
        }

        return null;
    }

}
