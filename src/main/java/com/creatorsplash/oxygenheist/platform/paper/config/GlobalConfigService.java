package com.creatorsplash.oxygenheist.platform.paper.config;

import com.creatorsplash.oxygenheist.application.common.debug.DebugFlags;
import com.creatorsplash.oxygenheist.platform.paper.OxygenHeistPlugin;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Loads {@link GlobalConfig} from the plugin's {@code config.yml}
 */
@RequiredArgsConstructor
public final class GlobalConfigService implements Supplier<GlobalConfig> {

    private final OxygenHeistPlugin plugin;

    private volatile GlobalConfig config;

    /* Get */

    @Override
    public GlobalConfig get() {
        return config;
    }

    /* Load */

    public void load() {
        FileConfiguration raw = plugin.getConfig();

        DebugFlags flags = loadDebugFlags(raw);
        String itemProviderRaw = raw.getString("weapons.item-provider", "nexo");
        GlobalConfig.ItemProvider itemProvider;
        try {
            itemProvider = GlobalConfig.ItemProvider.valueOf(itemProviderRaw.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            itemProvider = GlobalConfig.ItemProvider.VANILLA;
        }

        boolean weaponDebugBypass = raw.getBoolean("debug.weapon-bypass", false);
        boolean physicalAmmoDisplay = raw.getBoolean("weapons.physical-ammo-display", false);

        this.config = new GlobalConfig(
            flags,
            itemProvider,
            weaponDebugBypass,
            physicalAmmoDisplay,
            loadWeaponSpawner(raw)
        );
    }

    /* Internals */

    private GlobalConfig.WeaponSpawnerConfig loadWeaponSpawner(FileConfiguration c) {
        Set<Material> allowedBlocks = c.getStringList("weapons.spawner.allowed-surface-blocks")
            .stream()
            .map(s -> {
                try { return Material.valueOf(s.toUpperCase()); }
                catch (IllegalArgumentException e) {
                    plugin.getLogCenter().warn("Unknown material in weapon spawner whitelist: " + s);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        return new GlobalConfig.WeaponSpawnerConfig(
            c.getInt("weapons.spawner.max-per-player", 5),
            c.getInt("weapons.spawner.initial-count", 12),
            c.getInt("weapons.spawner.minimum-on-field", 6),
            c.getInt("weapons.spawner.maximum-on-field", 16),
            c.getInt("weapons.spawner.surface-scan-step", 3),
            c.getInt("weapons.spawner.min-spawn-y", 0),
            c.getInt("weapons.spawner.max-spawn-y", 320),
            c.getDouble("weapons.spawner.pickup-radius", 1.5),
            c.getInt("weapons.spawner.pickup-cooldown-seconds", 3),
            allowedBlocks
        );
    }

    private DebugFlags loadDebugFlags(FileConfiguration config) {
        List<String> flags = config.getStringList("debug.flags");
        Set<String> enabled = new HashSet<>(flags);
        return new DebugFlags(enabled);
    }

}
