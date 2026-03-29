package com.creatorsplash.oxygenheist.application.match.combat.weapon;

import com.creatorsplash.oxygenheist.application.match.MatchLifecycle;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Shared per-player cooldown tracking for weapon activations
 *
 *  <p>Cooldown durations are stored in milliseconds and compared against
 *  {@link System#currentTimeMillis()}</p>
 */
public final class WeaponCooldownService {

    private final Map<UUID, Long> lastUse = new HashMap<>();

    /**
     * Returns true if the player is currently on cooldown
     *
     * @param playerId the player to check
     * @param cooldownMillis the cooldown duration in milliseconds
     */
    public boolean isOnCooldown(UUID playerId, long cooldownMillis) {
        Long last = lastUse.get(playerId);
        if (last == null) return false;
        return System.currentTimeMillis() - last < cooldownMillis;
    }

    /**
     * Returns how many milliseconds remain on the player's cooldown, or 0 if not on cooldown
     */
    public long remainingMillis(UUID playerId, long cooldownMillis) {
        Long last = lastUse.get(playerId);
        if (last == null) return 0;
        long remaining = cooldownMillis - (System.currentTimeMillis() - last);
        return Math.max(0, remaining);
    }

    /**
     * Records a use for the given player, starting their cooldown now
     */
    public void recordUse(UUID playerId) {
        lastUse.put(playerId, System.currentTimeMillis());
    }

    /**
     * Clears cooldown state for a specific player
     */
    public void clear(UUID playerId) {
        lastUse.remove(playerId);
    }

    /**
     * Clears all cooldown state
     */
    public void clearAll() {
        lastUse.clear();
    }

}
