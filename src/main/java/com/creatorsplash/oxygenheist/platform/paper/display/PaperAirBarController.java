package com.creatorsplash.oxygenheist.platform.paper.display;

import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controls the vanilla air bar to represent oxygen
 */
public final class PaperAirBarController {

    private final ConcurrentHashMap<UUID, Boolean> trackedPlayers = new ConcurrentHashMap<>();

    /**
     * Update a players air based on oxygen values
     */
    public void update(Player player, double oxygen, double maxOxygen) {
        if (player == null) return;

        trackedPlayers.put(player.getUniqueId(), true);

        int maxAir = player.getMaximumAir();

        int air = (int) (oxygen / maxOxygen * maxAir);
        air = Math.clamp(air, 0, maxAir);

        player.setRemainingAir(air);
    }

    /**
     * Stop controlling a player
     */
    public void remove(Player player) {
        if (player == null) return;

        trackedPlayers.remove(player.getUniqueId());
        player.setRemainingAir(player.getMaximumAir());
    }

    /**
     * Check if we manage this player
     */
    public boolean isTracked(UUID playerId) {
        return trackedPlayers.containsKey(playerId);
    }

    /**
     * Reset everything
     */
    public void clearAll() {
        trackedPlayers.clear();
    }

 }
