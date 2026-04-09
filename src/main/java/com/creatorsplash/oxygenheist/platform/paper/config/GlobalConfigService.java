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
            weaponDebugBypass
        );
    }

    /* Internals */

    private DebugFlags loadDebugFlags(FileConfiguration config) {
        List<String> flags = config.getStringList("debug.flags");
        Set<String> enabled = new HashSet<>(flags);
        return new DebugFlags(enabled);
    }

}
