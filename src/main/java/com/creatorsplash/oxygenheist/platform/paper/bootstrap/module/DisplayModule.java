package com.creatorsplash.oxygenheist.platform.paper.bootstrap.module;

import com.creatorsplash.oxygenheist.application.bridge.display.MatchDisplayService;
import com.creatorsplash.oxygenheist.application.common.Module;
import com.creatorsplash.oxygenheist.platform.paper.OxygenHeistPlugin;
import com.creatorsplash.oxygenheist.platform.paper.display.LobbyDisplayService;
import com.creatorsplash.oxygenheist.platform.paper.display.PaperAirBarController;
import com.creatorsplash.oxygenheist.platform.paper.display.PaperMatchDisplayService;
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
    private LobbyDisplayService lobbyDisplayService;
    private PaperAirBarController airBarController;

    public DisplayModule build() {
        this.airBarController = new PaperAirBarController();

        this.displayService = new PaperMatchDisplayService(
            plugin,
            airBarController,
            configs.messageConfig()
        );

        this.lobbyDisplayService = new LobbyDisplayService(configs.messageConfig());

        return this;
    }

}
