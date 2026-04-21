package com.creatorsplash.oxygenheist.platform.paper.listener;

import com.creatorsplash.oxygenheist.platform.paper.display.AirBarManager;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityAirChangeEvent;

@RequiredArgsConstructor
public final class AirChangeListener implements Listener {

    private final AirBarManager airBarController;

    @EventHandler
    public void onAirChange(EntityAirChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (!airBarController.isTracked(player.getUniqueId())) return;

        event.setAmount(airBarController.getTargetAir(player.getUniqueId()));
    }

}
