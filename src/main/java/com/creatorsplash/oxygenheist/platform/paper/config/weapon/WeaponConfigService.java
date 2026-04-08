package com.creatorsplash.oxygenheist.platform.paper.config.weapon;

import com.creatorsplash.oxygenheist.application.common.LogCenter;
import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads and parses {@code weapons.yml} into typed {@link WeaponTypeConfig} instances
 */
@RequiredArgsConstructor
public final class WeaponConfigService {

    private static final String FILE_NAME = "weapons.yml";

    private final JavaPlugin plugin;
    private final LogCenter log;

    private final Map<String, WeaponTypeConfig> configs = new HashMap<>();

    /**
     * Loads {@code weapons.yml} from the plugin data folder
     * <p>
     * Saves the bundled default resource if the file does not yet exist
     */
    public void load() {
        configs.clear();

        File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (!file.exists()) {
            plugin.saveResource(FILE_NAME, false);
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        for (String id : yaml.getKeys(false)) {
            ConfigurationSection section = yaml.getConfigurationSection(id);
            if (section == null) {
                log.warn("Skipping weapon entry '" + id + "' - not a valid section");
                continue;
            }

            try {
                configs.put(id, parseWeapon(id, section));
                log.debug("weapons", "Loaded weapon config: " + id);
            } catch (Exception e) {
                log.warn("Failed to parse weapon '"
                    + id + "' from weapons.yml: " + e.getMessage());
            }

            log.info("Loaded " + configs.size() + " weapon configurations");
        }
    }

    /**
     * @return the parsed config for the given weapon id, or null if not found or
     * if parsing failed
     */
    public @Nullable WeaponTypeConfig getConfig(String weaponId) {
        return configs.get(weaponId);
    }

    /**
     * @return true if the given weapon id is present and marked as enabled
     */
    public boolean isEnabled(String weaponId) {
        WeaponTypeConfig config = configs.get(weaponId);
        return config != null && config.enabled();
    }

    /* == Parser == */

    private WeaponTypeConfig parseWeapon(String id, ConfigurationSection s) {
        boolean enabled = s.getBoolean("enabled", true);

        String itemId = s.getString("item-id", id);
        int reloadFrames = s.getInt("reload-frames", 0);

        WeaponTypeConfig.AmmoConfig ammo = parseAmmo(s);
        WeaponTypeConfig.TimingConfig timing = parseTiming(s);
        WeaponTypeConfig.CombatConfig combat = parseCombat(s);
        WeaponTypeConfig.PhysicsConfig physics = parsePhysics(s);
        WeaponTypeConfig.EffectConfig effects = parseEffects(s);

        return new WeaponTypeConfig(id, enabled, itemId, reloadFrames, ammo, timing, combat, physics, effects);
    }

    private WeaponTypeConfig.AmmoConfig parseAmmo(ConfigurationSection s) {
        int maxAmmo = s.getInt("ammo", 0);
        int startAmmo = s.getInt("start-ammo", maxAmmo); // defaults to max if not set
        return new WeaponTypeConfig.AmmoConfig(maxAmmo, startAmmo);
    }

    private WeaponTypeConfig.TimingConfig parseTiming(ConfigurationSection s) {
        return new WeaponTypeConfig.TimingConfig(
            s.getLong("reload-ms", 0),
            s.getLong("shot-cooldown-ms", 0),
            s.getLong("cooldown-ms", 0),
            s.getLong("burst-cooldown-ms", 0)
        );
    }

    private WeaponTypeConfig.CombatConfig parseCombat(ConfigurationSection s) {
        return new WeaponTypeConfig.CombatConfig(
            s.getDouble("damage", 0.0),
            s.getDouble("max-range", 0.0),
            s.getInt("burst-count", 1),
            s.getDouble("damage-per-shot", 0.0),
            s.getDouble("explosion-radius", 0.0),
            s.getDouble("close-range-distance", 0.0),
            s.getDouble("close-range-bonus-damage", 0.0),
            s.getDouble("long-range-distance", 0.0),
            s.getDouble("long-range-nerf-damage", 0.0)
        );
    }

    private WeaponTypeConfig.PhysicsConfig parsePhysics(ConfigurationSection s) {
        return new WeaponTypeConfig.PhysicsConfig(
            s.getDouble("launch-speed", 0.0),
            s.getDouble("launch-y", 0.0),
            s.getDouble("melee-knockback", 0.0),
            s.getDouble("melee-knockback-y", 0.0),
            s.getDouble("cone-angle", 0.0),
            s.getDouble("cloud-radius", 0.0),
            s.getDouble("aim-spread-multiplier", 0.7)
        );
    }

    private WeaponTypeConfig.EffectConfig parseEffects(ConfigurationSection s) {
        return new WeaponTypeConfig.EffectConfig(
            s.getInt("effect-duration-ticks", 0),
            s.getInt("poison-duration-ticks", 0)
        );
    }

}
