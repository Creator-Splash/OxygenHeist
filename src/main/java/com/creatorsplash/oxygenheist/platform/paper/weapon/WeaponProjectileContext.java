package com.creatorsplash.oxygenheist.platform.paper.weapon;

import com.creatorsplash.oxygenheist.domain.match.MatchSession;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.jetbrains.annotations.Nullable;

/**
 * Contextual data passed into {@link WeaponHandler#onProjectileHit}
 *
 * <p>{@code hitEntity} is nullable - projectiles can hit blocks as well as entities.
 * Handlers should null-check before applying entity effects</p>
 */
public record WeaponProjectileContext(
    Player shooter,
    Projectile projectile,
    @Nullable Entity hitEntity,
    MatchSession session,
    boolean effectsActive
) {}
