package com.creatorsplash.oxygenheist.platform.paper.weapon.service;

import com.creatorsplash.oxygenheist.application.common.LogCenter;
import com.creatorsplash.oxygenheist.application.match.MatchLifecycle;
import com.creatorsplash.oxygenheist.application.match.MatchService;
import com.creatorsplash.oxygenheist.application.match.Scheduler;
import com.creatorsplash.oxygenheist.domain.match.MatchSession;
import com.creatorsplash.oxygenheist.platform.paper.config.ArenaConfigService;
import com.creatorsplash.oxygenheist.platform.paper.config.GlobalConfigService;
import com.creatorsplash.oxygenheist.platform.paper.listener.WeaponListener;
import com.creatorsplash.oxygenheist.platform.paper.util.MM;
import com.creatorsplash.oxygenheist.platform.paper.util.ParticleUtils;
import com.creatorsplash.oxygenheist.platform.paper.weapon.WeaponRegistry;
import com.creatorsplash.oxygenheist.platform.paper.weapon.WeaponUtils;
import com.creatorsplash.oxygenheist.platform.paper.weapon.handler.WeaponHandler;
import com.creatorsplash.oxygenheist.platform.paper.world.ArenaSetup;
import lombok.RequiredArgsConstructor;
import org.bukkit.*;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.TextDisplay;
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
 *
 * TODO BIG CHANGES - ITEM DISPLAY - and other things t odo
 */
@RequiredArgsConstructor
public final class WeaponDropService implements MatchLifecycle, Listener {

    private static final Color PICKUP_PARTICLE_COLOR = Color.fromRGB(0, 255, 220);

    private final Server server;
    private final GlobalConfigService globals;
    private final ArenaConfigService arenaConfigService;
    private final WeaponRegistry weaponRegistry;
    private final MatchService matchService;
    private final Scheduler scheduler;
    private final LogCenter log;

    /** item UUID -> label TextDisplay UUID */
    private final Map<UUID, UUID> activeItems = new HashMap<>();
    private final Map<UUID, Double> particleOffsets = new HashMap<>();
    private final Random random = new Random();

    private Scheduler.Task spawnTask;
    private Scheduler.Task particleTask;

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        UUID itemId = event.getItem().getUniqueId();
        removeLabel(itemId);
        activeItems.remove(itemId);
        particleOffsets.remove(itemId);
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
        particleTask = scheduler.runRepeating(this::tickParticles, 1L, 4L);
    }

    @Override
    public void onMatchEnd() {
        if (spawnTask != null) { spawnTask.cancel(); spawnTask = null; }
        if (particleTask != null) { particleTask.cancel(); particleTask = null; }
        removeAllItems();
        activeItems.clear();
        particleOffsets.clear();
    }

    /* Spawning */

    private void spawnRandom() {
        if (activeItems.size() >= globals.get().weaponSpawner().maxActive()) return;
        if (weaponRegistry.all().isEmpty()) return;
        if (matchService.getSession().isEmpty()) return;

        Location loc = findRandomLocation();
        if (loc == null) {
            log.warn("WeaponDropService: could not find a valid spawn location");
            return;
        }

        WeaponHandler handler = weaponRegistry.random();
        ItemStack item = handler.createItemStack();
        if (item == null) return;

        Item dropped = loc.getWorld().dropItem(loc, item);
        dropped.setVelocity(new Vector(0.0, 0.0, 0.0));
        dropped.setPickupDelay(globals.get().weaponSpawner().pickupCooldownSeconds() * 20);

        TextDisplay label = spawnLabel(loc, WeaponUtils.formatDisplayName(handler.id()));

        activeItems.put(dropped.getUniqueId(), label.getUniqueId());
        particleOffsets.put(dropped.getUniqueId(), 0.0);
    }

    private TextDisplay spawnLabel(Location loc, String displayName) {
        return loc.getWorld().spawn(loc.clone().add(0, 1.4, 0), TextDisplay.class, d -> {
            d.setBillboard(Display.Billboard.CENTER);
            d.setShadowed(true);
            d.setDefaultBackground(false);
            d.setViewRange(12f);
            d.text(MM.msg("<aqua><bold>" + displayName + "</bold></aqua>\n<gray>⬆ Pick up"));
        });
    }

    /* Particles */

    private void tickParticles() {
        World world = resolveWorld();
        if (world == null) return;

        Particle.DustOptions dust = new Particle.DustOptions(PICKUP_PARTICLE_COLOR, 1.0f);

        for (UUID itemId : activeItems.keySet()) {
            Entity entity = world.getEntity(itemId);
            if (entity == null || !entity.isValid()) continue;

            double offset = particleOffsets.merge(itemId, 0.12, Double::sum) % (Math.PI * 2);
            particleOffsets.put(itemId, offset);

            Location center = entity.getLocation();
            for (int i = 0; i < 8; i++) {
                double angle = offset + (Math.PI * 2 * i / 8);
                ParticleUtils.spawn(
                    Particle.DUST,
                    center.clone().add(Math.cos(angle) * 0.7, 0.1, Math.sin(angle) * 0.7),
                    1, 0, 0, 0, 0, dust, (MatchSession) null
                );
            }
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

            boolean tooClose = activeItems.keySet().stream()
                .map(world::getEntity)
                .filter(Objects::nonNull)
                .anyMatch(e -> e.getLocation().distanceSquared(loc) < 100);

            if (!tooClose) return loc;
        }

        return null;
    }

    /* Cleanup */

    private void removeAllItems() {
        World world = resolveWorld();
        if (world == null) return;

        for (UUID itemId : activeItems.keySet()) {
            Entity entity = world.getEntity(itemId);
            if (entity != null) entity.remove();
            removeLabel(itemId);
        }
    }

    private void removeLabel(UUID itemId) {
        World world = resolveWorld();
        if (world == null) return;

        UUID labelId = activeItems.get(itemId);
        if (labelId == null) return;

        Entity label = world.getEntity(labelId);
        if (label != null) label.remove();
    }

    private World resolveWorld() {
        return arenaConfigService.getArena()
            .map(a -> server.getWorld(a.worldName()))
            .orElse(null);
    }

}
