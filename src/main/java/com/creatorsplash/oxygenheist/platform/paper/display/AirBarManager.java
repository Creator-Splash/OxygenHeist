package com.creatorsplash.oxygenheist.platform.paper.display;

import com.creatorsplash.oxygenheist.application.common.LogCenter;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controls the vanilla air bar to represent oxygen
 */
@RequiredArgsConstructor
public final class AirBarManager {

    private final LogCenter log;

    private final ConcurrentHashMap<UUID, Integer> targetAir = new ConcurrentHashMap<>();

    public void init(Player player) {
        player.setRemainingAir(player.getMaximumAir() - 1);
    }

    /**
     * Update a players air based on oxygen values
     */
    public void update(Player player, double oxygen, double maxOxygen) {
        if (player == null || maxOxygen <= 0) return;

        int maxAir = player.getMaximumAir();
        double ratio = oxygen / maxOxygen;
        int air = (int) Math.round(ratio * maxAir);
        air = Math.clamp(air, 0, maxAir - 1);

        player.setRemainingAir(air);
        targetAir.put(player.getUniqueId(), air);

//        log.debug("airbar", "<white> oxygen=" + String.format("%.1f", oxygen)
//            + " max=" + String.format("%.1f", maxOxygen)
//            + " ratio=" + String.format("%.3f", ratio)
//            + " air=" + air + "/" + maxAir);
    }

    public int getTargetAir(UUID playerId) {
        return targetAir.getOrDefault(playerId, 0);
    }

    /**
     * Stop controlling a player
     */
    public void remove(Player player) {
        if (player == null) return;

        targetAir.remove(player.getUniqueId());
        player.setRemainingAir(player.getMaximumAir());
    }

    /**
     * Check if we manage this player
     */
    public boolean isTracked(UUID playerId) {
        return targetAir.containsKey(playerId);
    }

    /**
     * Reset everything
     */
    public void clearAll() {
        targetAir.clear();
    }

 }
