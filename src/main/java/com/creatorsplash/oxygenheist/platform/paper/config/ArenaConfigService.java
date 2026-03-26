package com.creatorsplash.oxygenheist.platform.paper.config;

import com.creatorsplash.oxygenheist.application.common.LogCenter;
import com.creatorsplash.oxygenheist.platform.paper.world.ArenaSetup;
import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

@RequiredArgsConstructor
public final class ArenaConfigService {

    private static final String FILE_NAME = "arena.yml";

    private final JavaPlugin plugin;
    private final LogCenter log;

    private volatile Optional<ArenaSetup> arena = Optional.empty();

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

        if (!config.contains("arena.world")) {
            log.error("arena.yml is missing required field 'arena.world' - arena is unconfigured");
            return;
        }

        String world = config.getString("arena.world");
        double centerX = config.getDouble("arena.center-x", 0.0);
        double centerZ = config.getDouble("arena.center-z", 0.0);
        double initialSize = config.getDouble("arena.initial-size", 500);

        if (initialSize <= 0) {
            log.warn("arena.yml has invalid initial-size (" + initialSize + ") - must be > 0");
            return;
        }

        this.arena = Optional.of(new ArenaSetup(world, centerX, centerZ, initialSize));
        log.info("Arena loaded: world=<white>" + world + "</white> center=(<white>" +
            (int) centerX + ", " + (int) centerZ + "</white>) size=<white>" + (int) initialSize);
    }

    /**
     * Saves the given {@link ArenaSetup} to {@code arena.yml} and updates the in-memory state
     *
     * @param setup the arena geometry to persist
     * @throws RuntimeException if the file cannot be written
     */
    public void save(ArenaSetup setup) {
        File file = arenaFile();
        YamlConfiguration config = new YamlConfiguration();

        config.set("arena.world", setup.worldName());
        config.set("arena.center-x", setup.centerX());
        config.set("arena.center-z", setup.centerZ());
        config.set("arena.initial-size", setup.initialSize());

        try {
            config.save(file);
            this.arena = Optional.of(setup);
            log.info("Arena saved: world=<white>" + setup.worldName() +
                "</white> size=<white>" + (int) setup.initialSize() + "</white>");
        } catch (IOException e) {
            log.error("Failed to save arena.yml", e);
            throw new RuntimeException("Could not save arena configuration", e);
        }
    }

    /**
     * @return the currently loaded arena, or empty if not configured
     */
    public Optional<ArenaSetup> get() {
        return arena;
    }

    /**
     * @return true if an arena has been successfully loaded
     */
    public boolean isConfigured() {
        return arena.isPresent();
    }

    private File arenaFile() {
        return new File(plugin.getDataFolder(), FILE_NAME);
    }

}
