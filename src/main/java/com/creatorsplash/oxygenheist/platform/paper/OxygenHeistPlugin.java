package com.creatorsplash.oxygenheist.platform.paper;

import com.creatorsplash.oxygenheist.application.bridge.GameBridge;
import com.creatorsplash.oxygenheist.application.bridge.StandaloneGameBridge;
import com.creatorsplash.oxygenheist.application.common.LogCenter;
import com.creatorsplash.oxygenheist.application.common.debug.DebugFlags;
import com.creatorsplash.oxygenheist.application.common.math.FullPosition;
import com.creatorsplash.oxygenheist.application.common.math.PlayerPositionProvider;
import com.creatorsplash.oxygenheist.application.match.combat.CombatService;
import com.creatorsplash.oxygenheist.application.match.combat.DownedService;
import com.creatorsplash.oxygenheist.application.match.MatchService;
import com.creatorsplash.oxygenheist.application.match.Scheduler;
import com.creatorsplash.oxygenheist.application.match.combat.PlayerActionService;
import com.creatorsplash.oxygenheist.application.match.combat.revive.ReviveService;
import com.creatorsplash.oxygenheist.application.match.oxygen.PlayerOxygenService;
import com.creatorsplash.oxygenheist.application.match.zone.CaptureService;
import com.creatorsplash.oxygenheist.application.match.zone.ZoneOxygenService;
import com.creatorsplash.oxygenheist.application.match.zone.ZonePresenceService;
import com.creatorsplash.oxygenheist.application.match.zone.ZoneService;
import com.creatorsplash.oxygenheist.platform.paper.bootstrap.CommandRegistrar;
import com.creatorsplash.oxygenheist.platform.paper.bootstrap.logging.GlobalLogCenter;
import com.creatorsplash.oxygenheist.platform.paper.command.CommandHandler;
import com.creatorsplash.oxygenheist.platform.paper.command.DebugCommands;
import com.creatorsplash.oxygenheist.platform.paper.command.GameCommands;
import com.creatorsplash.oxygenheist.platform.paper.listener.CombatListener;
import com.creatorsplash.oxygenheist.platform.paper.listener.PlayerRestrictionListener;
import com.creatorsplash.oxygenheist.platform.paper.listener.ReviveListener;
import com.creatorsplash.oxygenheist.platform.paper.scheduler.PaperSchedulerAdapter;
import lombok.Getter;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Set;

public final class OxygenHeistPlugin extends JavaPlugin {

    @Getter
    private MatchService matchService;

    @Getter
    private LogCenter logCenter;

    private CommandRegistrar commandRegistrar;

    @Override
    public void onEnable() {

        /* Configs TODO */

        DebugFlags debugFlags = new DebugFlags(
            Set.of("all") // todo
        );

        /* Services */
        this.logCenter = new GlobalLogCenter(debugFlags);

        Scheduler scheduler = new PaperSchedulerAdapter(this);
        DownedService downedService = new DownedService();
        ReviveService reviveService = new ReviveService();

        GameBridge bridge = new StandaloneGameBridge();

        PlayerPositionProvider playerPositionProvider = playerId -> {
            var player = getServer().getPlayer(playerId);
            if (player == null || !player.isOnline()) return null;
            var loc = player.getLocation();
            return new FullPosition(loc.getWorld().getName(),
                loc.getX(), loc.getY(), loc.getZ(),
                loc.getYaw(), loc.getPitch()
            );
        };

        ZoneService zoneService = new ZoneService(playerPositionProvider);
        PlayerOxygenService playerOxygenService =
            new PlayerOxygenService(0.1, zoneService);
        CaptureService captureService =
            new CaptureService(playerOxygenService, 1, 1);
        ZonePresenceService zonePresenceService =
            new ZonePresenceService(playerPositionProvider);
        ZoneOxygenService zoneOxygenService =
            new ZoneOxygenService(zonePresenceService);

        this.matchService = new MatchService(
            scheduler,
            bridge,
            debugFlags,
            downedService,
            reviveService,
            playerPositionProvider,
            captureService,
            zoneOxygenService,
            playerOxygenService,
            zonePresenceService
        );

        PlayerActionService actionService = new PlayerActionService(this.matchService);
        CombatService combatService = new CombatService(
            this.matchService, downedService, reviveService);

        /* Listeners */

        registerListeners(
            new CombatListener(combatService, actionService),
            new ReviveListener(this.matchService, reviveService, actionService),
            new PlayerRestrictionListener(actionService)
        );

        /* Commands */

        this.commandRegistrar = new CommandRegistrar(this);
        registerCommands(
            new GameCommands(this.matchService),
            new DebugCommands(this.matchService)
        );

        logCenter.info("<green>Ready!");
    }

    @Override
    public void onDisable() {
        this.matchService.getScheduler().onDisable();

        HandlerList.unregisterAll(this);

        logCenter.info("<red>Shutdown!");
    }

    public static OxygenHeistPlugin instance() {
        return getPlugin(OxygenHeistPlugin.class);
    }

    public static LogCenter log() {
        return instance().getLogCenter();
    }

    /* Helpers */

    private void registerCommands(CommandHandler... handlers) {
        Arrays.stream(handlers).forEach(handler ->
            this.commandRegistrar.registerAnnotated(handler));
    }

    private void registerListeners(Listener... listeners) {
        Arrays.stream(listeners).forEach(listener ->
            getServer().getPluginManager().registerEvents(listener, this));
    }

}
