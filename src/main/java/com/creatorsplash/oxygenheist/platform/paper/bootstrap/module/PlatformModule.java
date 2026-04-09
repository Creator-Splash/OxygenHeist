package com.creatorsplash.oxygenheist.platform.paper.bootstrap.module;

import com.creatorsplash.oxygenheist.platform.paper.OxygenHeistPlugin;
import com.creatorsplash.oxygenheist.platform.paper.bootstrap.CommandRegistrar;
import com.creatorsplash.oxygenheist.platform.paper.command.DebugCommands;
import com.creatorsplash.oxygenheist.platform.paper.command.GameCommands;
import com.creatorsplash.oxygenheist.platform.paper.command.ReloadCommands;
import com.creatorsplash.oxygenheist.platform.paper.command.SetupCommands;
import com.creatorsplash.oxygenheist.platform.paper.display.placeholder.OxygenHeistPlaceholderExpansion;
import com.creatorsplash.oxygenheist.platform.paper.listener.*;
import org.bukkit.event.Listener;

public record PlatformModule(
    OxygenHeistPlugin plugin,
    ConfigModule configs,
    GameplayModule gameplay,
    DisplayModule display,
    WeaponModule weapons
) {

    public PlatformModule wire() {
        registerListeners();
        registerCommands();
        registerPlaceholders();

        return this;
    }

    private void registerListeners() {
        register(
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
            new TeamListener(gameplay.matchService().getScheduler(), gameplay.teamService())
            // TODO zone listener
        );
    }

    private void registerCommands() {
        CommandRegistrar registrar = new CommandRegistrar(plugin);

        registrar.registerAnnotated(new GameCommands(gameplay.matchService()));
        registrar.registerAnnotated(new DebugCommands(gameplay.matchService(), weapons.weaponRegistry()));
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
    }

    private void registerPlaceholders() {
        if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new OxygenHeistPlaceholderExpansion(gameplay.snapshotProvider()).register();
        }
    }

    private void register(Listener... listeners) {
        for (Listener l : listeners) {
            plugin.getServer().getPluginManager().registerEvents(l, plugin);
        }
    }

}
