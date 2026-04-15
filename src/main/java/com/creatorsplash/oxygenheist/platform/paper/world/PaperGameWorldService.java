package com.creatorsplash.oxygenheist.platform.paper.world;

import com.creatorsplash.oxygenheist.application.bridge.GameWorldService;
import com.creatorsplash.oxygenheist.application.common.LogCenter;
import com.creatorsplash.oxygenheist.application.match.Scheduler;
import com.creatorsplash.oxygenheist.domain.match.config.MatchBorderConfig;
import com.creatorsplash.oxygenheist.domain.match.config.MatchConfig;
import com.creatorsplash.oxygenheist.platform.paper.config.ArenaConfigService;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Paper implementation for {@link GameWorldService}
 *
 * <p>Responsible for applying and resetting the world border</p>
 */
public final class PaperGameWorldService implements GameWorldService {

    private final Server server;
    private final Scheduler scheduler;
    private final ArenaConfigService arenaConfigService;
    private final LogCenter log;

    public PaperGameWorldService(
        @NotNull final Server server,
        @NotNull final Scheduler scheduler,
        @NotNull final ArenaConfigService arenaConfigService,
        @NotNull final LogCenter log
    ) {
        this.server = server;
        this.scheduler = scheduler;
        this.arenaConfigService = arenaConfigService;
        this.log = log;

        World world = resolveWorld();
        if (world == null) return;
        WorldBorder border = world.getWorldBorder();
        border.reset();
    }

    @Override
    public void onMatchStarted(MatchConfig config) {
        World world = resolveWorld();
        if (world == null) return;

        ArenaSetup arena = arenaConfigService.getArena().orElseThrow();

        scheduler.runLater(() -> {
            WorldBorder border = world.getWorldBorder();
            border.setCenter(arena.centerX(), arena.centerZ());
            border.setSize(arena.initialSize());
            border.setWarningDistance(5);
            border.setWarningTime(0);
            border.setDamageAmount(0.2);
            border.setDamageBuffer(5.0);
        }, 1L);

        log.info("Border applied: size=<white>" + (int) + arena.initialSize() + " </white>");
    }

    @Override
    public void onBorderShrinkStart(MatchConfig config) {
        World world = resolveWorld();
        if (world == null) return;

        ArenaSetup arena = arenaConfigService.getArena().orElseThrow();
        MatchBorderConfig borderConfig = config.border();

        double targetSize = arena.initialSize() * (borderConfig.shrinkSizePercent() / 100d);
        targetSize = Math.max(targetSize, borderConfig.minimumSize());
        long durationSeconds = borderConfig.shrinkDurationSeconds();

        world.getWorldBorder().setSize(targetSize, durationSeconds);

        log.debug("border", "Border shrink started: <white>" + (int) arena.initialSize()
            + "</white> -> <white>" + (int) targetSize
            + "</white> over <white>" + durationSeconds + "s</white>");
    }

    @Override
    public void onMatchEnded() {
        World world = resolveWorld();
        if (world == null) return;

        ArenaSetup arena = arenaConfigService.getArena().orElseThrow();

        WorldBorder border = world.getWorldBorder();
        border.setSize(arena.initialSize());
        border.setDamageAmount(0.0);
        border.setDamageBuffer(5.0);
        border.setWarningDistance(5);
        border.setWarningTime(0);
    }

    @Override
    public void reset() {
        World world = resolveWorld();
        if (world == null) return;

        WorldBorder border = world.getWorldBorder();
        border.reset();
    }

    /* Internals */

    private @Nullable World resolveWorld() {
        ArenaSetup arena = arenaConfigService.getArena().orElse(null);

        if (arena == null) {
            log.warn("Border operation skipped - arena is not configured");
            return null;
        }

        World world = server.getWorld(arena.worldName());

        if (world == null) {
            log.warn("Border operation skipped - world '<white>" + arena.worldName() +
                "'</white> not found on the server");
            return null;
        }

        return world;
    }

}
