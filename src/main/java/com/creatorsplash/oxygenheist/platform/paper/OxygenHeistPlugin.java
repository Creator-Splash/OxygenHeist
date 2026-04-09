package com.creatorsplash.oxygenheist.platform.paper;

import com.creatorsplash.oxygenheist.application.common.LogCenter;
import com.creatorsplash.oxygenheist.application.common.Module;
import com.creatorsplash.oxygenheist.application.match.zone.*;
import com.creatorsplash.oxygenheist.platform.paper.bootstrap.logging.GlobalLogCenter;
import com.creatorsplash.oxygenheist.platform.paper.bootstrap.module.*;
import com.creatorsplash.oxygenheist.platform.paper.command.*;
import com.creatorsplash.oxygenheist.platform.paper.config.GlobalConfig;
import com.creatorsplash.oxygenheist.platform.paper.config.GlobalConfigService;
import com.creatorsplash.oxygenheist.platform.paper.listener.*;
import lombok.Getter;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.List;

public final class OxygenHeistPlugin extends JavaPlugin {

    @Getter
    private LogCenter logCenter;

    @Getter
    private GlobalConfigService configService;

    private List<Module> modules;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.configService = new GlobalConfigService(this);
        this.logCenter = new GlobalLogCenter(configService);

        var configs = new ConfigModule(this).load();
        var display = new DisplayModule(this, configs).build();
        var gameplay = new GameplayModule(this, configs, display).build();
        var weapons = new WeaponModule(configs, gameplay).build();
        var platform = new PlatformModule(this, configs, gameplay, display, weapons).wire();

        this.modules = List.of(
           configs,
           display,
           gameplay,
           weapons,
           platform
        );

        logCenter.info("<green>Ready!");
    }

    @Override
    public void onDisable() {
        this.modules.forEach(Module::disable);

        HandlerList.unregisterAll(this);

        logCenter.info("<red>Shutdown!");
    }

    public GlobalConfig globals() {
        return this.configService.get();
    }

    /* Static Helpers */

    public static OxygenHeistPlugin instance() {
        return getPlugin(OxygenHeistPlugin.class);
    }

    public static LogCenter log() {
        return instance().getLogCenter();
    }

}
