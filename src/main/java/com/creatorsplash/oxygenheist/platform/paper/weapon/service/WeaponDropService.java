package com.creatorsplash.oxygenheist.platform.paper.weapon.service;

import com.creatorsplash.oxygenheist.application.common.LogCenter;
import com.creatorsplash.oxygenheist.application.match.MatchLifecycle;
import com.creatorsplash.oxygenheist.application.match.MatchService;
import com.creatorsplash.oxygenheist.application.match.Scheduler;
import com.creatorsplash.oxygenheist.domain.match.MatchSession;
import com.creatorsplash.oxygenheist.platform.paper.config.ArenaConfigService;
import com.creatorsplash.oxygenheist.platform.paper.config.GlobalConfig;
import com.creatorsplash.oxygenheist.platform.paper.config.GlobalConfigService;
import com.creatorsplash.oxygenheist.platform.paper.config.message.MessageConfigService;
import com.creatorsplash.oxygenheist.platform.paper.config.misc.SoundConfig;
import com.creatorsplash.oxygenheist.platform.paper.listener.WeaponListener;
import com.creatorsplash.oxygenheist.platform.paper.util.MM;
import com.creatorsplash.oxygenheist.platform.paper.util.ParticleUtils;
import com.creatorsplash.oxygenheist.platform.paper.weapon.WeaponRegistry;
import com.creatorsplash.oxygenheist.platform.paper.weapon.WeaponUtils;
import com.creatorsplash.oxygenheist.platform.paper.weapon.handler.WeaponHandler;
import com.creatorsplash.oxygenheist.platform.paper.world.ArenaSetup;
import lombok.RequiredArgsConstructor;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Spawns weapon pickups as dropped items at random locations during a match
 *
 * <p>Items are real {@link Item} entities - players pick them up naturally
 * and {@link WeaponListener} handles them once in-hand</p>
 *
 * TODO ITEM DISPLAY
 */
@RequiredArgsConstructor
public final class WeaponDropService implements MatchLifecycle, Listener {

    private static final Color PICKUP_PARTICLE_COLOR = Color.fromRGB(0, 255, 220);

    private final Server server;
    private final GlobalConfigService globals;
    private final ArenaConfigService arenaConfigService;
    private final MessageConfigService messages;
    private final WeaponRegistry weaponRegistry;
    private final Scheduler scheduler;
    private final LogCenter log;

    private List<Location> surfaceCache = new ArrayList<>();

    /** item UUID -> label TextDisplay UUID */
    private final Map<UUID, UUID> activeItems = new HashMap<>();

    private final Map<UUID, Double> particleOffsets = new HashMap<>();

    private final Map<UUID, Long> pickupDeniedCooldown = new HashMap<>();
    private static final long PICKUP_DENIED_COOLDOWN_MS = 2000;

    private final Random random = new Random();
    private Scheduler.Task particleTask;

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        UUID itemId = event.getItem().getUniqueId();
        if (!activeItems.containsKey(itemId)) return;

        UUID playerId = player.getUniqueId();
        int maxPerPlayer = globals.get().weaponSpawner().maxPerPlayer();

        if (countHeldWeapons(player) >= maxPerPlayer) {
            event.setCancelled(true);

            // Prevent message spam
            long now = System.currentTimeMillis();
            if (now - pickupDeniedCooldown.getOrDefault(playerId, 0L) >= PICKUP_DENIED_COOLDOWN_MS) {
                pickupDeniedCooldown.put(playerId, now);

                var playerMsgs = messages.get().player();
                player.sendRichMessage(playerMsgs.weaponInventoryFull());
                SoundConfig sound = playerMsgs.weaponInventoryFullSound();
                if (sound != null) sound.playTo(player);
            }
            return;
        }

        // Valid pickup
        removeLabel(itemId);
        activeItems.remove(itemId);
        particleOffsets.remove(itemId);
        pickupDeniedCooldown.remove(playerId);

        // Respawn if below minimum
        int deficit = globals.get().weaponSpawner().minimumOnField() - activeItems.size();
        for (int i = 0; i < deficit; i++) {
            Location candidate = surfaceCache.get(random.nextInt(surfaceCache.size()));
            spawnAt(candidate);
        }
    }

    /* Lifecycle */

    @Override
    public void onCountdownStart() {
        scheduler.runAsync(() -> {
            buildSurfaceCache();
            log.info("Surface cache built: <white>" + surfaceCache.size() + "</white> valid positions");
        });
    }

    @Override
    public void onMatchStart() {
        spawnInitial();

        particleTask = scheduler.runRepeating(this::tickParticles, 1L, 4L);
    }

    @Override
    public void onMatchEnd() {
        if (particleTask != null) { particleTask.cancel(); particleTask = null; }
        removeAllItems();
        activeItems.clear();
        particleOffsets.clear();
        pickupDeniedCooldown.clear();
    }

    /* Spawning */

    private void spawnInitial() {
        var cfg = globals.get().weaponSpawner();

        if (surfaceCache.isEmpty()) {
            log.warn("WeaponDropService: surface cache is empty - no weapons will spawn. "
                + "Check min-spawn-y, max-spawn-y, exclusion zones, and allowed-surface-blocks");
            return;
        }

        int target = Math.min(cfg.initialCount(), cfg.maximumOnField());

        if (surfaceCache.size() < target) {
            log.warn("WeaponDropService: surface cache has only <white>" + surfaceCache.size()
                + "</white> positions but initial-count is <white>" + target
                + "</white> - spawning <white>" + surfaceCache.size() + "</white> weapons. "
                + "Reduce initial-count or adjust Y range / exclusion zones.");
            target = surfaceCache.size();
        }

        List<Location> shuffled = new ArrayList<>(surfaceCache);
        Collections.shuffle(shuffled, random);

        int bucketSize = Math.max(1, shuffled.size() / target);

        int spawned = 0;
        for (int i = 0; i < target; i++) {
            int bucketStart = i * bucketSize;
            if (bucketStart >= shuffled.size()) break;

            int bucketEnd = Math.min(bucketStart + bucketSize, shuffled.size());
            Location loc = shuffled.get(bucketStart + random.nextInt(bucketEnd - bucketStart));
            spawnAt(loc);
            spawned++;
        }

        log.info("WeaponDropService: spawned <white>" + spawned + "</white> weapons "
            + "(<white>" + surfaceCache.size() + "</white> valid positions available)");
    }

    private void spawnAt(Location loc) {
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
            d.setPersistent(false);
            d.text(MM.msg("<aqua><bold>" + displayName + "</bold></aqua>\n<gray>⬆ Pick up"));
        });
    }

    /* Particles */

    private void tickParticles() {
        World world = arenaConfigService.resolveWorld();
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
                    1, 0, 0, 0, 0.2, dust, (MatchSession) null
                );
            }
        }
    }

    /* Surface Cache */

    private void buildSurfaceCache() {
        surfaceCache.clear();

        ArenaSetup arena = arenaConfigService.getArena().orElse(null);
        World world = arenaConfigService.resolveWorld();
        if (arena == null || world == null) return;

        var cfg = globals.get().weaponSpawner();
        int target = Math.min(cfg.initialCount(), cfg.maximumOnField());

        loadChunkSnapshots(world, arena, cfg.surfaceScanStep())
            .thenAcceptAsync(snapshots -> {
                List<Location> result = scanSurfaces(snapshots, arena, world, cfg, cfg.surfaceScanStep());

                // Second pass with half the step size if we didn't find enough positions
                if (result.size() < target && cfg.surfaceScanStep() > 1) {
                    log.warn("Surface cache first pass found <white>" + result.size()
                        + "</white> positions (need <white>" + target
                        + "</white>) - running denser second pass");

                    int denseStep = Math.max(1, cfg.surfaceScanStep() / 2);
                    List<Location> secondPass = scanSurfaces(snapshots, arena, world, cfg, denseStep);

                    // Merge - add positions from second pass not already close to first pass results
                    for (Location loc : secondPass) {
                        boolean tooClose = result.stream()
                            .anyMatch(existing -> existing.distanceSquared(loc) < (denseStep * denseStep));
                        if (!tooClose) result.add(loc);
                    }

                    log.info("Second pass added <white>" + (result.size() - target)
                        + "</white> additional positions");
                }

                if (result.size() < target) {
                    log.warn("Surface cache has only <white>" + result.size()
                        + "</white> positions after both passes (target: <white>" + target
                        + "</white>). Adjust min/max-spawn-y, exclusion zones, or allowed-surface-blocks");
                }

                surfaceCache = result;
                log.info("Surface cache ready: <white>" + result.size()
                    + "</white> positions across <white>" + snapshots.size() + "</white> chunks");

                //scheduler.run(this::spawnInitial);
            });
    }

    private CompletableFuture<Map<ChunkPos, ChunkSnapshot>> loadChunkSnapshots(
        World world,
        ArenaSetup arena,
        int step
    ) {
        double half = arena.initialSize() / 2.0 * 0.85;
        double startX = arena.centerX() - half;
        double endX = arena.centerX() + half;
        double startZ = arena.centerZ() - half;
        double endZ = arena.centerZ() + half;

        Set<ChunkPos> seen = new HashSet<>();
        List<ChunkPos> neededList = new ArrayList<>();

        for (double x = startX; x <= endX; x += step) {
            for (double z = startZ; z <= endZ; z += step) {
                ChunkPos pos = new ChunkPos(((int) x) >> 4, ((int) z) >> 4);
                if (seen.add(pos)) neededList.add(pos);
            }
        }

        List<CompletableFuture<ChunkSnapshot>> futures = neededList.stream()
            .map(pos -> world.getChunkAtAsync(pos.x(), pos.z())
                .thenApply(Chunk::getChunkSnapshot))
            .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApplyAsync(ignored -> {
                Map<ChunkPos, ChunkSnapshot> snapshots = new HashMap<>();
                for (int i = 0; i < neededList.size(); i++) {
                    snapshots.put(neededList.get(i), futures.get(i).join());
                }
                return snapshots;
            });
    }

    private List<Location> scanSurfaces(
        Map<ChunkPos, ChunkSnapshot> snapshots,
        ArenaSetup arena,
        World world,
        GlobalConfig.WeaponSpawnerConfig cfg,
        int step
    ) {
        double half = arena.initialSize() / 2.0 * 0.85;
        double startX = arena.centerX() - half;
        double endX = arena.centerX() + half;
        double startZ = arena.centerZ() - half;
        double endZ = arena.centerZ() + half;

        int clampedMaxY = Math.min(cfg.maxSpawnY(), world.getMaxHeight() - 2);
        int clampedMinY = Math.max(cfg.minSpawnY(), world.getMinHeight());

        List<Location> result = new ArrayList<>();

        for (double x = startX; x <= endX; x += step) {
            for (double z = startZ; z <= endZ; z += step) {
                if (isExcluded(x, z, arena.worldName())) continue;

                ChunkSnapshot snap = snapshots.get(
                    new ChunkPos(((int) x) >> 4, ((int) z) >> 4));
                if (snap == null) continue;

                Location loc = findSurfaceInColumn(snap, world, x, z, clampedMinY, clampedMaxY, cfg);
                if (loc != null) result.add(loc);
            }
        }

        return result;
    }

    private @Nullable Location findSurfaceInColumn(
        ChunkSnapshot snap,
        World world,
        double x, double z,
        int minY, int maxY,
        GlobalConfig.WeaponSpawnerConfig cfg
    ) {
        int localX = ((int) x) & 15;
        int localZ = ((int) z) & 15;

        // Scan downward - finds trench floor, ignores bridges/canopy above maxY
        for (int y = maxY; y >= minY; y--) {
            Material surface = snap.getBlockType(localX, y, localZ);
            if (!surface.isSolid()) continue;

            Material air1 = snap.getBlockType(localX, y + 1, localZ);
            Material air2 = snap.getBlockType(localX, y + 2, localZ);
            if (!air1.isAir() || !air2.isAir()) continue;

            Set<Material> allowed = cfg.allowedSurfaceBlocks();
            if (!allowed.isEmpty() && !allowed.contains(surface)) continue;

            return new Location(world, x, y + 1, z);
        }

        return null;
    }

    private boolean isExcluded(double x, double z, String worldName) {
        return arenaConfigService.getExclusionZones().stream()
            .anyMatch(zone -> zone.contains(x, z, worldName));
    }

    /* Cleanup */

    private void removeAllItems() {
        World world = arenaConfigService.resolveWorld();
        if (world == null) return;

        for (UUID itemId : activeItems.keySet()) {
            Entity entity = world.getEntity(itemId);
            if (entity != null) entity.remove();
            removeLabel(itemId);
        }
    }

    private void removeLabel(UUID itemId) {
        World world = arenaConfigService.resolveWorld();
        if (world == null) return;

        UUID labelId = activeItems.get(itemId);
        if (labelId == null) return;

        Entity label = world.getEntity(labelId);
        if (label != null) label.remove();
    }

    /* Helpers */

    private int countHeldWeapons(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && weaponRegistry.find(item) != null) count++;
        }
        return count;
    }

    private record ChunkPos(int x, int z) {}

}
