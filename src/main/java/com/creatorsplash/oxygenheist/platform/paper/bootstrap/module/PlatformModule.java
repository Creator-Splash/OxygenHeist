package com.creatorsplash.oxygenheist.platform.paper.bootstrap.module;

import com.creatorsplash.oxygenheist.application.common.Module;
import com.creatorsplash.oxygenheist.application.match.MatchService;
import com.creatorsplash.oxygenheist.platform.paper.OxygenHeistPlugin;
import com.creatorsplash.oxygenheist.platform.paper.bootstrap.CommandRegistrar;
import com.creatorsplash.oxygenheist.platform.paper.command.*;
import com.creatorsplash.oxygenheist.platform.paper.display.ZoneDisplayManager;
import com.creatorsplash.oxygenheist.platform.paper.display.placeholder.OxygenHeistPlaceholderExpansion;
import com.creatorsplash.oxygenheist.platform.paper.listener.*;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

public record PlatformModule(
    OxygenHeistPlugin plugin,
    ConfigModule configs,
    GameplayModule gameplay,
    DisplayModule display,
    WeaponModule weapons
) implements Module {

    public PlatformModule wire() {
        registerListeners();
        registerCommands();
        registerPlaceholders();
        registerLifecycles();

        return this;
    }

    private void registerLifecycles() {
        MatchService matchService = gameplay.matchService();

        matchService.registerLifecycle(weapons().weaponRegistry());
        matchService.registerLifecycle(weapons().projectileTracker());
        matchService.registerLifecycle(weapons().effectsState());
        matchService.registerLifecycle(weapons().hideService());
        matchService.registerLifecycle(weapons().dropService());

        ZoneDisplayManager zoneDisplayManager = new ZoneDisplayManager(
            plugin.getServer(),
            gameplay.teamService(),
            configs.arenaConfig(),
            configs.messageConfig(),
            gameplay.snapshotProvider(),
            gameplay.scheduler(),
            plugin.getLogCenter()
        );
        matchService.registerLifecycle(zoneDisplayManager);

        matchService.registerLifecycle(gameplay.reviveService());
        matchService.registerLifecycle(gameplay.downedService());

        matchService.registerLifecycle(display.matchDisplayManager());
        matchService.registerLifecycle(display.downedDisplayManager());
    }

    private void registerListeners() {
        register(
            weapons().dropService(),
            gameplay().selectionService(),
            new CombatListener(gameplay.combatService(), gameplay.actionService()),
            new ReviveListener(gameplay.matchService(), gameplay.reviveService(), gameplay.actionService()),
            new PlayerRestrictionListener(gameplay.actionService()),
            new WeaponListener(
                configs.globals(),
                weapons.weaponRegistry(),
                weapons.projectileTracker(),
                weapons.effectsState(),
                gameplay.matchService(),
                gameplay.actionService()
            ),
            new TeamListener(
                gameplay.scheduler(),
                gameplay.teamService(),
                gameplay.matchService(),
                configs.messageConfig(),
                display.lobbyDisplayManager()
            ),
            new MatchJoinListener(gameplay.matchService(), display.matchDisplayManager()),
            new AirChangeListener(display.airBarController())
        );

        // Register weapon listeners
        register(weapons.listeners().toArray(new Listener[0]));
    }

    private void registerCommands() {
        CommandRegistrar registrar = new CommandRegistrar(plugin);

        registrar.registerAnnotated(new GameCommands(
            gameplay.matchService(),
            configs.arenaConfig(),
            gameplay.teamService()
        ));
        registrar.registerAnnotated(new DebugCommands(
            gameplay.matchService(),
            gameplay.gamePlayerService(),
            weapons.weaponRegistry()
        ));
        registrar.registerAnnotated(new SetupCommands(
            plugin.getLogCenter(),
            gameplay.selectionService(),
            gameplay.zoneSelectionService(),
            configs.arenaConfig()
        ));
        registrar.registerAnnotated(new ReloadCommands(
            plugin,
            configs.matchConfig(),
            configs.messageConfig(),
            plugin.getLogCenter()
        ));
        registrar.registerAnnotated(new TeamCommands(
            plugin,
            gameplay.teamService(),
            gameplay.configs().teamConfig(),
            display.lobbyDisplayManager()
        ));
    }

    private void registerPlaceholders() {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new OxygenHeistPlaceholderExpansion(
                plugin,
                gameplay.teamService(),
                gameplay.snapshotProvider()
            ).register();
        }
    }

    private void register(Listener... listeners) {
        for (Listener l : listeners) {
            plugin.getServer().getPluginManager().registerEvents(l, plugin);
        }
    }

}
