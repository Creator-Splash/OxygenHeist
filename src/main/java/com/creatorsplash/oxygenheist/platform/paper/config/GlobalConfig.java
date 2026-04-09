package com.creatorsplash.oxygenheist.platform.paper.config;

import com.creatorsplash.oxygenheist.application.common.debug.DebugFlags;
import org.jetbrains.annotations.NotNull;

public record GlobalConfig(
    @NotNull DebugFlags debugFlags,
    boolean weaponDebugBypass
) {
}
