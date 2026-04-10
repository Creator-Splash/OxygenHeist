package com.creatorsplash.oxygenheist.platform.paper.config;

import com.creatorsplash.oxygenheist.application.common.debug.DebugFlags;
import org.jetbrains.annotations.NotNull;

public record GlobalConfig(
    @NotNull DebugFlags debugFlags,
    boolean weaponDebugBypass,
    WeaponSpawnerConfig weaponSpawner
) {

    public record WeaponSpawnerConfig(
        int initialCount,
        int maxActive,
        int spawnIntervalSeconds,
        double pickupRadius,
        int pickupCooldownSeconds
    ) {
        public static WeaponSpawnerConfig defaults() {
            return new WeaponSpawnerConfig(3, 8, 45, 1.5, 3);
        }
    }

}
