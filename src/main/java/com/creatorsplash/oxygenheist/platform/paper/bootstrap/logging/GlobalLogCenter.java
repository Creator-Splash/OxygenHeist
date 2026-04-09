package com.creatorsplash.oxygenheist.platform.paper.bootstrap.logging;

import com.creatorsplash.oxygenheist.application.common.LogCenter;
import com.creatorsplash.oxygenheist.application.common.debug.DebugFlags;
import com.creatorsplash.oxygenheist.platform.paper.config.GlobalConfigService;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public final class GlobalLogCenter implements LogCenter {

    private final GlobalConfigService globals;

    @Override
    public @NotNull String prefix() {
        return "<gold>[<aqua>OxygenHeist</aqua>]</gold>";
    }

    @Override
    public boolean debugEnabled() {
        return globals.get().debugFlags().enabled("global");
    }

    @Override
    public boolean debugEnabled(@NotNull String key) {
        return debugEnabled() && globals.get().debugFlags().enabled(key);
    }

    @Override
    public void sendLog(@NotNull String message) {
        Bukkit.getConsoleSender().sendRichMessage(message);
    }

}
