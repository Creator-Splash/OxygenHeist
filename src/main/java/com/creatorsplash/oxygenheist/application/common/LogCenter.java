package com.creatorsplash.oxygenheist.application.common;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Central logging abstraction
 */
public interface LogCenter {

    /** Prefix rendered before every message */
    @NotNull String prefix();

    /** Whether debug messages should be sent */
    boolean debugEnabled();

    /** Whether a specific debug channel is enabled */
    boolean debugEnabled(@NotNull String key);

    /* == Core == */

    void sendLog(@NotNull String message);

    default void info(@NotNull String msg) {
        sendLog(prefix() + " <dark_gray>|</dark_gray> <gray>" + msg);
    }

    default void warn(@NotNull String msg) {
        sendLog(prefix() + " <dark_gray>|</dark_gray> <yellow>" + msg);
    }

    default void error(@NotNull String msg) {
        error(msg, null);
    }

    default void error(@NotNull String msg, @Nullable Throwable t) {
        String message = t == null ? msg : msg + ": " + t.getMessage();
        sendLog(prefix() + " <dark_gray>|</dark_gray> <red>" + message);
    }

    /* Debug */

    default void debug(@NotNull String msg) {
        if (debugEnabled()) {
            sendDebug("global", msg);
        }
    }

    default void debug(@NotNull String key, @NotNull String msg) {
        if (debugEnabled(key)) {
            sendDebug(key, msg);
        }
    }

    default void sendDebug(@NotNull String key, @NotNull String msg) {
        sendLog(
            "<dark_gray>[<yellow>Debug</yellow>:<aqua>" + key + "</aqua>]</dark_gray> "
                + prefix() + " <dark_gray>|</dark_gray> <gray>" + msg
        );
    }

}
