package com.creatorsplash.oxygenheist.platform.paper.config;

import com.creatorsplash.oxygenheist.application.common.debug.DebugFlags;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public record GlobalConfig(
    @NotNull DebugFlags debugFlags,
    ItemProvider itemProvider,
    boolean weaponDebugBypass,
    boolean physicalAmmoDisplay,
    WeaponSpawnerConfig weaponSpawner
) {

    public enum ItemProvider {
        NEXO, ITEMSADDER, CRAFTENGINE, ORAXEN, VANILLA
    }

    public record WeaponSpawnerConfig(
        int maxPerPlayer,
        int initialCount,
        int minimumOnField,
        int maximumOnField,
        int surfaceScanStep,
        int minSpawnY,
        int maxSpawnY,
        double pickupRadius,
        int pickupCooldownSeconds,
        Set<Material> allowedSurfaceBlocks
    ) {
        public static WeaponSpawnerConfig defaults() {
            return new WeaponSpawnerConfig(
                3,
                10 * 8,
                3 * 8,
                10 * 8,
                3,
                64,
                70,
                1.5,
                3,
                Set.of()
            );
        }
    }

}
