package com.creatorsplash.oxygenheist.platform.paper.weapon.service;

import com.creatorsplash.oxygenheist.application.match.MatchLifecycle;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks in-flight projectiles fired by weapon handlers
 */
public class WeaponProjectileTracker implements MatchLifecycle {

    /**
     * Identifies the weapon and shooter behind a tracked in-flight projectile.
     */
    public record TrackedProjectile(String weaponId, UUID shooterId) {}

    private final Map<UUID, TrackedProjectile> tracked = new HashMap<>();

    /**
     * Registers a newly fired projectile.
     *
     * @param projectileId the entity UUID of the projectile
     * @param weaponId the id of the weapon that fired it (must match a registered handler)
     * @param shooterId the UUID of the player who fired it
     */
    public void track(UUID projectileId, String weaponId, UUID shooterId) {
        tracked.put(projectileId, new TrackedProjectile(weaponId, shooterId));
    }

    /**
     * Retrieves and removes the tracking entry for the given projectile.
     *
     * <p>Returns {@code null} if this projectile was not fired by a tracked weapon handler.
     * Consuming removes the entry - subsequent calls for the same projectile return null.</p>
     */
    public @Nullable TrackedProjectile consume(UUID projectileId) {
        return tracked.remove(projectileId);
    }

    /**
     * Returns true if this projectile was fired by a tracked weapon handler.
     * Does not consume the entry.
     */
    public boolean isTracked(UUID projectileId) {
        return tracked.containsKey(projectileId);
    }

    /* Lifecycle */

    @Override
    public void onMatchEnd() {
        tracked.clear();
    }

    @Override
    public void onPlayerLeave(UUID playerId) {
        tracked.entrySet().removeIf(e -> e.getValue().shooterId().equals(playerId));
    }

}
