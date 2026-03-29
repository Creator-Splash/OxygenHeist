package com.creatorsplash.oxygenheist.application.match.combat.weapon;

import com.creatorsplash.oxygenheist.application.match.MatchLifecycle;

import java.util.*;

/**
 * Per-player reload state tracker for weapons with animation-based reloading
 */
public final class ReloadTracker implements MatchLifecycle {

    private final Set<UUID> reloading   = new HashSet<>();
    private final Map<UUID, Long> startMs = new HashMap<>();
    private final Map<UUID, Integer> slot = new HashMap<>();

    /**
     * Begins a reload for the given player, recording the start time and current slot.
     */
    public void begin(UUID playerId, int currentSlot) {
        reloading.add(playerId);
        startMs.put(playerId, System.currentTimeMillis());
        slot.put(playerId, currentSlot);
    }

    /**
     * Cancels an in-progress reload, clearing all state for the player.
     */
    public void cancel(UUID playerId) {
        reloading.remove(playerId);
        startMs.remove(playerId);
        slot.remove(playerId);
    }

    /** Returns true if the player is currently reloading. */
    public boolean isReloading(UUID playerId) {
        return reloading.contains(playerId);
    }

    /**
     * Returns the elapsed milliseconds since the player started reloading,
     * or {@code 0} if they are not reloading.
     */
    public long elapsedMs(UUID playerId) {
        Long start = startMs.get(playerId);
        if (start == null) return 0;
        return System.currentTimeMillis() - start;
    }

    /**
     * Returns true if the player has switched to a different hotbar slot
     * since their reload began - indicating the reload should be canceled.
     */
    public boolean isWrongSlot(UUID playerId, int currentSlot) {
        Integer expected = slot.get(playerId);
        return expected != null && expected != currentSlot;
    }

    /* Lifecycle */

    @Override
    public void onMatchEnd() {
        reloading.clear();
        startMs.clear();
        slot.clear();
    }

    @Override
    public void onPlayerLeave(UUID playerId) {
        cancel(playerId);
    }

}
