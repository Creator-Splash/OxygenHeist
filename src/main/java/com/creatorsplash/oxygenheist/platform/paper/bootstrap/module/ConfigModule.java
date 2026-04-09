package com.creatorsplash.oxygenheist.platform.paper.bootstrap.module;

import com.creatorsplash.oxygenheist.application.common.Module;
import com.creatorsplash.oxygenheist.platform.paper.OxygenHeistPlugin;
import com.creatorsplash.oxygenheist.platform.paper.config.ArenaConfigService;
import com.creatorsplash.oxygenheist.platform.paper.config.GlobalConfigService;
import com.creatorsplash.oxygenheist.platform.paper.config.match.MatchConfigService;
import com.creatorsplash.oxygenheist.platform.paper.config.message.MessageConfigService;
import com.creatorsplash.oxygenheist.platform.paper.config.team.TeamConfigService;
import com.creatorsplash.oxygenheist.platform.paper.config.weapon.WeaponConfigService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@RequiredArgsConstructor
@Accessors(fluent = true)
public final class ConfigModule implements Module {

    private final OxygenHeistPlugin plugin;
    private final GlobalConfigService globals;

    private MatchConfigService matchConfig;
    private ArenaConfigService arenaConfig;
    private WeaponConfigService weaponConfig;
    private MessageConfigService messageConfig;
    private TeamConfigService teamConfig;

    public ConfigModule load() {
        this.matchConfig = new MatchConfigService();
        this.arenaConfig = new ArenaConfigService(plugin, plugin.getLogCenter());
        this.weaponConfig = new WeaponConfigService(plugin, plugin.getLogCenter());
        this.messageConfig = new MessageConfigService(plugin);
        this.teamConfig = new TeamConfigService();

        matchConfig.load(plugin.getConfig());
        arenaConfig.load();
        weaponConfig.load();
        messageConfig.load();

        return this;
    }

}
