package com.creatorsplash.oxygenheist.platform.paper.bootstrap.module;

import com.creatorsplash.oxygenheist.application.bridge.display.MatchDisplayService;
import com.creatorsplash.oxygenheist.application.common.Module;
import com.creatorsplash.oxygenheist.platform.paper.OxygenHeistPlugin;
import com.creatorsplash.oxygenheist.platform.paper.display.LobbyDisplayManager;
import com.creatorsplash.oxygenheist.platform.paper.display.PaperAirBarController;
import com.creatorsplash.oxygenheist.platform.paper.display.PaperMatchDisplayManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@RequiredArgsConstructor
@Accessors(fluent = true)
public final class DisplayModule implements Module {

    private final OxygenHeistPlugin plugin;
    private final ConfigModule configs;

    private MatchDisplayService displayService;
    private LobbyDisplayManager lobbyDisplayManager;
    private PaperAirBarController airBarController;

    public DisplayModule build() {
        this.airBarController = new PaperAirBarController();

        this.displayService = new PaperMatchDisplayManager(
            plugin,
            airBarController,
            configs.matchConfig(),
            configs.messageConfig()
        );

        this.lobbyDisplayManager = new LobbyDisplayManager(configs.messageConfig());

        return this;
    }

}
