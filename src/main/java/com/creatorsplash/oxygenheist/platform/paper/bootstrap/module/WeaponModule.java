package com.creatorsplash.oxygenheist.platform.paper.bootstrap.module;

import com.creatorsplash.oxygenheist.application.common.LogCenter;
import com.creatorsplash.oxygenheist.application.common.Module;
import com.creatorsplash.oxygenheist.application.match.Scheduler;
import com.creatorsplash.oxygenheist.platform.paper.OxygenHeistPlugin;
import com.creatorsplash.oxygenheist.platform.paper.config.GlobalConfig;
import com.creatorsplash.oxygenheist.platform.paper.config.weapon.WeaponTypeConfig;
import com.creatorsplash.oxygenheist.platform.paper.display.WeaponAmmoDisplayService;
import com.creatorsplash.oxygenheist.platform.paper.weapon.WeaponEffectsState;
import com.creatorsplash.oxygenheist.platform.paper.weapon.WeaponRegistry;
import com.creatorsplash.oxygenheist.platform.paper.weapon.handler.WeaponHandler;
import com.creatorsplash.oxygenheist.platform.paper.weapon.handler.impl.*;
import com.creatorsplash.oxygenheist.platform.paper.weapon.provider.WeaponItemProvider;
import com.creatorsplash.oxygenheist.platform.paper.weapon.provider.impl.AbstractWeaponProvider;
import com.creatorsplash.oxygenheist.platform.paper.weapon.provider.impl.ItemsAdderWeaponItemProvider;
import com.creatorsplash.oxygenheist.platform.paper.weapon.provider.impl.NexoWeaponItemProvider;
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
    private WeaponAmmoDisplayService ammoDisplay;
    private WeaponProjectileTracker projectileTracker;
    private WeaponEffectsState effectsState;
    private WeaponHideService hideService;
    private WeaponDropService dropService;

    /** Listeners that must be registered after build completes */
    private final List<Listener> listeners = new ArrayList<>();

    public WeaponModule build() {
        GlobalConfig.ItemProvider providerType = plugin.globals().itemProvider();

        switch (providerType) {
            case NEXO -> {
                var nexoProvider = new NexoWeaponItemProvider(configs.weaponConfig(), log);
                this.itemProvider = nexoProvider;
                listeners.add(nexoProvider);
                log.info("[WeaponModule] Using Nexo item provider");
            }
            case ITEMSADDER -> {
                ItemsAdderWeaponItemProvider iaProvider =
                    new ItemsAdderWeaponItemProvider(configs.weaponConfig(), log);
                this.itemProvider = iaProvider;
                listeners.add(iaProvider);  // IA needs a listener for load event
                log.info("[WeaponModule] Using ItemsAdder item provider");
            }
            default -> throw new IllegalStateException(
                "Unhandled item provider type: " + providerType
            );
        }

        this.ammoDisplay = new WeaponAmmoDisplayService(
            log,
            configs.globals(),
            (AbstractWeaponProvider<?>) itemProvider
        );

        this.weaponRegistry = new WeaponRegistry(ammoDisplay);
        this.projectileTracker = new WeaponProjectileTracker();
        this.effectsState = new WeaponEffectsState();
        this.hideService = new WeaponHideService(plugin().getServer(), gameplay.scheduler());

        this.dropService = new WeaponDropService(
            gameplay.actionService(),
            configs.globals(),
            configs.arenaConfig(),
            configs.messageConfig(),
            weaponRegistry,
            gameplay.scheduler(),
            plugin.getLogCenter()
        );
        listeners.add(dropService);

        if (gameplay.gamePlayerService() instanceof PaperGamePlayerService gamePlayerService) {
            gamePlayerService.setWeaponDropService(this.dropService);
        }

        registerHandlers();

        return this;
    }

    private void registerHandlers() {
        Scheduler scheduler = gameplay.scheduler();

        register(SiltBlasterHandler.ID, config -> new SiltBlasterHandler(
            scheduler,
            config,
            itemProvider,
            effectsState,
            hideService
        ));

        register(VenomSpitterHandler.ID, config -> new VenomSpitterHandler(
            config,
            itemProvider
        ));

        register(ClawCannonHandler.ID, config -> new ClawCannonHandler(
            scheduler,
            config,
            itemProvider
        ));

        register(DartSlingshotHandler.ID, config -> new DartSlingshotHandler(
            config,
            itemProvider,
            projectileTracker
        ));

        register(NeedleRifleHandler.ID, config -> new NeedleRifleHandler(
            config,
            itemProvider,
            projectileTracker
        ));

        register(SpikeShooterHandler.ID, config -> new SpikeShooterHandler(
            config,
            itemProvider,
            projectileTracker
        ));

        register(ReefHarpoonGunHandler.ID, config -> new ReefHarpoonGunHandler(
            config,
            itemProvider,
            projectileTracker
        ));
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
