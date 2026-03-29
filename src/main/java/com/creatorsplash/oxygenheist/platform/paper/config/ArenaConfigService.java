package com.creatorsplash.oxygenheist.platform.paper.config;

import com.creatorsplash.oxygenheist.application.common.LogCenter;
import com.creatorsplash.oxygenheist.domain.zone.config.ZoneDefinition;
import com.creatorsplash.oxygenheist.platform.paper.world.ArenaSetup;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Blocking;

import java.io.File;
import java.util.*;

/**
 * Manages persistent storage of arena geometry and zone definitions in {@code arena.yml}
 */
@RequiredArgsConstructor
public final class ArenaConfigService {

    private static final String FILE_NAME = "arena.yml";

    private static final String ARENA = "arena";
    private static final String ZONES = "zones";

    private static final String DISPLAY_NAME = "display.name";
    private static final String WORLD = "world";
    private static final String CENTER_X = "center-x";
    private static final String CENTER_Y = "center-y";
    private static final String CENTER_Z = "center-z";
    private static final String RADIUS = "radius";
    private static final String INITIAL_SIZE = "initial-size";

    private final JavaPlugin plugin;
    private final LogCenter log;

    @Getter
    private volatile Optional<ArenaSetup> arena = Optional.empty();
    private final Map<String, ZoneDefinition> zones = new LinkedHashMap<>();

    /* == Load == */

    /**
     * Loads arena geometry from {@code arena.yml} if it exists
     */
    public void load() {
        File arenaFile = arenaFile();

        if (!arenaFile.exists()) {
            log.warn("No arena.yml found - arena is not configured. Use '/oh arena set' to configure");
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(arenaFile);

        loadArena(config);
    }

    private void loadArena(YamlConfiguration config) {
        if (!config.contains(path(ARENA, WORLD))) {
            log.error(FILE_NAME + " is missing required field '"
                + path(ARENA, WORLD) + "' - arena is unconfigured");
            return;
        }

        String world = config.getString(path(ARENA, WORLD));
        double centerX = config.getDouble(path(ARENA, CENTER_X), 0.0);
        double centerZ = config.getDouble(path(ARENA, CENTER_Z), 0.0);
        double initialSize = config.getDouble(path(ARENA, INITIAL_SIZE), 500);

        if (initialSize <= 0) {
            log.warn(FILE_NAME + " has invalid initial-size (" + initialSize + ") - must be > 0");
            return;
        }

        this.arena = Optional.of(new ArenaSetup(world, centerX, centerZ, initialSize));
        log.info("Arena loaded: world=<white>" + world + "</white> center=(<white>" +
                (int) centerX + ", " + (int) centerZ + "</white>) size=<white>" + (int) initialSize);
    }

    private void loadZones(YamlConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection(ZONES);
        if (section == null) return;

        int loaded = 0;
        for (String id : section.getKeys(false)) {
            ConfigurationSection zs = section.getConfigurationSection(id);
            if (zs == null) continue;

            String displayName = zs.getString(DISPLAY_NAME, id);
            String world = zs.getString(WORLD);

            if (world == null || world.isBlank()) {
                log.warn("Zone '<white>" + id + "</white>' is missing 'world' - skipped");
                continue;
            }

            ZoneDefinition def;

            if (zs.contains(RADIUS)) {
                double cx = zs.getDouble(CENTER_X);
                double cy = zs.getDouble(CENTER_Y);
                double cz = zs.getDouble(CENTER_Z);
                double r = zs.getDouble(RADIUS);

                if (r <= 0) {
                    log.warn("Zone '<white>" + id + "</white>' has invalid radius (" + r + ") - skipped");
                    continue;
                }

                def = new ZoneDefinition.Circle(id, displayName, world, cx, cy, cz, r);
            } else {
                // Cuboid format
                double minX = zs.getDouble("min-x");
                double minY = zs.getDouble("min-y");
                double minZ = zs.getDouble("min-z");
                double maxX = zs.getDouble("max-x");
                double maxY = zs.getDouble("max-y");
                double maxZ = zs.getDouble("max-z");

                def = new ZoneDefinition.Cuboid(id, displayName, world,
                    minX, minY, minZ, maxX, maxY, maxZ);
            }

            zones.put(id, def);
            loaded++;
        }

        log.info("Loaded <white>" + loaded + "</white> zone(s) from " + FILE_NAME);
    }

    /* Arena */

    /**
     * Saves the given {@link ArenaSetup} to {@code arena.yml} and updates the in-memory state
     *
     * @param setup the arena geometry to persist
     * @throws RuntimeException if the file cannot be written
     */
    @Blocking
    public void saveArena(ArenaSetup setup) {
        YamlConfiguration config = loadOrEmpty();

        config.set(WORLD, setup.worldName());
        config.set(CENTER_X, setup.centerX());
        config.set(CENTER_Z, setup.centerZ());
        config.set(INITIAL_SIZE, setup.initialSize());

        persist(config);
        this.arena = Optional.of(setup);

        log.info("Arena saved: world=<white>" + setup.worldName()
            + "</white> size=<white>" + (int) setup.initialSize() + "</white>");
    }

    /**
     * @return true if an arena has been successfully loaded
     */
    public boolean isArenaConfigured() {
        return arena.isPresent();
    }

    /* Zones */

    /**
     * Saves a zone definition to {@code arena.yml} and registers it in-memory
     *
     * <p>If a zone with the same id already exists it is overwritten.</p>
     */
    @Blocking
    public void saveZone(ZoneDefinition zone) {
        YamlConfiguration config = loadOrEmpty();
        String path = path(ZONES, zone.id());

        config.set(path(path, DISPLAY_NAME), zone.displayName());
        config.set(path(path, WORLD), zone.worldName());

        switch (zone) {
            case ZoneDefinition.Cuboid c -> {
                config.set(path(path, "min-x"), c.minX());
                config.set(path(path, "min-y"), c.minY());
                config.set(path(path, "min-z"), c.minZ());
                config.set(path(path, "max-x"), c.maxX());
                config.set(path(path, "max-y"), c.maxY());
                config.set(path(path, "max-z"), c.maxZ());
            }
            case ZoneDefinition.Circle ci -> {
                config.set(path(path, CENTER_X), ci.centerX());
                config.set(path(path, CENTER_Y), ci.centerY());
                config.set(path(path, CENTER_Z), ci.centerZ());
                config.set(path(path, RADIUS), ci.radius());
            }
        }

        persist(config);
        zones.put(zone.id(), zone);

        log.info("Zone saved: <white>" + zone.id() + "</white> ("
            + (zone instanceof ZoneDefinition.Circle ? "circle" : "cuboid") + ")");
    }

    /**
     * Removes a zone definition by id from {@code arena.yml} and in-memory state
     *
     * @return true if the zone existed and was removed
     */
    @Blocking
    public boolean removeZone(String id) {
        if (!zones.containsKey(id)) return false;

        YamlConfiguration config = loadOrEmpty();
        config.set("zones." + id, null);

        persist(config);
        zones.remove(id);

        log.info("Zone removed: <white>" + id + "</white>");
        return true;
    }

    /**
     * @return all currently loaded zone definitions as an unmodifiable list
     */
    public List<ZoneDefinition> getZones() {
        return List.copyOf(zones.values());
    }

    /**
     * @return true if at least one zone has been configured
     */
    public boolean hasZones() {
        return !zones.isEmpty();
    }

    /* Helpers */

    private YamlConfiguration loadOrEmpty() {
        File file = arenaFile();
        return file.exists()
            ? YamlConfiguration.loadConfiguration(file)
            : new YamlConfiguration();
    }

    private void persist(YamlConfiguration config) {
        try {
            config.save(arenaFile());
        } catch (Exception e) {
            log.error("Failed to save " + FILE_NAME, e);
            throw new RuntimeException("Could not save arena configuration", e);
        }
    }

    private String path(String... nodes) {
        return String.join(".", nodes);
    }

    private File arenaFile() {
        return new File(plugin.getDataFolder(), FILE_NAME);
    }

}
