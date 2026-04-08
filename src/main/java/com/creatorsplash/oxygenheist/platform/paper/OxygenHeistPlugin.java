package com.creatorsplash.oxygenheist.platform.paper;

import com.creatorsplash.oxygenheist.application.bridge.GameBridge;
import com.creatorsplash.oxygenheist.application.bridge.GameWorldService;
import com.creatorsplash.oxygenheist.application.bridge.StandaloneGameBridge;
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
import com.creatorsplash.oxygenheist.application.match.team.TeamService;
import com.creatorsplash.oxygenheist.application.match.zone.*;
import com.creatorsplash.oxygenheist.domain.zone.config.ZoneDefinition;
import com.creatorsplash.oxygenheist.platform.paper.bootstrap.CommandRegistrar;
import com.creatorsplash.oxygenheist.platform.paper.bootstrap.logging.GlobalLogCenter;
import com.creatorsplash.oxygenheist.platform.paper.command.*;
import com.creatorsplash.oxygenheist.platform.paper.config.ArenaConfigService;
import com.creatorsplash.oxygenheist.platform.paper.config.match.MatchConfigService;
import com.creatorsplash.oxygenheist.platform.paper.config.message.MessageConfigService;
import com.creatorsplash.oxygenheist.platform.paper.config.team.TeamConfigService;
import com.creatorsplash.oxygenheist.platform.paper.config.weapon.WeaponConfigService;
import com.creatorsplash.oxygenheist.platform.paper.display.PaperAirBarController;
import com.creatorsplash.oxygenheist.platform.paper.display.PaperMatchDisplayService;
import com.creatorsplash.oxygenheist.platform.paper.display.placeholder.OxygenHeistPlaceholderExpansion;
import com.creatorsplash.oxygenheist.platform.paper.listener.*;
import com.creatorsplash.oxygenheist.platform.paper.scheduler.PaperSchedulerAdapter;
import com.creatorsplash.oxygenheist.platform.paper.weapon.WeaponEffectsState;
import com.creatorsplash.oxygenheist.platform.paper.weapon.WeaponProjectileTracker;
import com.creatorsplash.oxygenheist.platform.paper.weapon.WeaponRegistry;
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

    @Getter
    PlayerSelectionService selectionService;

    @Getter
    TeamService teamService;

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

        WeaponConfigService weaponConfigService = new WeaponConfigService(this, this.logCenter);
        weaponConfigService.load();

        MessageConfigService messageConfigService = new MessageConfigService(this);
        messageConfigService.load();

        TeamConfigService teamConfigService = new TeamConfigService();
        TeamService teamService = teamConfigService.load(this);

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

        DownedService downedService = new DownedService();
        ReviveService reviveService = new ReviveService();

        ZoneService zoneService = new ZoneService(playerPositionProvider);
        PlayerOxygenService playerOxygenService = new PlayerOxygenService(zoneService);
        CaptureService captureService = new CaptureService(playerOxygenService);
        ZonePresenceService zonePresenceService = new ZonePresenceService(playerPositionProvider);
        ZoneOxygenService zoneOxygenService = new ZoneOxygenService(zonePresenceService);

        /* == Display Services == */

        PaperAirBarController airBarController = new PaperAirBarController();

        MatchDisplayService displayService = new PaperMatchDisplayService(
            this,
            airBarController,
            messageConfigService
        );

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
            zoneProvider,
            teamService
        );

        PlayerActionService actionService = new PlayerActionService(this.matchService);
        CombatService combatService = new CombatService(this.matchService, reviveService);

        /* == Weapons == */

        WeaponProjectileTracker projectileTracker = new WeaponProjectileTracker();
        WeaponEffectsState effectsState = new WeaponEffectsState();
        WeaponRegistry weaponRegistry = new WeaponRegistry();

        // todo register weapons here

        /* == Listeners == */

        registerListeners(
            new CombatListener(combatService, actionService),
            new ReviveListener(this.matchService, reviveService, actionService),
            new PlayerRestrictionListener(actionService),
            new AirChangeListener(airBarController),
            new WeaponListener(weaponRegistry, projectileTracker, effectsState, matchService, actionService)
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
            ),
            new ReloadCommands(
                this,
                matchConfigService,
                messageConfigService,
                this.logCenter
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

    /* Static Helpers */

    public static OxygenHeistPlugin instance() {
        return getPlugin(OxygenHeistPlugin.class);
    }

    public static LogCenter log() {
        return instance().getLogCenter();
    }

    /* Internal Helpers */

    private void registerCommands(CommandHandler... handlers) {
        Arrays.stream(handlers).forEach(handler ->
            this.commandRegistrar.registerAnnotated(handler));
    }

    private void registerListeners(Listener... listeners) {
        Arrays.stream(listeners).forEach(listener ->
            getServer().getPluginManager().registerEvents(listener, this));
    }

}
