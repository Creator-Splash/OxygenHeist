package com.creatorsplash.oxygenheist.platform.paper.config;

import com.creatorsplash.oxygenheist.application.common.debug.DebugFlags;
import com.creatorsplash.oxygenheist.platform.paper.OxygenHeistPlugin;
import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

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
        boolean weaponDebugBypass = raw.getBoolean("debug.weapon-bypass", false);

        this.config = new GlobalConfig(
            flags,
            weaponDebugBypass,
            loadWeaponSpawner(raw)
        );
    }

    /* Internals */

    private GlobalConfig.WeaponSpawnerConfig loadWeaponSpawner(FileConfiguration c) {
        return new GlobalConfig.WeaponSpawnerConfig(
            c.getInt("weapons.spawner.initial-count", 3),
            c.getInt("weapons.spawner.max-active", 8),
            c.getInt("weapons.spawner.spawn-interval-seconds", 45),
            c.getDouble("weapons.spawner.pickup-radius", 1.5),
            c.getInt("weapons.spawner.pickup-cooldown-seconds", 3)
        );
    }

    private DebugFlags loadDebugFlags(FileConfiguration config) {
        List<String> flags = config.getStringList("debug.flags");
        Set<String> enabled = new HashSet<>(flags);
        return new DebugFlags(enabled);
    }

}
