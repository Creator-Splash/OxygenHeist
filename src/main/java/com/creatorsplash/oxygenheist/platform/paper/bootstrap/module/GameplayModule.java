package com.creatorsplash.oxygenheist.platform.paper.bootstrap.module;

import com.creatorsplash.oxygenheist.application.bridge.GameBridge;
import com.creatorsplash.oxygenheist.application.bridge.GameWorldService;
import com.creatorsplash.oxygenheist.application.bridge.StandaloneGameBridge;
import com.creatorsplash.oxygenheist.application.common.Module;
import com.creatorsplash.oxygenheist.application.common.math.FullPosition;
import com.creatorsplash.oxygenheist.application.common.math.PlayerPositionProvider;
import com.creatorsplash.oxygenheist.application.match.MatchService;
import com.creatorsplash.oxygenheist.application.match.MatchSnapshotProvider;
import com.creatorsplash.oxygenheist.application.match.Scheduler;
import com.creatorsplash.oxygenheist.application.match.combat.CombatService;
import com.creatorsplash.oxygenheist.application.match.combat.DownedService;
import com.creatorsplash.oxygenheist.application.match.combat.PlayerActionService;
import com.creatorsplash.oxygenheist.application.match.combat.revive.ReviveService;
import com.creatorsplash.oxygenheist.application.match.oxygen.PlayerOxygenService;
import com.creatorsplash.oxygenheist.application.match.team.TeamService;
import com.creatorsplash.oxygenheist.application.match.zone.*;
import com.creatorsplash.oxygenheist.domain.zone.config.ZoneDefinition;
import com.creatorsplash.oxygenheist.platform.paper.OxygenHeistPlugin;
import com.creatorsplash.oxygenheist.platform.paper.scheduler.PaperSchedulerAdapter;
import com.creatorsplash.oxygenheist.platform.paper.world.PaperGameWorldService;
import com.creatorsplash.oxygenheist.platform.paper.world.PlayerSelectionService;
import com.creatorsplash.oxygenheist.platform.paper.world.ZoneSelectionService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@RequiredArgsConstructor
@Accessors(fluent = true)
public final class GameplayModule implements Module {

    private final OxygenHeistPlugin plugin;
    private final ConfigModule configs;
    private final DisplayModule display;

    private TeamService teamService;
    private MatchService matchService;
    private PlayerSelectionService selectionService;
    private ZoneSelectionService zoneSelectionService;
    private PlayerActionService actionService;
    private CombatService combatService;
    private ReviveService reviveService;
    private MatchSnapshotProvider snapshotProvider;

    public GameplayModule build() {
        this.teamService = configs.teamConfig().load(plugin);
        this.selectionService = new PlayerSelectionService(plugin);
        this.zoneSelectionService = new ZoneSelectionService(selectionService);

        PlayerPositionProvider positionProvider = buildPositionProvider();

        DownedService downedService = new DownedService();
        this.reviveService = new ReviveService();

        ZoneService zoneService = new ZoneService(positionProvider);
        PlayerOxygenService playerOxygenService = new PlayerOxygenService(zoneService);
        CaptureService captureService = new CaptureService(playerOxygenService);
        ZonePresenceService zonePresenceService = new ZonePresenceService(positionProvider);
        ZoneOxygenService zoneOxygenService = new ZoneOxygenService(zonePresenceService);

        Scheduler scheduler = new PaperSchedulerAdapter(plugin);
        GameBridge bridge = new StandaloneGameBridge();
        GameWorldService worldService = new PaperGameWorldService(
            plugin.getServer(), configs.arenaConfig(), plugin.getLogCenter()
        );

        this.snapshotProvider = new MatchSnapshotProvider();

        ZoneProvider zoneProvider = () -> configs.arenaConfig().getZones()
            .stream()
            .map(ZoneDefinition::toRuntimeState)
            .toList();

        this.matchService = new MatchService(
            configs.globals(),
            configs.matchConfig(),
            snapshotProvider,
            display().displayService(),
            worldService,
            scheduler,
            bridge,
            downedService,
            reviveService,
            positionProvider,
            captureService,
            playerOxygenService,
            zoneOxygenService,
            zonePresenceService,
            zoneProvider,
            teamService
        );

        this.actionService = new PlayerActionService(matchService);
        this.combatService = new CombatService(matchService, reviveService);

        return this;
    }

    private PlayerPositionProvider buildPositionProvider() {
        return playerId -> {
            var player = plugin.getServer().getPlayer(playerId);
            if (player == null || !player.isOnline()) return null;
            var loc = player.getLocation();
            return new FullPosition(
                loc.getWorld().getName(),
                loc.getX(), loc.getY(), loc.getZ(),
                loc.getYaw(), loc.getPitch()
            );
        };
    }

    @Override
    public void disable() {
        selectionService.clear();
        matchService.getScheduler().onDisable();
    }
    
}

