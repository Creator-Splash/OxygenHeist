package com.creatorsplash.oxygenheist.platform.paper;

import com.creatorsplash.oxygenheist.application.bridge.StandaloneGameBridge;
import com.creatorsplash.oxygenheist.application.combat.CombatService;
import com.creatorsplash.oxygenheist.application.match.MatchService;
import com.creatorsplash.oxygenheist.platform.paper.listener.CombatListener;
import lombok.Getter;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

public final class OxygenHeistPlugin extends JavaPlugin {

    @Getter
    private MatchService matchService;

    @Override
    public void onEnable() {
        /* Services */
        this.matchService = new MatchService(new StandaloneGameBridge());

        CombatService combatService = new CombatService(this.matchService);


        /* Listeners */

        registerListeners(
            new CombatListener(combatService)
        );
    }

    @Override
    public void onDisable() {
        // TODO
    }

    /* Helpers */

    private void registerListeners(Listener... listeners) {
        Arrays.stream(listeners).forEach(listener ->
            getServer().getPluginManager().registerEvents(listener, this));
    }

}
