package com.creatorsplash.oxygenheist.platform.paper;

import com.creatorsplash.oxygenheist.application.bridge.StandaloneGameBridge;
import com.creatorsplash.oxygenheist.application.match.MatchService;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

public class OxygenHeistPlugin extends JavaPlugin {

    @Getter
    private MatchService matchService;

    @Override
    public void onEnable() {
        this.matchService = new MatchService(new StandaloneGameBridge());
    }

    @Override
    public void onDisable() {
        // TODO
    }

}
