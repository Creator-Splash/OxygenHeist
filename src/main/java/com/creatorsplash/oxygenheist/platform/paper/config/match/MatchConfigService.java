package com.creatorsplash.oxygenheist.platform.paper.config.match;

import com.creatorsplash.oxygenheist.domain.match.config.DownedConfig;
import com.creatorsplash.oxygenheist.domain.match.config.MatchConfig;
import com.creatorsplash.oxygenheist.domain.match.config.OxygenConfig;
import com.creatorsplash.oxygenheist.domain.zone.config.ZoneConfig;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Provides the current {@link MatchConfig} and supports reloading from a Bukkit {@link FileConfiguration}
 */
public final class MatchConfigService implements Supplier<MatchConfig> {

    private volatile MatchConfig config = MatchConfig.EMPTY;

    public boolean isLoaded() { return config != MatchConfig.EMPTY; }

    @Override
    public MatchConfig get() {
        //if (!isLoaded()) throw new IllegalStateException("Cannot use MatchConfig before its loaded");
        return config;
    }

    public MatchConfig load(FileConfiguration fileConfig) {
        Objects.requireNonNull(fileConfig, "fileConfig");

        int duration = fileConfig.getInt("match.duration-seconds", 600);

        OxygenConfig oxygen = new OxygenConfig(
            fileConfig.getDouble("oxygen.max", 300),
            fileConfig.getDouble("oxygen.drain-per-tick", 0.1),
                fileConfig.getInt("oxygen.depletion-down-ticks", 200)
        );

        DownedConfig downed = new DownedConfig(
            fileConfig.getInt("downed.bleedout-seconds", 30),
            fileConfig.getInt("downed.revive-ticks", 100),
            fileConfig.getDouble("downed.revive-max-distance", 3.0),
            fileConfig.getLong("downed.intent-ttl-ticks", 5)
        );

        ZoneConfig zones = new ZoneConfig(
            fileConfig.getDouble("zones.capture-rate-per-tick", 0.05),
            fileConfig.getDouble("zones.drain-percent-per-second", 100.0 / 120.0),
            fileConfig.getInt("zones.max-drain-multiplier", 5),
            fileConfig.getDouble("zones.refill-percent-per-second", (100.0 / 120.0) * 0.5),
            fileConfig.getInt("zones.capture-oxygen-restore", 50)
        );

        MatchConfig newConfig =  new MatchConfig(duration, oxygen, downed, zones);

        this.config = newConfig;
        return newConfig;
    }

}
