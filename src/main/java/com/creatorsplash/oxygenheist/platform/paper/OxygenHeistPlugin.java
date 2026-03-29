package com.creatorsplash.oxygenheist.platform.paper;

import com.creatorsplash.oxygenheist.application.bridge.GameBridge;
import com.creatorsplash.oxygenheist.application.bridge.GameWorldService;
import com.creatorsplash.oxygenheist.application.bridge.StandaloneGameBridge;
import com.creatorsplash.oxygenheist.application.bridge.StandaloneGameWorldService;
import com.creatorsplash.oxygenheist.application.bridge.display.DefaultMatchDisplayService;
import com.creatorsplash.oxygenheist.application.bridge.display.MatchDisplayGateway;
import com.creatorsplash.oxygenheist.application.bridge.display.MatchDisplayService;
import com.creatorsplash.oxygenheist.application.common.LogCenter;
import com.creatorsplash.oxygenheist.application.common.debug.DebugFlags;
import com.creatorsplash.oxygenheist.application.common.math.FullPosition;
import com.creatorsplash.oxygenheist.application.common.math.PlayerPositionProvider;
import com.creatorsplash.oxygenheist.application.match.MatchSnapshotProvider;
import com.creatorsplash.oxygenheist.application.match.combat.CombatService;
import com.creatorsplash.oxygenheist.application.match.combat.DownedService;
import com.creatorsplash.oxygenheist.application.match.MatchService;
import com.creatorsplash.oxygenheist.application.match.Scheduler;
import com.creatorsplash.oxygenheist.application.match.combat.PlayerActionService;
import com.creatorsplash.oxygenheist.application.match.combat.revive.ReviveService;
import com.creatorsplash.oxygenheist.application.match.oxygen.PlayerOxygenService;
import com.creatorsplash.oxygenheist.application.match.zone.*;
import com.creatorsplash.oxygenheist.domain.zone.config.ZoneDefinition;
import com.creatorsplash.oxygenheist.platform.paper.bootstrap.CommandRegistrar;
import com.creatorsplash.oxygenheist.platform.paper.bootstrap.logging.GlobalLogCenter;
import com.creatorsplash.oxygenheist.platform.paper.command.CommandHandler;
import com.creatorsplash.oxygenheist.platform.paper.command.DebugCommands;
import com.creatorsplash.oxygenheist.platform.paper.command.GameCommands;
import com.creatorsplash.oxygenheist.platform.paper.command.SetupCommands;
import com.creatorsplash.oxygenheist.platform.paper.config.ArenaConfigService;
import com.creatorsplash.oxygenheist.platform.paper.config.match.MatchConfigService;
import com.creatorsplash.oxygenheist.platform.paper.display.PaperAirBarController;
import com.creatorsplash.oxygenheist.platform.paper.display.PaperMatchDisplayGateway;
import com.creatorsplash.oxygenheist.platform.paper.display.placeholder.OxygenHeistPlaceholderExpansion;
import com.creatorsplash.oxygenheist.platform.paper.listener.AirChangeListener;
import com.creatorsplash.oxygenheist.platform.paper.listener.CombatListener;
import com.creatorsplash.oxygenheist.platform.paper.listener.PlayerRestrictionListener;
import com.creatorsplash.oxygenheist.platform.paper.listener.ReviveListener;
import com.creatorsplash.oxygenheist.platform.paper.scheduler.PaperSchedulerAdapter;
import com.creatorsplash.oxygenheist.platform.paper.world.PaperGameWorldService;
import com.creatorsplash.oxygenheist.platform.paper.world.PlayerSelectionService;
import com.creatorsplash.oxygenheist.platform.paper.world.ZoneSelectionService;
import lombok.Getter;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Set;

public final class OxygenHeistPlugin extends JavaPlugin {

    @Getter
    private MatchService matchService;

    @Getter PlayerSelectionService selectionService;

    @Getter
    private LogCenter logCenter;

    private CommandRegistrar commandRegistrar;

    @Override
    public void onEnable() {

        /* == Configs == */

        saveDefaultConfig();

        DebugFlags debugFlags = new DebugFlags(
            Set.of("all") // todo
        );

        this.logCenter = new GlobalLogCenter(debugFlags);

        MatchConfigService matchConfigService = new MatchConfigService();
        matchConfigService.load(getConfig());

        ArenaConfigService arenaConfigService = new ArenaConfigService(this, this.logCenter);
        arenaConfigService.load();

        /* == Gameplay Services == */

        Scheduler scheduler = new PaperSchedulerAdapter(this);

        GameBridge bridge = new StandaloneGameBridge();
        GameWorldService worldService = new PaperGameWorldService(
            getServer(), arenaConfigService, this.logCenter);

        MatchSnapshotProvider snapshotProvider = new MatchSnapshotProvider();

        this.selectionService = new PlayerSelectionService(this);
        ZoneSelectionService zoneSelectionService = new ZoneSelectionService(selectionService);

        PlayerPositionProvider playerPositionProvider = playerId -> {
            var player = getServer().getPlayer(playerId);
            if (player == null || !player.isOnline()) return null;
            var loc = player.getLocation();
            return new FullPosition(loc.getWorld().getName(),
                loc.getX(), loc.getY(), loc.getZ(),
                loc.getYaw(), loc.getPitch()
            );
        };

        /* Gameplay Services */

        DownedService downedService = new DownedService();
        ReviveService reviveService = new ReviveService();

        ZoneService zoneService = new ZoneService(playerPositionProvider);
        PlayerOxygenService playerOxygenService =
            new PlayerOxygenService(zoneService);
        CaptureService captureService =
            new CaptureService(playerOxygenService);
        ZonePresenceService zonePresenceService =
            new ZonePresenceService(playerPositionProvider);
        ZoneOxygenService zoneOxygenService =
            new ZoneOxygenService(zonePresenceService);

        /* Display Services */

        PaperAirBarController airBarController = new PaperAirBarController();

        MatchDisplayGateway displayGateway = new PaperMatchDisplayGateway(
            this,
            airBarController
        );
        MatchDisplayService displayService = new DefaultMatchDisplayService(displayGateway);

        ZoneProvider zoneProvider = () -> arenaConfigService.getZones()
            .stream()
            .map(ZoneDefinition::toRuntimeState)
            .toList();

        this.matchService = new MatchService(
            matchConfigService,
            snapshotProvider,
            displayService,
            worldService,
            scheduler,
            bridge,
            debugFlags,
            downedService,
            reviveService,
            playerPositionProvider,
            captureService,
            playerOxygenService,
            zoneOxygenService,
            zonePresenceService,
            zoneProvider
        );

        PlayerActionService actionService = new PlayerActionService(this.matchService);
        CombatService combatService = new CombatService(
            this.matchService, downedService, reviveService);

        /* == Listeners == */

        registerListeners(
            new CombatListener(combatService, actionService),
            new ReviveListener(this.matchService, reviveService, actionService),
            new PlayerRestrictionListener(actionService),
            new AirChangeListener(airBarController)
        );

        /* == Commands == */

        this.commandRegistrar = new CommandRegistrar(this);
        registerCommands(
            new GameCommands(this.matchService),
            new DebugCommands(this.matchService),
            new SetupCommands(
                this.logCenter,
                this.selectionService,
                zoneSelectionService,
                arenaConfigService
            )
        );

        /* == PAPI == */

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new OxygenHeistPlaceholderExpansion(snapshotProvider).register();
        }

        logCenter.info("<green>Ready!");
    }

    @Override
    public void onDisable() {
        this.selectionService.clear();
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
