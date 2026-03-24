package com.creatorsplash.oxygenheist.platform.paper.bootstrap.logging;

import com.creatorsplash.oxygenheist.application.common.LogCenter;
import com.creatorsplash.oxygenheist.application.common.debug.DebugFlags;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public final class GlobalLogCenter implements LogCenter {

    private final DebugFlags flags;

    @Override
    public @NotNull String prefix() {
        return "<gold>[<aqua>OxygenHeist</aqua>]</gold>";
    }

    @Override
    public boolean debugEnabled() {
        return flags.enabled("global");
    }

    @Override
    public boolean debugEnabled(@NotNull String key) {
        return debugEnabled() && flags.enabled(key);
    }

    @Override
    public void sendLog(@NotNull String message) {
        Bukkit.getConsoleSender().sendRichMessage(message);
    }

}
