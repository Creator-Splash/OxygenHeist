package com.creatorsplash.oxygenheist.platform.paper.bootstrap.module;

import com.creatorsplash.oxygenheist.application.common.Module;
import com.creatorsplash.oxygenheist.platform.paper.weapon.WeaponEffectsState;
import com.creatorsplash.oxygenheist.platform.paper.weapon.WeaponRegistry;
import com.creatorsplash.oxygenheist.platform.paper.weapon.service.WeaponProjectileTracker;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@RequiredArgsConstructor
@Accessors(fluent = true)
public final class WeaponModule implements Module {

    private final ConfigModule configs;
    private final GameplayModule gameplay;

    private WeaponRegistry weaponRegistry;
    private WeaponProjectileTracker projectileTracker;
    private WeaponEffectsState effectsState;

    public WeaponModule build() {
        this.projectileTracker = new WeaponProjectileTracker();
        this.effectsState = new WeaponEffectsState();
        this.weaponRegistry = new WeaponRegistry();

        registerHandlers();

        return this;
    }

    private void registerHandlers() {
        // TODO
    }

}
