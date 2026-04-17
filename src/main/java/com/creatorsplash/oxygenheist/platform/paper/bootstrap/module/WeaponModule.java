package com.creatorsplash.oxygenheist.platform.paper.bootstrap.module;

import com.creatorsplash.oxygenheist.application.common.LogCenter;
import com.creatorsplash.oxygenheist.application.common.Module;
import com.creatorsplash.oxygenheist.application.match.Scheduler;
import com.creatorsplash.oxygenheist.platform.paper.OxygenHeistPlugin;
import com.creatorsplash.oxygenheist.platform.paper.config.weapon.WeaponTypeConfig;
import com.creatorsplash.oxygenheist.platform.paper.weapon.WeaponEffectsState;
import com.creatorsplash.oxygenheist.platform.paper.weapon.WeaponRegistry;
import com.creatorsplash.oxygenheist.platform.paper.weapon.handler.WeaponHandler;
import com.creatorsplash.oxygenheist.platform.paper.weapon.handler.impl.ClawCannonHandler;
import com.creatorsplash.oxygenheist.platform.paper.weapon.handler.impl.SiltBlasterHandler;
import com.creatorsplash.oxygenheist.platform.paper.weapon.handler.impl.VenomSpitterHandler;
import com.creatorsplash.oxygenheist.platform.paper.weapon.provider.WeaponItemProvider;
import com.creatorsplash.oxygenheist.platform.paper.weapon.provider.impl.ItemsAdderWeaponItemProvider;
import com.creatorsplash.oxygenheist.platform.paper.weapon.service.WeaponDropService;
import com.creatorsplash.oxygenheist.platform.paper.weapon.service.WeaponHideService;
import com.creatorsplash.oxygenheist.platform.paper.weapon.service.WeaponProjectileTracker;
import com.creatorsplash.oxygenheist.platform.paper.world.PaperGamePlayerService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Getter
@RequiredArgsConstructor
@Accessors(fluent = true)
public final class WeaponModule implements Module {

    private final OxygenHeistPlugin plugin;
    private final LogCenter log;
    private final ConfigModule configs;
    private final GameplayModule gameplay;

    private WeaponItemProvider itemProvider;
    private WeaponRegistry weaponRegistry;
    private WeaponProjectileTracker projectileTracker;
    private WeaponEffectsState effectsState;
    private WeaponHideService hideService;
    private WeaponDropService dropService;

    /** Listeners that must be registered after build completes */
    private final List<Listener> listeners = new ArrayList<>();

    public WeaponModule build() {
        ItemsAdderWeaponItemProvider iaProvider = new ItemsAdderWeaponItemProvider(configs.weaponConfig());
        this.itemProvider = iaProvider;
        listeners.add(iaProvider);

        this.projectileTracker = new WeaponProjectileTracker();
        this.effectsState = new WeaponEffectsState();
        this.weaponRegistry = new WeaponRegistry();
        this.hideService = new WeaponHideService(plugin().getServer(), gameplay.scheduler());

        this.dropService = new WeaponDropService(
            plugin.getServer(),
            configs.globals(),
            configs.arenaConfig(),
            configs.messageConfig(),
            weaponRegistry,
            gameplay.scheduler(),
            plugin.getLogCenter()
        );

        if (gameplay.gamePlayerService() instanceof PaperGamePlayerService gamePlayerService) {
            gamePlayerService.setWeaponDropService(this.dropService);
        }

        registerHandlers();

        return this;
    }

    private void registerHandlers() {
        Scheduler scheduler = gameplay.scheduler();

        register("silt_blaster", config -> new SiltBlasterHandler(
            scheduler,
            config,
            itemProvider,
            effectsState,
            hideService
        ));

        register("venom_spitter", config -> new VenomSpitterHandler(
            config,
            itemProvider
        ));

        register("claw_cannon", config -> new ClawCannonHandler(
            scheduler,
            config,
            itemProvider
        ));

        // TODO
    }

    /* == Internals == */

    /**
     * Looks up the config for the given weapon id, logs a warning and skips
     * if not found or disabled, then registers the handler
     */
    private void register(String weaponId, Function<WeaponTypeConfig, WeaponHandler> factory) {
        WeaponTypeConfig config = configs.weaponConfig().getConfig(weaponId);
        if (config == null) {
            log.warn("[WeaponModule] No config found for weapon '" + weaponId + "' - skipping");
            return;
        }
        if (!config.enabled()) {
            log.info(
                "[WeaponModule] Weapon '" + weaponId + "' is disabled - skipping"
            );
            return;
        }
        weaponRegistry.register(factory.apply(config));
    }

}
