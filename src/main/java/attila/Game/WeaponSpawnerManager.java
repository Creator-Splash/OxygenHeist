package attila.Game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import attila.Config.GameConfigHandler;
import attila.OxygenMain;

/**
 * Manages weapon pickup holograms that spawn randomly within the game border.
 * When a player walks through a hologram, it executes a configurable console command.
 */
public class WeaponSpawnerManager implements Listener {
    
    private final OxygenMain plugin;
    private final GameManager gameManager;
    private final GameConfigHandler config;
    private final Random random;
    
    private final Map<String, WeaponPickupHologram> activeHolograms;
    private final Set<UUID> recentPickups; // Cooldown tracking
    private final Map<UUID, Long> pickupCooldowns;
    
    private BukkitTask spawnTask;
    private BukkitTask particleTask;
    private BukkitTask animationTask;
    private int hologramCounter = 0;
    private boolean running = false;
    
    public WeaponSpawnerManager(OxygenMain plugin, GameManager gameManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.config = plugin.getGameConfig();
        this.random = new Random();
        this.activeHolograms = new HashMap<>();
        this.recentPickups = new HashSet<>();
        this.pickupCooldowns = new HashMap<>();
    }
    
    /**
     * Starts the weapon spawner system.
     */
    public void start() {
        if (running) return;
        running = true;
        
        plugin.getLogger().info("Starting weapon spawner system...");

        spawnInitialHolograms();

        int spawnInterval = config.getWeaponSpawnInterval();
        spawnTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!running) {
                    cancel();
                    return;
                }
                spawnRandomHologram();
            }
        }.runTaskTimer(plugin, spawnInterval * 20L, spawnInterval * 20L);

        particleTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!running) {
                    cancel();
                    return;
                }
                for (WeaponPickupHologram hologram : activeHolograms.values()) {
                    if (hologram.isActive()) {
                        hologram.spawnParticles();
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);

        animationTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!running) {
                    cancel();
                    return;
                }
                for (WeaponPickupHologram hologram : activeHolograms.values()) {
                    if (hologram.isActive()) {
                        hologram.updateAnimation();
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
        
        plugin.getLogger().info("Weapon spawner system started!");
    }
    
    /**
     * Stops the weapon spawner system and cleans up all holograms.
     */
    public void stop() {
        if (!running) return;
        running = false;
        
        plugin.getLogger().info("Stopping weapon spawner system...");
        
        if (spawnTask != null) {
            spawnTask.cancel();
            spawnTask = null;
        }
        
        if (particleTask != null) {
            particleTask.cancel();
            particleTask = null;
        }
        
        if (animationTask != null) {
            animationTask.cancel();
            animationTask = null;
        }

        for (WeaponPickupHologram hologram : activeHolograms.values()) {
            hologram.remove();
        }
        activeHolograms.clear();
        recentPickups.clear();
        pickupCooldowns.clear();
        
        plugin.getLogger().info("Weapon spawner system stopped!");
    }
    
    /**
     * Spawns the initial set of holograms.
     */
    private void spawnInitialHolograms() {
        int initialCount = config.getWeaponInitialSpawnCount();
        for (int i = 0; i < initialCount; i++) {
            spawnRandomHologram();
        }
        plugin.getLogger().info("Spawned " + initialCount + " initial weapon pickups.");
    }
    
    /**
     * Spawns a random hologram at a random location within the border.
     */
    public void spawnRandomHologram() {

        if (!running || gameManager.getGameState() != GameState.PLAYING) {
            return;
        }

        int maxHolograms = config.getWeaponMaxHolograms();
        if (activeHolograms.size() >= maxHolograms) {
            return;
        }

        List<String> weaponIds = config.getWeaponIds();
        if (weaponIds.isEmpty()) {
            return;
        }
        
        String weaponId = weaponIds.get(random.nextInt(weaponIds.size()));
        String displayName = config.getWeaponDisplayName(weaponId);
        String command = config.getWeaponCommand(weaponId);
        double pickupRadius = config.getWeaponPickupRadius();

        Location spawnLocation = findRandomLocationInBorder();
        if (spawnLocation == null) {
            return;
        }

        String hologramId = "weapon_" + (++hologramCounter);
        WeaponPickupHologram hologram = new WeaponPickupHologram(
            hologramId,
            spawnLocation,
            displayName,
            command,
            pickupRadius
        );
        
        activeHolograms.put(hologramId, hologram);
    }
    
    /**
     * Finds a random safe location within the world border.
     */
    private Location findRandomLocationInBorder() {
        World world = plugin.getBorderManager().getArenaWorld();
        if (world == null) {
            String worldName = config.getPlaygroundWorld();
            if (worldName != null && !worldName.isEmpty()) {
                world = Bukkit.getWorld(worldName);
            }
        }
        
        if (world == null) return null;
        
        WorldBorder border = world.getWorldBorder();
        Location center = border.getCenter();
        double size = border.getSize() / 2.0;

        double safeSize = size * 0.85;

        for (int attempt = 0; attempt < 10; attempt++) {
            double offsetX = (random.nextDouble() * 2 - 1) * safeSize;
            double offsetZ = (random.nextDouble() * 2 - 1) * safeSize;
            
            double x = center.getX() + offsetX;
            double z = center.getZ() + offsetZ;

            int highestY = world.getHighestBlockYAt((int) x, (int) z);

            if (highestY < 1 || highestY > 255) {
                continue;
            }
            
            Location testLoc = new Location(world, x, highestY + 1, z);

            if (!testLoc.clone().subtract(0, 1, 0).getBlock().getType().isSolid()) {
                continue;
            }

            if (!testLoc.getBlock().getType().isAir() || 
                !testLoc.clone().add(0, 1, 0).getBlock().getType().isAir()) {
                continue;
            }

            boolean tooClose = false;
            for (WeaponPickupHologram existing : activeHolograms.values()) {
                if (existing.getLocation().distance(testLoc) < 10) {
                    tooClose = true;
                    break;
                }
            }
            
            if (!tooClose) {
                return testLoc;
            }
        }
        
        return null;
    }
    
    /**
     * Handles player movement to check for hologram pickups.
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!running) return;
        if (gameManager.getGameState() != GameState.PLAYING) return;
        
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        Long lastPickup = pickupCooldowns.get(playerUUID);
        long cooldownMs = config.getWeaponPickupCooldown() * 1000L;
        if (lastPickup != null && System.currentTimeMillis() - lastPickup < cooldownMs) {
            return;
        }

        if (gameManager.isPlayerDead(playerUUID) || gameManager.isPlayerDowned(playerUUID)) {
            return;
        }
        
        Location playerLoc = player.getLocation();

        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, WeaponPickupHologram> entry : activeHolograms.entrySet()) {
            WeaponPickupHologram hologram = entry.getValue();
            
            if (hologram.isActive() && hologram.isPlayerInRange(playerLoc)) {

                hologram.onPickup(player);
                toRemove.add(entry.getKey());
                pickupCooldowns.put(playerUUID, System.currentTimeMillis());
                
                player.sendMessage("§a§l¡Recogiste: §b" + hologram.getDisplayName() + "§a§l!");

                break;
            }
        }

        for (String id : toRemove) {
            activeHolograms.remove(id);
        }
    }
    
    /**
     * Gets the count of active holograms.
     */
    public int getActiveHologramCount() {
        return activeHolograms.size();
    }
    
    /**
     * Removes all holograms (for cleanup).
     */
    public void removeAllHolograms() {
        for (WeaponPickupHologram hologram : activeHolograms.values()) {
            hologram.remove();
        }
        activeHolograms.clear();
    }
    
    /**
     * Cleans up orphaned ArmorStands from weapon pickups.
     */
    public void cleanupOrphanedHolograms() {
        String playgroundWorld = config.getPlaygroundWorld();
        if (playgroundWorld == null || playgroundWorld.isEmpty()) return;
        
        World world = Bukkit.getWorld(playgroundWorld);
        if (world == null) return;
        
        int removed = 0;
        for (org.bukkit.entity.Entity entity : world.getEntities()) {
            if (entity instanceof ArmorStand) {
                ArmorStand stand = (ArmorStand) entity;
                if (!stand.isVisible() && stand.isMarker()) {
                    String customName = stand.getCustomName();

                    if (customName == null || customName.contains("⚔") || customName.contains("¡Pasa para recoger!") || customName.contains("¡PICKUP!")) {
                        stand.remove();
                        removed++;
                    }
                }
            }
        }
        
        if (removed > 0) {
            plugin.getLogger().info("Cleaned up " + removed + " orphaned weapon pickup holograms.");
        }
    }
    
    public boolean isRunning() {
        return running;
    }
}
