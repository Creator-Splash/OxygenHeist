package com.creatorsplash.oxygenheist.platform.paper;

import com.creatorsplash.oxygenheist.application.bridge.GameBridge;
import com.creatorsplash.oxygenheist.application.bridge.StandaloneGameBridge;
import com.creatorsplash.oxygenheist.application.match.combat.CombatService;
import com.creatorsplash.oxygenheist.application.match.combat.DownedService;
import com.creatorsplash.oxygenheist.application.match.MatchService;
import com.creatorsplash.oxygenheist.application.match.Scheduler;
import com.creatorsplash.oxygenheist.platform.paper.listener.CombatListener;
import com.creatorsplash.oxygenheist.platform.paper.scheduler.PaperSchedulerAdapter;
import lombok.Getter;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

public final class OxygenHeistPlugin extends JavaPlugin {

    @Getter
    private MatchService matchService;

    @Override
    public void onEnable() {

        /* Services */

        Scheduler scheduler = new PaperSchedulerAdapter(this);
        DownedService downedService = new DownedService();

        GameBridge bridge = new StandaloneGameBridge();
        this.matchService = new MatchService(scheduler, bridge, downedService);

        CombatService combatService = new CombatService(this.matchService, downedService);

        /* Listeners */

        registerListeners(
            new CombatListener(combatService)
        );

        getLogger().info("OxygenHeight Ready!");
    }

    @Override
    public void onDisable() {
        this.matchService.getScheduler().onDisable();

        HandlerList.unregisterAll(this);

        getLogger().info("OxygenHeight Shutdown!");
    }

    /* Helpers */

    private void registerListeners(Listener... listeners) {
        Arrays.stream(listeners).forEach(listener ->
            getServer().getPluginManager().registerEvents(listener, this));
    }

}
