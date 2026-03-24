package com.creatorsplash.oxygenheist.platform.paper.bootstrap.logging;

import com.creatorsplash.oxygenheist.application.common.LogCenter;
import com.creatorsplash.oxygenheist.application.common.debug.DebugFlags;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

/**
 * Match-scoped logger
 */
@RequiredArgsConstructor
public final class MatchLogCenter implements LogCenter {

    private final String matchId;
    private final DebugFlags flags;

    @Override
    public @NotNull String prefix() {
        return "<gold>[<aqua>Oxygen</aqua>]</gold> <dark_gray>»</dark_gray> <aqua>(match:" + matchId + ")</aqua>";
    }

    @Override
    public boolean debugEnabled() {
        return flags.enabled("global") && flags.enabled("match");
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
