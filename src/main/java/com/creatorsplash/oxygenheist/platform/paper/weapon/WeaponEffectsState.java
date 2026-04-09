package com.creatorsplash.oxygenheist.platform.paper.weapon;

import com.creatorsplash.oxygenheist.application.match.MatchLifecycle;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Shared state for active weapon-applied effects that must be queried
 * outside a handlers own invocation context
 */
public class WeaponEffectsState implements MatchLifecycle {

    private final Set<UUID> invertedControls = new HashSet<>();

    /* Inverted controls — SiltBlaster */

    public void addInvertedControls(UUID playerId) {
        invertedControls.add(playerId);
    }

    public void removeInvertedControls(UUID playerId) {
        invertedControls.remove(playerId);
    }

    public boolean hasInvertedControls(UUID playerId) {
        return invertedControls.contains(playerId);
    }

    /* Lifecycle */

    @Override
    public void onMatchEnd() {
        invertedControls.clear();
    }

    @Override
    public void onPlayerLeave(UUID playerId) {
        invertedControls.remove(playerId);
    }

}
